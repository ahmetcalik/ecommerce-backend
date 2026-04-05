package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.*;
import com.project.ecommerce_backend.business.dtos.events.OrderCreatedEvent;
import com.project.ecommerce_backend.business.dtos.requests.order.AddOrderRequest;
import com.project.ecommerce_backend.business.dtos.requests.order.UpdateOrderStatusRequest;
import com.project.ecommerce_backend.business.dtos.responses.installment.InstallmentOptionResponse;
import com.project.ecommerce_backend.business.dtos.responses.order.AddOrderResponse;
import com.project.ecommerce_backend.business.dtos.responses.order.ListUserOrdersResponse;
import com.project.ecommerce_backend.business.dtos.responses.order.OrderDetailResponse;
import com.project.ecommerce_backend.business.dtos.responses.order.UserOrderDetailResponse;
import com.project.ecommerce_backend.business.helpers.CacheHelper;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.utils.mapper.ModelMapperService;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.Result;
import com.project.ecommerce_backend.core.utils.result.SuccessDataResult;
import com.project.ecommerce_backend.core.utils.result.SuccessResult;
import com.project.ecommerce_backend.entities.concretes.*;
import com.project.ecommerce_backend.entities.enums.OrderStatusEnum;
import com.project.ecommerce_backend.repositories.abstracts.OrderItemRepository;
import com.project.ecommerce_backend.repositories.abstracts.OrderRepository;
import com.project.ecommerce_backend.repositories.abstracts.OrderTrackingRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Sistemdeki sipariş ekosisteminin merkez üssü olan ve uçtan uca sipariş yaşam döngüsünü yöneten temel iş mantığı servisidir.
 * Bu sınıf; sepetin siparişe dönüştürülmesi, karmaşık finansal hesaplamaların (KDV, taksit faizi, kargo ücreti) doğrulanması, sipariş durumlarının (Hazırlanıyor, Kargoda, Teslim Edildi vb.) yasal ve mantıksal kurallar çerçevesinde güncellenmesi süreçlerinden sorumludur. Ayrıca, sistemin asenkron katmanıyla (Kafka) entegre çalışarak stok ve fatura süreçlerini tetikler ve Redis üzerinden çok katmanlı bir önbellekleme stratejisi yürüterek yüksek trafikli sipariş sorgularını optimize eder.
 */
