package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.*;
import com.project.ecommerce_backend.business.dtos.requests.return_request.AddReturnRequest;
import com.project.ecommerce_backend.business.dtos.requests.return_request.UpdateReturnStatusRequest;
import com.project.ecommerce_backend.business.dtos.responses.return_request.ReturnRequestResponse;
import com.project.ecommerce_backend.business.helpers.CacheHelper;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.utils.mapper.ModelMapperService;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.SuccessDataResult;
import com.project.ecommerce_backend.entities.concretes.Order;
import com.project.ecommerce_backend.entities.concretes.OrderItem;
import com.project.ecommerce_backend.entities.concretes.OrderStatus;
import com.project.ecommerce_backend.entities.concretes.ReturnRequest;
import com.project.ecommerce_backend.entities.enums.OrderStatusEnum;
import com.project.ecommerce_backend.entities.enums.ReturnStatusEnum;
import com.project.ecommerce_backend.repositories.abstracts.ReturnRequestRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;

/**
 * Müşteri iade süreçlerini yöneten, yasal iade sürelerini denetleyen ve iade sonrası envanter/finans güncellemelerini koordine eden servis katmanıdır.
 * Bu sınıf; iade taleplerinin oluşturulması, satıcı veya yönetici bazlı yetki kontrollerinin yapılması, iade edilebilirliğin zaman bazlı doğrulanması ve onaylanan iadeler sonucunda stokların artırılarak faturaların iade durumuna çekilmesi süreçlerini yönetir. Veri tutarlılığını sağlamak adına tüm işlemleri atomik bir yapıda yürütür ve iade listelerini Redis üzerinde sayfalı olarak önbelleğe alır.
 */
@Service
public class ReturnRequestManager implements ReturnRequestService {

    private final ReturnRequestRepository returnRequestRepository;
    private final OrderService orderService;
    private final ProductService productService;
    private final OrderStatusService orderStatusService;
    private final InvoiceService invoiceService;
    private final ModelMapperService modelMapperService;
    private final MessageService messageService;
    private final CacheHelper cacheHelper;
    private final int returnPeriodDays;


    public ReturnRequestManager(
            ReturnRequestRepository returnRequestRepository,
            OrderService orderService,
            ProductService productService,
            OrderStatusService orderStatusService,
            InvoiceService invoiceService,
            ModelMapperService modelMapperService,
            MessageService messageService,
            CacheHelper cacheHelper,
            @Value("${app.return.period-days}") int returnPeriodDays) {
        this.returnRequestRepository = returnRequestRepository;
        this.orderService = orderService;
        this.productService = productService;
        this.orderStatusService = orderStatusService;
        this.invoiceService = invoiceService;
        this.modelMapperService = modelMapperService;
        this.messageService = messageService;
        this.cacheHelper = cacheHelper;
        this.returnPeriodDays = returnPeriodDays;
    }


