package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.InvoiceService;
import com.project.ecommerce_backend.business.dtos.responses.invoice.ListUserInvoicesResponse;
import com.project.ecommerce_backend.business.dtos.responses.invoice.UserInvoiceDetailResponse;
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
import com.project.ecommerce_backend.entities.enums.InvoiceStatus;
import com.project.ecommerce_backend.repositories.abstracts.InvoiceRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

/**
 * Sistemdeki tüm mali kayıtların ve faturalandırma süreçlerinin yönetildiği, ticari veri bütünlüğünün sağlandığı merkez servisidir.
 * Bu sınıf; başarılı siparişlerin ardından yasal fatura kayıtlarının oluşturulması, KDV, kargo ve vade farkı gibi kalemlerin finansal dökümü, fatura iptal ve iade süreçlerinin yasal mevzuata uygun yönetimi ve bu kayıtların Redis üzerinde kullanıcı bazlı önbelleğe alınarak yüksek performansla sunulması süreçlerinden sorumludur.
 */
@Service
@RequiredArgsConstructor
public class InvoiceManager implements InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final ModelMapperService modelMapperService;
    private final MessageService messageService;
    private final CacheHelper cacheHelper;


    /**
     * Tamamlanan bir sipariş için tüm finansal kalemlerin (ara toplam, KDV, vade farkı, kargo ücreti) dahil edildiği resmi bir fatura kaydı oluşturur.
     * Metot çalıştırıldığında, siparişin son toplam tutarı ile fatura kalemlerinin tutarlılığı doğrulanır; fatura "ÖDENDİ" statüsünde sisteme kaydedilir ve işlem sonunda ilgili müşterinin geçmiş fatura listelerini içeren Redis önbelleği, yeni veriyle güncellenmesi için temizlenir.
     *
     * @param order Faturanın bağlanacağı ana sipariş entity nesnesi.
     * @param paymentMethod Siparişte kullanılan ödeme yöntemi.
     * @param productSubTotal Ürünlerin vergiler hariç ham toplam tutarı.
     * @param productVatAmount Ürünler üzerinden hesaplanan toplam KDV miktarı.
     * @param interestAmount Taksitli işlemlerde uygulanan toplam vade farkı tutarı.
     * @param installmentCount Toplam taksit sayısı.
     * @param shippingMethod Tercih edilen lojistik sağlayıcı ve kargo ücreti bilgisi.
     * @return Veritabanına kalıcı olarak kaydedilen Invoice entity nesnesini döner.
     */
    @Caching(evict = {
            @CacheEvict(value = "invoice", key = "'list:' + #order.customer.id + '*'", allEntries = true)
    })
    @Override
    public Invoice createInvoiceForOrder(Order order, PaymentMethod paymentMethod,
                                         BigDecimal productSubTotal, BigDecimal productVatAmount,
                                         BigDecimal interestAmount, Integer installmentCount,
                                         ShippingMethod shippingMethod) {

        BigDecimal shippingFee = shippingMethod.getShippingPrice();
        BigDecimal grandTotal = order.getOrderTotal();

        Invoice invoice = Invoice.builder()
                .order(order)
                .paymentMethod(paymentMethod)
                .grandTotal(grandTotal)
                .subTotal(productSubTotal)
                .vatAmount(productVatAmount)
                .shippingFee(shippingFee)
                .interestAmount(interestAmount)
                .installmentCount(installmentCount)
                .status(InvoiceStatus.PAID)
                .build();

        return this.invoiceRepository.save(invoice);
    }

    /**
     * Sisteme giriş yapmış olan kullanıcının geçmişten günümüze tüm fatura kayıtlarını, performans odaklı bir sayfalama yapısıyla liste halinde sunar.
     * Jackson serileştirme kısıtlamaları nedeniyle veritabanından gelen Slice içeriği standart Java List yapısına map edilerek döner; sorgu sonuçları kullanıcının kimliği ve sayfalama parametrelerine göre Redis üzerinde önbelleğe alınarak tekrarlı görüntülemelerde sistem yükü minimize edilir.
     *
     * @param pageable Sayfa numarası ve sıralama kriterlerini barındıran konfigürasyon nesnesi.
     * @return Kullanıcının fatura koleksiyonunu içeren başarılı bir veri sonucu (DataResult) döner.
     */
    @Override
    @Cacheable(value = "invoice", key = "'list:' + @cacheHelper.getAuthenticatedUserId() + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()")
    public DataResult<List<ListUserInvoicesResponse>> getAllMyInvoices(Pageable pageable) {
        Long authenticatedUserId = cacheHelper.getAuthenticatedUserId();

        Slice<Invoice> invoiceSlice = invoiceRepository.findSliceByCustomerId(authenticatedUserId, pageable);

        List<ListUserInvoicesResponse> response = invoiceSlice.getContent().stream()
                .map(invoice -> modelMapperService.getMapper().map(invoice, ListUserInvoicesResponse.class ))
                .toList();

        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.Invoice.INVOICES_SUCCESSFULLY_LISTED));
    }

    /**
     * Kullanıcının talep ettiği belirli bir faturanın tüm finansal ve lojistik detaylarını, güvenlik kontrolleri eşliğinde sunar.
     * Sorgulama aşamasında faturanın gerçekten oturum açmış kullanıcıya ait olup olmadığı "Ownership Validation" adımından geçer; doğrulama başarılı ise veri, kullanıcı kimliği ve fatura ID'si ile eşleşecek şekilde Redis hafızasına işlenir.
     *
     * @param id Detayları görüntülenmek istenen faturanın benzersiz kimlik numarasıdır.
     * @return Faturanın kalem kalem dökümünü içeren UserInvoiceDetailResponse nesnesini döner.
     * @throws NotFoundException Fatura bulunamazsa veya kullanıcıya ait değilse fırlatılır.
     */
    @Override
    @Cacheable(value = "invoice", key = "'detail:' + @cacheHelper.getAuthenticatedUserId() + ':' + #id")
    public DataResult<UserInvoiceDetailResponse> getMyInvoiceById(Long id) {
        Long authenticatedUserId = cacheHelper.getAuthenticatedUserId();

        Invoice invoice = invoiceRepository.findByIdAndCustomerIdWithDetails(id, authenticatedUserId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage(
                        Messages.Invoice.INVOICE_NOT_FOUND_OR_NOT_AUTHORIZED)));

        UserInvoiceDetailResponse response = modelMapperService.getMapper().map(invoice, UserInvoiceDetailResponse.class);

        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.Invoice.INVOICE_DETAIL_SUCCESSFULLY_LISTED));
    }

    /**
     * Belirtilen bir faturayı sistem üzerinde "İPTAL EDİLDİ" statüsüne çeker ve yasal kaydı pasifleştirir.
     * İş akışı kapsamında faturanın mevcut statüsü kontrol edilerek zaten iptal edilmiş veya iade süreci tamamlanmış faturaların tekrar işleme alınması engellenir; işlem sonunda CacheHelper aracılığıyla ilgili müşterinin hem detay hem de liste bazlı tüm fatura önbellekleri geçersiz kılınır.
     *
     * @param invoiceId İptal edilecek faturanın benzersiz ID bilgisidir.
     * @return İşlemin başarı durumunu bildiren sonuç nesnesini döner.
     */
    @Caching(evict = {
            @CacheEvict(value = "invoice", key = "'detail:' + @cacheHelper.getInvoiceCustomerId(#invoiceId) + ':' + #invoiceId"),
            @CacheEvict(value = "invoice", key = "'list:' + @cacheHelper.getInvoiceCustomerId(#invoiceId) + '*'", allEntries = true)
    })
    @Override
    @Transactional
    public Result cancelInvoice(Long invoiceId) {
        Invoice invoiceToCancel = findInvoiceByIdOrThrow(invoiceId);

        checkIfInvoiceCanBeCancelled(invoiceToCancel);

        invoiceToCancel.setStatus(InvoiceStatus.CANCELLED);
        invoiceRepository.save(invoiceToCancel);

        return new SuccessResult(messageService.getMessage(
                Messages.Invoice.INVOICE_SUCCESSFULLY_CANCELLED));
    }

    /**
     * Bir faturayı, iade süreci tamamlandıktan sonra "İADE EDİLDİ" olarak işaretleyerek finansal döngüyü sonlandırır.
     * Sadece "ÖDENDİ" statüsündeki faturaların iadesine izin verilerek hatalı işlem yapılması önlenir; güncelleme sonrası sistemdeki ilgili tüm fatura önbellekleri otomatik olarak temizlenerek veri tutarlılığı sağlanır.
     *
     * @param invoiceId İade işlemi onaylanan faturanın kimlik numarasıdır.
     * @return Güncelleme sonucunu bildiren başarı mesajını döner.
     */
    @Caching(evict = {
            @CacheEvict(value = "invoice", key = "'detail:' + @cacheHelper.getInvoiceCustomerId(#invoiceId) + ':' + #invoiceId"),
            @CacheEvict(value = "invoice", key = "'list:' + @cacheHelper.getInvoiceCustomerId(#invoiceId) + '*'", allEntries = true)
    })
    @Override
    @Transactional
    public Result markInvoiceAsRefunded(Long invoiceId) {
        Invoice invoiceToUpdate = findInvoiceByIdOrThrow(invoiceId);

        checkIfInvoiceIsAlreadyRefunded(invoiceToUpdate);
        checkIfInvoiceStatusIsPaid(invoiceToUpdate);

        invoiceToUpdate.setStatus(InvoiceStatus.REFUNDED);
        invoiceRepository.save(invoiceToUpdate);

        return new SuccessResult(messageService.getMessage(
                Messages.Invoice.INVOICE_SUCCESSFULLY_MARKED_REFUNDED));
    }

    // --- YARDIMCI METOTLAR ---

    /**
     * Veritabanı sorgularında tekrara düşmemek için tasarlanmış, temel fatura arama ve "bulunamadı" hatası fırlatma mekanizmasıdır.
     */
    private Invoice findInvoiceByIdOrThrow(Long invoiceId) {
        return invoiceRepository.findById(invoiceId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Invoice.INVOICE_NOT_FOUND, invoiceId)));
    }

    /**
     * Faturanın iptal edilebilir durumda olup olmadığını, mevcut statüsünü (Örn: İADE, İPTAL) analiz ederek denetler.
     */
    private void checkIfInvoiceCanBeCancelled(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.CANCELLED || invoice.getStatus() == InvoiceStatus.REFUNDED) {
            throw new BusinessException(messageService.getMessageWithParams(
                    Messages.Invoice.INVOICE_CANNOT_BE_CANCELLED, invoice.getStatus().name()));
        }
    }

    /**
     * Ticari veri güvenliği için, bir faturanın statüsünün işleme uygun olup olmadığını kontrol eder.
     */
    private void checkIfInvoiceIsAlreadyRefunded(Invoice invoice) {
        if (invoice.getStatus() == InvoiceStatus.REFUNDED) {
            throw new BusinessException(messageService.getMessage(
                    Messages.Invoice.INVOICE_ALREADY_REFUNDED));
        }
    }

    /**
     * Ticari veri güvenliği için, bir faturanın statüsünün işleme uygun olup olmadığını kontrol eder.
     */
    private void checkIfInvoiceStatusIsPaid(Invoice invoice) {
        if (invoice.getStatus() != InvoiceStatus.PAID) {
            throw new BusinessException(messageService.getMessage(
                    Messages.Invoice.INVOICE_STATUS_MUST_BE_PAID));
        }
    }

}