@Service
@RequiredArgsConstructor
public class OrderManager implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CustomerService customerService;
    private final ShoppingCartService shoppingCartService;
    private final ProductService productService;
    private final AddressService addressService;
    private final ShippingMethodService shippingMethodService;
    private final OrderStatusService orderStatusService;
    private final PaymentMethodService paymentMethodService;
    private final InvoiceService invoiceService;
    private final InstallmentService installmentService;
    private final OrderTrackingRepository orderTrackingRepository;
    private final CarrierService carrierService;
    private final ModelMapperService modelMapperService;
    private final MessageService messageService;
    private final KafkaProducerService kafkaProducerService;
    private final CacheHelper cacheHelper;

    /**
     * Kimliği doğrulanmış bir kullanıcının veya yetkili bir satıcının belirli bir siparişe ait tüm teknik, finansal ve lojistik detaylarını görüntülemesini sağlar.
     * Erişim aşamasında "Data Ownership" (Veri Sahipliği) kontrolü yapılarak, kullanıcının sadece kendi siparişlerine veya satıcının sadece kendi ürünlerini barındıran sipariş detaylarına erişmesi garanti edilir; sorgu sonuçları performans kazanımı için kullanıcı bazlı anahtarlarla Redis üzerinde önbelleğe alınır.
     *
     * @param orderId Detayları incelenmek istenen siparişin eşsiz kimlik numarasıdır.
     * @return Siparişin kalem kalem dökümünü ve durum bilgilerini içeren UserOrderDetailResponse nesnesini döner.
     * @throws NotFoundException Sipariş bulunamazsa veya kullanıcı bu veriyi görmeye yetkili değilse fırlatılır.
     */
    @Override
    @Cacheable(value = "order", key = "'detail:' + @cacheHelper.getAuthenticatedUserId() + ':' + #orderId")
    public DataResult<UserOrderDetailResponse> getOrderDetailById(Long orderId) {
        Order order = findAndAuthorizeOrder(orderId);

        UserOrderDetailResponse response = modelMapperService.getMapper().map(order, UserOrderDetailResponse.class);

        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.Order.ORDER_DETAIL_SUCCESSFULLY_LISTED));
    }

    /**
     * Aktif kullanıcının geçmişten günümüze tüm sipariş serüvenini, performans dostu bir sayfalama yapısıyla liste halinde sunar.
     * Bu işlem sırasında veritabanı yükünü minimize etmek adına "Slice" yapısı kullanılır ve sonuçlar sayfa indeksi, boyutu ve sıralama tercihleriyle eşleşecek şekilde Redis üzerinde saklanarak kullanıcıya pürüzsüz bir arayüz deneyimi sunulur.
     *
     * @param pageable Sayfalama ve sıralama parametrelerini barındıran konfigürasyon nesnesidir.
     * @return Kullanıcının sipariş tarihçesini içeren başarılı bir veri sonuç nesnesini döner.
     */
    @Override
    @Cacheable(value = "order", key = "'list:' + @cacheHelper.getAuthenticatedUserId() + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()")
    public DataResult<List<ListUserOrdersResponse>> getAllOrders(Pageable pageable) {
        Long authenticatedUserId = cacheHelper.getAuthenticatedUserId();

        Slice<Order> orderSlice = orderRepository.findSliceByCustomerId(authenticatedUserId, pageable);

        List<ListUserOrdersResponse> response = orderSlice.getContent().stream()
                .map(order -> modelMapperService.getMapper().map(order, ListUserOrdersResponse.class))
                .toList();

        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.Order.ORDER_SUCCESSFULLY_LISTED));
    }

    /**
     * Müşterinin alışveriş sepetini resmi bir siparişe dönüştüren ve sistemin en kritik veri akışını başlatan metodun mimarıdır.
     * Süreç; sepet içeriğinin doğrulanması, adres ve ödeme yöntemlerinin kontrolü, sunucu tarafında yeniden yapılan mali hesaplamaların (taksit faizi dahil) istemci tarafıyla uyuştuğunun teyit edilmesiyle başlar. Sipariş veritabanına kaydedildikten sonra, stok düşürme ve fatura oluşturma gibi işlemlerin asenkron olarak tamamlanması için Kafka üzerinden bir 'OrderCreatedEvent' fırlatılır; böylece sistemin yanıt hızı (latency) korunurken arka plandaki süreçlerin hataya dayanıklı (event-driven) ilerlemesi sağlanır.
     *
     * @param request Siparişi veren müşteri, seçilen kargo, adres ve ödeme detaylarını barındıran talep nesnesidir.
     * @return Siparişin alındığına dair hızlı onay mesajını ve oluşturulan siparişin temel bilgilerini döner.
     * @throws BusinessException Sepetin boş olması veya finansal tutarların uyuşmaması gibi durumlarda fırlatılır.
     */
    @Caching(evict = {
            @CacheEvict(value = "order", key = "'list:' + #request.customerId + '*'", allEntries = true),
            @CacheEvict(value = "shoppingCart", key = "'customer:' + #request.customerId"),
    })
    @Override
    @Transactional
    public DataResult<AddOrderResponse> add(AddOrderRequest request) {
        Customer customer = customerService.getByIdAsEntity(request.getCustomerId());
        ShoppingCart cart = shoppingCartService.getActiveCartByCustomerId(request.getCustomerId());
        Address shippingAddress = addressService.getByIdAndCustomerId(request.getShippingAddressId(), request.getCustomerId());
        ShippingMethod shippingMethod = shippingMethodService.getByIdAsEntity(request.getShippingMethodId());
        OrderStatus initialStatus = orderStatusService.getInitialStatus();
        PaymentMethod paymentMethod = paymentMethodService.getByIdAndCustomerId(request.getPaymentMethodId(), request.getCustomerId());
        Set<ShoppingCartItem> cartItems = cart.getCartItems();
        if (cartItems.isEmpty()) {
            throw new BusinessException(messageService.getMessage(Messages.Order.ORDER_ERROR_EMPTY_CART));
        }

        Order newOrder = Order.builder()
                .customer(customer)
                .shippingAddress(shippingAddress)
                .shippingMethod(shippingMethod)
                .orderStatus(initialStatus)
                .orderDate(OffsetDateTime.now())
                .orderTotal(BigDecimal.ZERO)
                .build();
        Order savedOrder = this.orderRepository.save(newOrder);

        List<OrderItem> orderItems = cartItems.stream()
                .map(cartItem -> {
                    ProductItem productItem = cartItem.getProductItem();
                    return OrderItem.builder()
                            .order(savedOrder)
                            .productItem(productItem)
                            .quantity(cartItem.getQuantity())
                            .priceAtOrder(productItem.getUnitPrice())
                            .vatRate(productItem.getProduct().getVatRate())
                            .build();
                })
                .collect(Collectors.toList());
        this.orderItemRepository.saveAll(orderItems);

        BigDecimal totalSubTotal = orderItems.stream()
                .map(item -> item.getPriceAtOrder().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal totalVatAmount = orderItems.stream()
                .map(item -> item.getPriceAtOrder().multiply(BigDecimal.valueOf(item.getQuantity())).multiply(item.getVatRate()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal amountForInstallment = totalSubTotal.add(totalVatAmount);
        InstallmentOptionResponse calculatedOption = installmentService.getSpecificOption(
                amountForInstallment, paymentMethod.getCardFamily(), request.getInstallmentCount());
        BigDecimal serverCalculatedTotal = calculatedOption.getTotalAmount().add(shippingMethod.getShippingPrice());
        checkIfTotalAmountMatches(request.getExpectedTotalAmount(), serverCalculatedTotal);

        savedOrder.setOrderTotal(serverCalculatedTotal);
        this.orderRepository.save(savedOrder);

        List<OrderCreatedEvent.OrderItemDetail> itemDetailsForEvent = orderItems.stream()
                .map(item -> new OrderCreatedEvent.OrderItemDetail(
                        item.getProductItem().getId(),
                        item.getQuantity()
                ))
                .toList();

        OrderCreatedEvent event = new OrderCreatedEvent(
                savedOrder.getId(),
                savedOrder.getCustomer().getId(),
                savedOrder.getOrderTotal(),
                paymentMethod.getId(),
                request.getInstallmentCount(),
                itemDetailsForEvent
        );

        this.kafkaProducerService.sendOrderCreatedEvent(event);
        this.shoppingCartService.clearCartById(cart.getId());

        AddOrderResponse response = this.modelMapperService.getMapper().map(savedOrder, AddOrderResponse.class);
        return new SuccessDataResult<>(response, messageService.getMessage(Messages.Order.ORDER_SUCCESSFULLY_ADDED));
    }

    /**
     * Belirli bir adresin sistemdeki herhangi bir siparişte kargo adresi olarak kullanılıp kullanılmadığını kontrol eder.
     * Bu metot, özellikle adres silme veya güncelleme işlemlerinde veri bütünlüğünü korumak; geçmişe dönük sipariş kayıtlarının fiziksel adres referanslarını kaybetmesini engellemek için kritik bir bariyer görevi görür.
     *
     * @param addressId Denetlenecek adresin kimlik numarası.
     * @return Adres en az bir siparişte kullanılmışsa 'true' döner.
     */
    @Override
    public boolean existsByShippingAddressId(Long addressId) {
        return this.orderRepository.existsByShippingAddressId(addressId);
    }

    /**
     * Hazırlık aşaması tamamlanan bir siparişi "Kargoda" statüsüne geçirir ve lojistik takip bilgilerini sistem kayıtlarına ekler.
     * Bu güncelleme ile birlikte siparişe ait tüm kargo takip numarası ve kargo firması bilgileri OrderTracking tablosuna işlenir; işlemin ardından hem genel sipariş listeleri hem de siparişe özel tüm detay önbellekleri temizlenerek kullanıcının kargo durumunu anlık olarak görmesi sağlanır.
     *
     * @param orderId Kargoya verilecek siparişin ID'si.
     * @param request Kargo takip numarasını ve lojistik sağlayıcı bilgisini barındıran nesne.
     * @return Siparişin güncellenmiş kargo detaylarını döner.
     */
    @Caching(evict = {
            @CacheEvict(value = "order", key = "'list:' + @cacheHelper.getAuthenticatedUserId() + '*'", allEntries = true),
            @CacheEvict(value = "order", key = "'detail:' + @cacheHelper.getAuthenticatedUserId() + ':' + #orderId"),
            @CacheEvict(value = "order", key = "'entity:' + #orderId")
    })
    @Override
    @Transactional
    public DataResult<OrderDetailResponse> shippingOrder(Long orderId, UpdateOrderStatusRequest request) {
        Order order = findOrderByIdOrThrow(orderId);

        checkIfOrderIsShippable(order);

        Carrier carrier = carrierService.getByIdAsEntity(request.getCarrierId());
        OrderStatus shippedStatus = orderStatusService.getShippedStatus();
        order.setOrderStatus(shippedStatus);

        OrderTracking tracking = OrderTracking.builder()
                .order(order)
                .trackingNumber(request.getTrackingNumber())
                .carrier(carrier)
                .build();
        orderTrackingRepository.save(tracking);

        Order savedOrder = orderRepository.save(order);

        return new SuccessDataResult<>(mapOrderToDetailResponse(savedOrder), messageService.getMessage(
                Messages.Order.ORDER_SUCCESSFULLY_SHIPPED));
    }

    /**
     * Kargolama süreci tamamlanan bir siparişi "Teslim Edildi" statüsüne geçirerek sipariş yaşam döngüsünü nihai sonucuna ulaştırır.
     * Bu işlem; lojistik sürecin başarıyla sonlandığını sisteme kaydederken, aynı zamanda müşterinin sipariş geçmişinde güncel durumu görmesini sağlar. Durum değişikliği veri bütünlüğünü etkilediği için, işlem sonunda müşterinin hem genel sipariş listesini hem de ilgili siparişe ait özel detay önbelleğini (Redis) geçersiz kılarak verinin en güncel haliyle sunulmasını garanti eder.
     *
     * @param orderId Teslimat onayı verilecek olan siparişin kimlik numarasıdır.
     * @return Siparişin "Teslim Edildi" olarak güncellenmiş son detaylarını başarı mesajıyla birlikte döner.
     * @throws BusinessException Eğer sipariş teslim edilmeye uygun bir aşamada değilse (Örn: Henüz kargoya verilmemişse) fırlatılır.
     */
    @Caching(evict = {
            @CacheEvict(value = "order", key = "'list:' + @cacheHelper.getAuthenticatedUserId() + '*'", allEntries = true),
            @CacheEvict(value = "order", key = "'detail:' + @cacheHelper.getAuthenticatedUserId() + ':' + #orderId"),
            @CacheEvict(value = "order", key = "'entity:' + #orderId")
    })
    @Override
    @Transactional
    public DataResult<OrderDetailResponse> deliveryOrder(Long orderId) {
        Order order = findOrderByIdOrThrow(orderId);

        checkIfOrderIsDeliverable(order);

        OrderStatus deliveredStatus = orderStatusService.getDeliveredStatus();
        order.setOrderStatus(deliveredStatus);

        Order savedOrder = orderRepository.save(order);

        return new SuccessDataResult<>(mapOrderToDetailResponse(savedOrder), messageService.getMessage(
                Messages.Order.ORDER_SUCCESSFULLY_DELIVERED));
    }

    /**
     * Henüz hazırlık aşamasında olan bir siparişi kullanıcı veya sistem talebiyle resmi olarak iptal eder.
     * İptal protokolü gereği; öncelikle sipariş edilen ürünlerin stok miktarları envantere geri iade edilir, ardından siparişin faturası varsa ilgili fatura servisi üzerinden yasal iptal süreci başlatılır ve nihayetinde tüm ilgili önbellek kayıtları geçersiz kılınarak finansal döngü güvenli bir şekilde sonlandırılır.
     *
     * @param orderId İptal edilecek olan siparişin kimlik numarasıdır.
     * @return İptal işleminin başarı durumunu bildiren sonuç nesnesini döner.
     */
    @Caching(evict = {
            @CacheEvict(value = "order", key = "'list:' + @cacheHelper.getAuthenticatedUserId() + '*'", allEntries = true),
            @CacheEvict(value = "order", key = "'detail:' + @cacheHelper.getAuthenticatedUserId() + ':' + #orderId"),
            @CacheEvict(value = "order", key = "'entity:' + #orderId")
    })
    @Override
    @Transactional
    public Result cancelOrder(Long orderId) {
        Long authenticatedUserId = cacheHelper.getAuthenticatedUserId();

        Order orderToCancel = orderRepository.findByIdAndCustomerIdWithDetails(orderId, authenticatedUserId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage(
                        Messages.Order.ORDER_NOT_FOUND_OR_NOT_AUTHORIZED)));

        checkIfOrderIsCancellable(orderToCancel);
        returnStockForOrderItems(orderToCancel.getOrderItems());

        OrderStatus cancelledStatus = orderStatusService.getCancelledStatus();
        orderToCancel.setOrderStatus(cancelledStatus);
        orderRepository.save(orderToCancel);

        safelyCancelInvoice(orderToCancel);

        return new SuccessResult(messageService.getMessage(
                Messages.Order.ORDER_SUCCESSFULLY_CANCELLED));
    }

    /**
     * Bir siparişe ait spesifik bir ürünü (OrderItem), kullanıcı yetkilerini doğrulayarak entity formatında getirir.
     * Güvenlik katmanı gereği, sadece siparişi veren müşterinin kendi sipariş kalemine erişmesine izin verilir; bu sayede çapraz kullanıcı veri sızıntısı (IDOR) riskleri minimize edilir.
     *
     * @param orderItemId Sorgulanan sipariş kalemi kimliği.
     * @param customerId İşlemi yapan müşterinin kimliği.
     * @return Doğrulanan OrderItem entity nesnesini döner.
     */
    @Override
    public OrderItem findOrderItemByIdAndCustomerId(Long orderItemId, Long customerId) {
        return orderItemRepository.findByIdAndOrderCustomerId(orderItemId, customerId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage(
                        Messages.Order.ORDER_ITEM_NOT_FOUND_OR_NOT_AUTHORIZED)));
    }

    /**
     * Sipariş ID ve Müşteri ID kombinasyonuyla daha derinlemesine bir doğrulama yaparak spesifik bir sipariş kalemini getirir.
     * Bu metot, genellikle siparişin belirli bir evresinde sadece o siparişe ait kalemlerin güncellenmesi veya iade süreçlerinin başlatılması gibi operasyonlarda kesin doğrulama sağlamak amacıyla kullanılır.
     */
    @Override
    public OrderItem findOrderItemByIdAndOrderIdAndCustomerId(Long orderItemId, Long orderId, Long customerId) {
        return orderItemRepository.findByIdAndOrderIdAndOrderCustomerId(orderItemId, orderId, customerId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage(
                        Messages.Order.ORDER_ITEM_NOT_FOUND_OR_NOT_AUTHORIZED)));
    }

    /**
     * Siparişin durumunu (Status) güncelleyerek ilgili tüm önbellek kayıtlarını senkronize bir şekilde temizler.
     * Bu metot, genellikle yönetici (Admin) paneli üzerinden veya sistem içi otomatik süreçler tarafından tetiklenir; geçişin yasal olup olmadığını (`checkIfOrderStatusTransitionIsValid`) denetledikten sonra sadece veritabanını güncellemekle kalmaz, aynı zamanda `CacheHelper` aracılığıyla müşterinin tüm sipariş listesini ve detay kayıtlarını da geçersiz kılarak veri tutarlılığını sağlar.
     *
     * @param orderId Durumu güncellenecek siparişin benzersiz numarası.
     * @param newStatus Geçilmek istenen yeni durum nesnesi.
     * @return İşlemin başarıyla tamamlandığını bildiren sonuç nesnesi.
     */
    @Caching(evict = {
            @CacheEvict(value = "order", key = "'list:' + @cacheHelper.getOrderCustomerId(#orderId) + '*'", allEntries = true),
            @CacheEvict(value = "order", key = "'detail:' + @cacheHelper.getOrderCustomerId(#orderId) + ':' + #orderId"),
            @CacheEvict(value = "order", key = "'entity:' + #orderId")
    })
    @Override
    @Transactional
    public Result updateOrderStatus(Long orderId, OrderStatus newStatus) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Order.ORDER_NOT_FOUND, orderId)));

        checkIfOrderStatusTransitionIsValid(order.getOrderStatus(), newStatus);

        order.setOrderStatus(newStatus);
        orderRepository.save(order);
        return new SuccessResult(messageService.getMessage(
                Messages.Order.ORDER_STATUS_SUCCESSFULLY_UPDATED));
    }

    /**
     * Siparişi tüm ilişkili detaylarıyla (Customer, Address, Items) birlikte ham entity formatında veritabanından getirir.
     * Özellikle Kafka Consumer gibi servislerin sipariş detaylarına asenkron olarak erişmesi ve üzerinde işlem yapması gerektiği senaryolarda kullanılır; performans optimizasyonu için sonuç 'entity' anahtarıyla Redis üzerinde saklanır.
     */
    @Override
    @Cacheable(value = "order", key = "'entity:' + #orderId")
    public Order findByIdAsEntityWithDetails(Long orderId) {
        return findOrderByIdOrThrow(orderId);
    }

    // --- YARDIMCI METOTLAR ---

    /**
     * Sipariş işlemleri sırasında hem müşteri (Customer) hem de satıcı (Seller) bazlı veri sahipliği kontrollerini yaparak, hassas ticari verilerin sadece yetkili taraflara sunulmasını sağlayan güvenlik katmanıdır.
     */
    private Order findAndAuthorizeOrder(Long orderId) {
        Long authenticatedUserId = cacheHelper.getAuthenticatedUserId();

        Optional<Order> customerOrder = orderRepository.findByIdAndCustomerIdWithDetails(orderId, authenticatedUserId);

        if (customerOrder.isPresent()) {
            return customerOrder.get();
        }

        if (orderRepository.existsByOrderIdAndOrderItemProductItemProductSupplierId(orderId, authenticatedUserId)) {
            return findOrderByIdOrThrow(orderId);
        }

        throw new NotFoundException(messageService.getMessage(
                Messages.Order.ORDER_NOT_FOUND_OR_NOT_AUTHORIZED));
    }

    /**
     * Sipariş oluşturma aşamasında, istemci tarafından gönderilen beklenen toplam tutar ile sunucu tarafında hesaplanan nihai tutarın uyuşup uyuşmadığını denetler.
     * Bu kontrol, ağ üzerinden gelen verilerde olası bir manipülasyonu veya kuruşluk hesaplama hatalarını engelleyerek finansal güvenliği sağlar.
     */
    private void checkIfTotalAmountMatches(BigDecimal expectedTotalAmount, BigDecimal actualTotalAmount) {
        if (expectedTotalAmount.compareTo(actualTotalAmount) != 0) {
            throw new BusinessException(messageService.getMessage(
                    Messages.Errors.ORDER_TOTAL_DOES_NOT_MATCH));
        }
    }

    /**
     * Veritabanında siparişi tüm alt detaylarıyla (Eager Loading) sorgulayan ve bulunamaması durumunda uluslararasılaştırma desteğiyle hata fırlatan merkezi sorgu metodudur.
     */
    private Order findOrderByIdOrThrow(Long orderId) {
        return orderRepository.findByIdWithDetails(orderId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Order.ORDER_NOT_FOUND, orderId)));
    }

    /**
     * Karmaşık bir sipariş entity nesnesini, kullanıcı arayüzüne sunulacak olan basitleştirilmiş veri transfer nesnesine (DTO) dönüştürür.
     */
    private OrderDetailResponse mapOrderToDetailResponse(Order order) {
        return modelMapperService.getMapper().map(order, OrderDetailResponse.class);
    }

    /**
     * Sipariş iptali durumunda, eğer oluşturulmuş bir fatura mevcutsa bu faturayı yasal mevzuata uygun şekilde iptal sürecine sokan finansal emniyet mekanizmasıdır.
     */
    private void safelyCancelInvoice(Order order) {
        if (order.getInvoice() != null) {
            invoiceService.cancelInvoice(order.getInvoice().getId());
        }
    }

    /**
     * Bir siparişin fiziksel olarak paketlenip yola çıkmaya (Shipping) hazır olup olmadığını, mevcut durumunun yasal ön koşullarını (Örn: Sadece yeni siparişler kargolanabilir) inceleyerek doğrular.
     */
    private void checkIfOrderIsShippable(Order order) {
        OrderStatus initialStatus = orderStatusService.getInitialStatus();
        if (!order.getOrderStatus().getId().equals(initialStatus.getId())) {
            throw new BusinessException(messageService.getMessage(
                    Messages.Order.ORDER_IS_NOT_SHIPPABLE));
        }
    }

    /**
     * Kargo sürecindeki bir siparişin son kullanıcıya ulaştığı bilgisinin işlenebilir olup olmadığını (Örn: Sadece kargodaki siparişler teslim edildi sayılabilir) denetler.
     */
    private void checkIfOrderIsDeliverable(Order order) {
        OrderStatus shippedStatus = orderStatusService.getShippedStatus();
        if (!order.getOrderStatus().getId().equals(shippedStatus.getId())) {
            throw new BusinessException(messageService.getMessage(
                    Messages.Order.ORDER_IS_NOT_DELIVERABLE));
        }
    }

    /**
     * Müşterinin veya sistemin siparişi iptal etme talebinin, siparişin mevcut aşamasına (Örn: Hazırlık aşaması geçilmemiş olmalı) uygunluğunu kontrol eder.
     */
    private void checkIfOrderIsCancellable(Order order) {
        OrderStatus initialStatus = orderStatusService.getInitialStatus();
        if (!order.getOrderStatus().getId().equals(initialStatus.getId())) {
            throw new BusinessException(messageService.getMessage(
                    Messages.Order.ORDER_IS_NOT_CANCELLABLE));
        }
    }

    /**
     * İptal edilen siparişlerdeki ürün kalemlerini tek tek analiz ederek, eksilen stokların envanter yönetimine (ProductService) kusursuz bir şekilde geri iade edilmesini sağlar.
     */
    private void returnStockForOrderItems(Set<OrderItem> orderItems) {
        if (orderItems == null || orderItems.isEmpty()) return;
        orderItems.forEach(item ->
                productService.increaseStock(item.getProductItem().getId(), item.getQuantity())
        );
    }

    /**
     * Siparişlerin yaşam döngüsü boyunca bir durumdan diğerine geçişinin (Örn: Hazırlanıyor -> Kargoda) yasal ve mantıksal bir sıra takip edip etmediğini kontrol eder.
     * Bu kontrol, "Kargodaki" bir siparişin doğrudan "Hazırlanıyor" aşamasına geri çekilmesini veya "İptal Edilmiş" bir siparişin "Teslim Edildi" olarak işaretlenmesini engelleyerek sistemin veri tutarlılığını korur.
     */
    private void checkIfOrderStatusTransitionIsValid(OrderStatus currentStatus, OrderStatus newStatus) {
        OrderStatusEnum currentEnum = currentStatus.getStatusName();
        OrderStatusEnum newEnum = newStatus.getStatusName();

        if (currentEnum == newEnum) {
            return;
        }

        boolean isValidTransition = switch (currentEnum) {
            case PREPARING -> (newEnum == OrderStatusEnum.SHIPPED || newEnum == OrderStatusEnum.CANCELLED);
            case SHIPPED -> (newEnum == OrderStatusEnum.DELIVERED);
            case DELIVERED -> (newEnum == OrderStatusEnum.RETURNED);
            case RETURN_REQUESTED -> (newEnum == OrderStatusEnum.RETURNED || newEnum == OrderStatusEnum.DELIVERED);
            case CANCELLED, RETURNED -> false;
        };

        if (!isValidTransition) {
            throw new BusinessException(
                    String.format(messageService.getMessageWithParams(
                            Messages.Order.ORDER_INVALID_STATUS_TRANSITION, currentEnum, newEnum, currentEnum, newEnum)));
        }
    }
}