    /**
     * Sistemdeki tüm iade taleplerini yönetici veya satıcı yetkileri dahilinde sayfalanmış bir liste olarak sunar.
     * Bu listeleme işlemi yüksek trafikli yönetim panellerinde kullanıldığı için, performans kazanımı sağlamak amacıyla sayfa numarası ve sıralama kriterlerine göre Redis üzerinde önbelleğe alınır.
     *
     * @param pageable Sayfa boyutu ve sıralama bilgilerini içeren nesnedir.
     * @return Sayfalanmış iade talebi listesini başarı mesajıyla birlikte döner.
     */
    @Override
    @Cacheable(value = "returnRequest", key = "'list:' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()")
    public DataResult<Slice<ReturnRequestResponse>> getAllReturns(Pageable pageable) {
        Slice<ReturnRequest> returnRequestSlice = returnRequestRepository.findAll(pageable);

        Slice<ReturnRequestResponse> response = returnRequestSlice.map(
                returnRequest -> modelMapperService.getMapper().map(returnRequest, ReturnRequestResponse.class)
        );

        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.ReturnRequest.RETURN_REQUESTS_SUCCESSFULLY_LISTED));
    }

    /**
     * Belirli bir iade talebinin detaylarını, talep eden kullanıcının yönetici veya ilgili ürünün satıcısı olup olmadığını doğrulayarak getirir.
     * Yetki kontrolü aşamasında veri sahipliği denetlenir ve doğrulanmış sonuçlar hızlı erişim için iade kimliği üzerinden önbelleğe alınır.
     *
     * @param id İncelenmek istenen iade talebinin benzersiz kimliğidir.
     * @return İade talebi detaylarını barındıran veri transfer nesnesini döner.
     * @throws NotFoundException Talep bulunamazsa veya kullanıcı yetkisizse fırlatılır.
     */
    @Override
    @Cacheable(value = "returnRequest", key = "#id")
    public DataResult<ReturnRequestResponse> getReturnByIdForAdmin(Long id) {

        checkReturnManagementAuthorization(id);

        ReturnRequest returnRequest = findReturnRequestByIdOrThrow(id);

        ReturnRequestResponse response = modelMapperService.getMapper().map(returnRequest, ReturnRequestResponse.class);

        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.ReturnRequest.RETURN_REQUEST_DETAIL_SUCCESSFULLY_LISTED));
    }

    /**
     * Teslim edilmiş bir sipariş kalemine ait yeni bir iade talebi oluşturur.
     * İşlem sırasında; aynı ürün için mükerrer talep olup olmadığı, siparişin teslim edilip edilmediği ve yasal iade süresinin geçip geçmediği titizlikle denetlenir. Yeni talep oluşturulduğunda veri güncelliğini korumak adına tüm iade listesi önbellekleri geçersiz kılınır.
     *
     * @param request İade nedeni ve ilgili sipariş kalemi bilgilerini içeren talep nesnesidir.
     * @return Oluşturulan iade talebinin özet bilgilerini döner.
     * @throws BusinessException İade süresi dolmuşsa veya şartlar sağlanmıyorsa fırlatılır.
     */
    @Caching(evict = {
            @CacheEvict(value = "returnRequest", key = "'list:' + '*'", allEntries = true)
    })
    @Override
    @Transactional
    public DataResult<ReturnRequestResponse> add(AddReturnRequest request) {
        Long authenticatedUserId = cacheHelper.getAuthenticatedUserId();

        OrderItem orderItemToReturn = orderService.findOrderItemByIdAndOrderIdAndCustomerId(
                request.getOrderItemId(),
                request.getOrderId(),
                authenticatedUserId);

        checkIfReturnIsPossible(orderItemToReturn);

        ReturnRequest returnRequest = ReturnRequest.builder()
                .order(orderItemToReturn.getOrder())
                .orderItem(orderItemToReturn)
                .reason(request.getReason())
                .customReason(request.getCustomReason())
                .status(ReturnStatusEnum.PENDING_APPROVAL)
                .build();

        ReturnRequest savedRequest = returnRequestRepository.save(returnRequest);

        ReturnRequestResponse response = modelMapperService.getMapper().map(savedRequest, ReturnRequestResponse.class);
        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.ReturnRequest.RETURN_REQUEST_SUCCESSFULLY_CREATED));
    }

    /**
     * Mevcut bir iade talebinin durumunu güncelleyerek onay, red veya tamamlanma aşamalarına taşır.
     * Durum güncellemesi sırasında talep daha önce sonuçlandırılmışsa işlem engellenir; eğer talep başarıyla tamamlandı statüsüne geçerse stok iadesi ve fatura iptali gibi operasyonel yan etkiler otomatik olarak tetiklenir. Güncelleme sonrası ilgili tekil kayıt ve genel liste önbellekleri temizlenir.
     *
     * @param id      Durumu değiştirilecek iade talebinin kimliğidir.
     * @param request Yeni durum bilgisini barındıran nesnedir.
     * @return Güncellenmiş iade talebi bilgilerini döner.
     */
    @Caching(evict = {
            @CacheEvict(value = "returnRequest", key = "#id"),
            @CacheEvict(value = "returnRequest", key = "'list:' + '*'", allEntries = true)
    })
    @Override
    @Transactional
    public DataResult<ReturnRequestResponse> updateReturnStatus(Long id, UpdateReturnStatusRequest request) {

        checkReturnManagementAuthorization(id);

        ReturnRequest returnRequest = findReturnRequestByIdOrThrow(id);
        ReturnStatusEnum newStatus = request.getNewStatus();

        checkIfReturnIsAlreadyFinalized(returnRequest);

        returnRequest.setStatus(newStatus);
        ReturnRequest savedRequest = returnRequestRepository.save(returnRequest);

        handleReturnStatusSideEffects(savedRequest);

        ReturnRequestResponse response = modelMapperService.getMapper().map(savedRequest, ReturnRequestResponse.class);

        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.ReturnRequest.RETURN_REQUEST_STATUS_SUCCESSFULLY_UPDATED));
    }

    // --- YARDIMCI METOTLAR ---

    /**
     * Yönetici olmayan kullanıcıların sadece kendi mağazalarına ait ürünlerin iade taleplerine müdahale edebilmesini garanti altına alan veri sahipliği kontrolüdür.
     */
    private void checkReturnManagementAuthorization(Long returnRequestId) {
        if (isUserAdmin()) {
            return;
        }

        Long authenticatedUserId = cacheHelper.getAuthenticatedUserId();

        if (!returnRequestRepository.existsByIdAndOrderItemProductItemProductSupplierId(returnRequestId, authenticatedUserId)) {
            throw new NotFoundException(messageService.getMessage(
                    Messages.ReturnRequest.RETURN_REQUEST_NOT_AUTHORIZED));
        }
    }

    /**
     * Oturum açmış kullanıcının sistemdeki rol yetkilerini analiz ederek yönetici haklarına sahip olup olmadığını denetler.
     * Bu kontrol, iade süreçlerinde sınırsız erişim yetkisi olan yöneticiler ile sadece kendi ürünlerini yönetebilen satıcılar arasındaki ayrımı yapmak için kullanılır.
     */
    private boolean isUserAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN"));
    }

    /**
     * Veritabanında iade talebi sorgulaması yapan ve kayıt bulunamadığında uluslararasılaştırma desteğiyle hata fırlatan merkezi arama metodudur.
     */
    private ReturnRequest findReturnRequestByIdOrThrow(Long id) {
        return returnRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.ReturnRequest.RETURN_REQUEST_NOT_FOUND, id)));
    }

    /**
     * İade sürecinin yasal olarak başlayıp başlayamayacağını; sipariş statüsü, teslimat tarihi ve mükerrer kayıt kontrolü üzerinden analiz eden denetim mekanizmasıdır.
     */
    private void checkIfReturnIsPossible(OrderItem orderItem) {
        if (returnRequestRepository.existsByOrderIdAndOrderItemId(orderItem.getOrder().getId(), orderItem.getId())) {
            throw new BusinessException(messageService.getMessage(
                    Messages.ReturnRequest.RETURN_REQUEST_ALREADY_EXISTS));
        }

        if (orderItem.getOrder().getOrderStatus().getStatusName() != OrderStatusEnum.DELIVERED) {
            throw new BusinessException(messageService.getMessage(
                    Messages.ReturnRequest.RETURN_NOT_POSSIBLE_FOR_ORDER_STATUS));
        }

        OffsetDateTime deliveryDate = orderItem.getOrder().getUDate();
        long daysSinceDelivery = ChronoUnit.DAYS.between(deliveryDate, OffsetDateTime.now());
        if (daysSinceDelivery > this.returnPeriodDays) {
            throw new BusinessException(messageService.getMessageWithParams(
                    Messages.ReturnRequest.RETURN_PERIOD_EXPIRED, this.returnPeriodDays));
        }
    }

    /**
     * Sonuçlandırılmış (Tamamlandı veya Reddedildi) iade taleplerinin üzerinde tekrar değişiklik yapılmasını engelleyerek işlem güvenliğini sağlar.
     */
    private void checkIfReturnIsAlreadyFinalized(ReturnRequest returnRequest) {
        if (returnRequest.getStatus() == ReturnStatusEnum.COMPLETED ||
                returnRequest.getStatus() == ReturnStatusEnum.REJECTED) {
            throw new BusinessException(messageService.getMessage(
                    Messages.ReturnRequest.RETURN_REQUEST_ALREADY_FINALIZED));
        }
    }

    /**
     * Onaylanan iade talepleri sonucunda sistemdeki envanterin artırılması, siparişin iade edildi olarak işaretlenmesi ve faturanın geri ödeme durumuna getirilmesi süreçlerini yöneten operasyonel merkezdir.
     */
    private void handleReturnStatusSideEffects(ReturnRequest returnRequest) {
        if (returnRequest.getStatus() == ReturnStatusEnum.COMPLETED) {
            OrderItem orderItem = returnRequest.getOrderItem();
            Order order = returnRequest.getOrder();

            productService.increaseStock(orderItem.getProductItem().getId(), orderItem.getQuantity());

            OrderStatus returnedStatus = orderStatusService.getReturnedStatus();

            orderService.updateOrderStatus(order.getId(), returnedStatus);

            if (order.getInvoice() != null) {
                invoiceService.markInvoiceAsRefunded(order.getInvoice().getId());
            }
        }
    }

}