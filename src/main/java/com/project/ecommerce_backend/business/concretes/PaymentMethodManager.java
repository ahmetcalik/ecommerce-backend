package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.*;
import com.project.ecommerce_backend.business.dtos.requests.payment.AddPaymentMethodRequest;
import com.project.ecommerce_backend.business.dtos.requests.payment.UpdatePaymentMethodRequest;
import com.project.ecommerce_backend.business.dtos.responses.payment.ListPaymentMethodResponse;
import com.project.ecommerce_backend.business.dtos.responses.payment.PaymentMethodDetailResponse;
import com.project.ecommerce_backend.business.helpers.CacheHelper;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.utils.mapper.ModelMapperService;
import com.project.ecommerce_backend.core.utils.result.*;
import com.project.ecommerce_backend.entities.concretes.Customer;
import com.project.ecommerce_backend.entities.concretes.PaymentMethod;
import com.project.ecommerce_backend.entities.concretes.PaymentType;
import com.project.ecommerce_backend.entities.enums.PaymentTypeEnum;
import com.project.ecommerce_backend.repositories.abstracts.PaymentMethodRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Kullanıcıların ödeme araçlarını güvenli bir şekilde yöneten, kart tokenizasyonu ve finansal doğrulama süreçlerini koordine eden merkez servisidir.
 * Bu sınıf; hassas kart verilerinin maskelenmiş formatta (son 4 hane) saklanması, kartın ait olduğu banka ailesinin tespiti, mükerrer kayıt kontrolü ve aktif aboneliklerle olan ilişkilerin denetlenmesi gibi süreçlerden sorumludur. Kullanıcıya özel finansal verileri Redis üzerinde çok katmanlı bir yapıyla önbelleğe alarak hem güvenliği hem de ödeme sayfasındaki işlem hızını optimize eder.
 */
@Service
public class PaymentMethodManager implements PaymentMethodService {

    private final PaymentMethodRepository paymentMethodRepository;
    private final CustomerService customerService;
    private final BinLookupService binLookupService;
    private final SubscriptionService subscriptionService;
    private final PaymentTypeService paymentTypeService;
    private final ModelMapperService modelMapperService;
    private final MessageService messageService;
    private final PaymentMethodService self;
    private final CacheHelper cacheHelper;

    public PaymentMethodManager(
            PaymentMethodRepository paymentMethodRepository,
            CustomerService customerService,
            BinLookupService binLookupService,
            @Lazy SubscriptionService subscriptionService,
            PaymentTypeService paymentTypeService,
            ModelMapperService modelMapperService,
            MessageService messageService,
            @Lazy PaymentMethodService self,
            CacheHelper cacheHelper) {
        this.paymentMethodRepository = paymentMethodRepository;
        this.customerService = customerService;
        this.binLookupService = binLookupService;
        this.subscriptionService = subscriptionService;
        this.paymentTypeService = paymentTypeService;
        this.modelMapperService = modelMapperService;
        this.messageService = messageService;
        this.self = self;
        this.cacheHelper = cacheHelper;
    }

    /**
     * Oturum açmış olan kullanıcının sistemde kayıtlı tüm ödeme yöntemlerini liste halinde sunar.
     * Kullanıcı deneyimini hızlandırmak adına, müşterinin ödeme araçları listesi kendi kullanıcı kimliği üzerinden Redis hafızasında saklanır; böylece ödeme adımlarında veya profil sayfasında bu verilere anlık olarak erişilebilir.
     *
     * @return Kullanıcının kayıtlı ödeme yöntemlerini barındıran başarılı bir veri sonucu döner.
     */
    @Override
    @Cacheable(value = "paymentMethod", key = "'customer:' + @cacheHelper.getAuthenticatedUserId()")
    public DataResult<List<ListPaymentMethodResponse>> getAll() {
        Long authenticatedUserId = cacheHelper.getAuthenticatedUserId();
        List<PaymentMethod> methods = paymentMethodRepository.findAllByCustomerId(authenticatedUserId);
        List<ListPaymentMethodResponse> response = methods.stream()
                .map(method -> modelMapperService.getMapper().map(method, ListPaymentMethodResponse.class))
                .toList();
        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.PaymentMethod.PAYMENT_METHOD_SUCCESSFULLY_LISTED));
    }

    /**
     * Belirli bir ödeme yönteminin tüm teknik detaylarını, sahiplik doğrulaması yaparak getirir.
     * Güvenlik protokolü gereği, talep edilen ödeme yönteminin gerçekten oturum açmış kullanıcıya ait olup olmadığı `getByIdAndCustomerId` katmanında denetlenir; başarılı sorgular ID bazlı olarak Redis'te önbelleğe alınır.
     *
     * @param id Detayları istenen ödeme yönteminin benzersiz kimlik numarasıdır.
     * @return Ödeme yöntemine ait detaylı profil bilgilerini döner.
     * @throws NotFoundException Ödeme yöntemi bulunamazsa veya kullanıcıya ait değilse fırlatılır.
     */
    @Override
    @Cacheable(value = "paymentMethod", key = "#id")
    public DataResult<PaymentMethodDetailResponse> getById(Long id) {
        Long authenticatedUserId = cacheHelper.getAuthenticatedUserId();
        PaymentMethod method = self.getByIdAndCustomerId(id, authenticatedUserId);
        PaymentMethodDetailResponse response = modelMapperService.getMapper().map(method, PaymentMethodDetailResponse.class);
        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.PaymentMethod.PAYMENT_METHOD_DETAIL_SUCCESSFULLY_LISTED));
    }

    /**
     * Sisteme yeni bir ödeme yöntemi tanımlar ve güvenli bir dijital kart token'ı üretir.
     * İş akışı kapsamında; kart numarasının ilk altı hanesiyle (BIN) kart ailesi tespit edilir, veri tekilliği için aynı kartın son dört hanesi ve son kullanma tarihiyle mükerrerlik kontrolü yapılır. Kart numarası asla tam olarak saklanmaz, bunun yerine sadece son dört hane kaydedilir ve işlem sonunda kullanıcının güncel ödeme listesi önbellekten temizlenerek verinin tazeliği sağlanır.
     *
     * @param request Kart numarası, ham sahibi ve son kullanma tarihi gibi bilgileri içeren kayıt talebidir.
     * @return Kaydedilen ödeme yönteminin maskelenmiş bilgilerini ve sistem tarafından atanan token'ı döner.
     */
    @Caching(evict = {
            @CacheEvict(value = "paymentMethod", key = "#result.data.id"),
            @CacheEvict(value = "paymentMethod", key = "'secure:' + #result.data.id"),
            @CacheEvict(value = "paymentMethod", key = "'customer:' + @cacheHelper.getAuthenticatedUserId()")
    })
    @Override
    @Transactional
    public DataResult<PaymentMethodDetailResponse> add(AddPaymentMethodRequest request) {
        Long authenticatedUserId = cacheHelper.getAuthenticatedUserId();
        Customer customer = customerService.getByIdAsEntity(authenticatedUserId);

        PaymentType creditCardType = paymentTypeService.getByName(PaymentTypeEnum.CREDIT_CARD);

        String cardNumber = request.getCardNumber();
        String lastFourDigits = cardNumber.substring(cardNumber.length() - 4);

        checkIfPaymentMethodExists(authenticatedUserId, lastFourDigits, request.getExpiryMonth(), request.getExpiryYear());

        String binNumber = cardNumber.substring(0, 6);
        String cardFamily = binLookupService.getCardFamilyByBinNumber(binNumber);

        PaymentMethod paymentMethod = PaymentMethod.builder()
                .customer(customer)
                .paymentType(creditCardType)
                .cardHolderName(request.getCardHolderName())
                .lastFourDigits(lastFourDigits)
                .cardFamily(cardFamily)
                .expiryMonth(request.getExpiryMonth())
                .expiryYear(request.getExpiryYear())
                .cardToken(UUID.randomUUID().toString())
                .build();

        PaymentMethod savedMethod = paymentMethodRepository.save(paymentMethod);
        PaymentMethodDetailResponse response = modelMapperService.getMapper().map(savedMethod, PaymentMethodDetailResponse.class);
        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.PaymentMethod.PAYMENT_METHOD_SUCCESSFULLY_ADDED));
    }

    /**
     * Mevcut bir ödeme yönteminin bilgilerini günceller.
     * Güncelleme işlemi sadece kartın sahibine açık olup, işlem tamamlandığında hem genel ödeme listesi hem de o karta özel detay/güvenli önbellekleri otomatik olarak geçersiz kılınır.
     */
    @Caching(evict = {
            @CacheEvict(value = "paymentMethod", key = "#paymentMethodId"),
            @CacheEvict(value = "paymentMethod", key = "'secure:' + #paymentMethodId"),
            @CacheEvict(value = "paymentMethod", key = "'customer:' + @cacheHelper.getAuthenticatedUserId()")
    })
    @Override
    @Transactional
    public DataResult<PaymentMethodDetailResponse> update(Long paymentMethodId, UpdatePaymentMethodRequest request) {
        Long authenticatedUserId = cacheHelper.getAuthenticatedUserId();
        PaymentMethod methodToUpdate = self.getByIdAndCustomerId(paymentMethodId, authenticatedUserId);

        modelMapperService.getMapper().map(request, methodToUpdate);
        PaymentMethod updatedMethod = paymentMethodRepository.save(methodToUpdate);

        PaymentMethodDetailResponse response = modelMapperService.getMapper().map(updatedMethod, PaymentMethodDetailResponse.class);
        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.PaymentMethod.PAYMENT_METHOD_SUCCESSFULLY_UPDATED));
    }

    /**
     * Belirtilen ödeme yöntemini kullanıcının profilinden kalıcı olarak kaldırır.
     * Kritik iş kuralı denetimi gereği; eğer bu ödeme yöntemi devam eden aktif bir abonelik tarafından kullanılıyorsa, finansal kopukluk yaşanmaması adına silme işlemi engellenir; aksi durumda kayıt silinir ve ilgili tüm önbellek alanları temizlenir.
     *
     * @param paymentMethodId Sistemden kaldırılacak ödeme yönteminin kimliği.
     * @return Silme işleminin başarı durumunu döner.
     * @throws BusinessException Ödeme yöntemi aktif bir aboneliğe bağlıysa fırlatılır.
     */
    @Caching(evict = {
            @CacheEvict(value = "paymentMethod", key = "#paymentMethodId"),
            @CacheEvict(value = "paymentMethod", key = "'secure:' + #paymentMethodId"),
            @CacheEvict(value = "paymentMethod", key = "'customer:' + @cacheHelper.getAuthenticatedUserId()")
    })
    @Override
    @Transactional
    public Result delete(Long paymentMethodId) {
        checkIfPaymentMethodIsInUse(paymentMethodId);

        Long authenticatedUserId = cacheHelper.getAuthenticatedUserId();
        PaymentMethod methodToDelete = self.getByIdAndCustomerId(paymentMethodId, authenticatedUserId);

        paymentMethodRepository.delete(methodToDelete);
        return new SuccessResult(messageService.getMessage(
                Messages.PaymentMethod.PAYMENT_METHOD_SUCCESSFULLY_DELETED));
    }

    /**
     * Ödeme yöntemini, talep eden müşterinin sahipliğini doğrulayarak entity formatında geri döndürür.
     * Bu metot; özellikle ödeme süreçlerinde veya dahili servis çağrılarında güvenlik bariyeri olarak görev yapar.
     */
    @Override
    public PaymentMethod getByIdAndCustomerId(Long id, Long customerId) {
        return paymentMethodRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessage(
                        Messages.PaymentMethod.PAYMENT_METHOD_NOT_FOUND_FOR_CUSTOMER)));
    }

    // --- YARDIMCI METOTLAR ---

    /**
     * Aynı kullanıcının aynı kartı birden fazla kez kaydetmesini engelleyerek veritabanı kirliliğinin önüne geçer.
     */
    private void checkIfPaymentMethodExists(Long customerId, String lastFour, Integer expMonth, Integer expYear) {
        if (paymentMethodRepository.existsByCustomerIdAndLastFourDigitsAndExpiryMonthAndExpiryYear(
                customerId, lastFour, expMonth, expYear)) {
            throw new BusinessException(messageService.getMessage(
                    Messages.PaymentMethod.PAYMENT_METHOD_ALREADY_EXISTS));
        }
    }

    /**
     * Bir kartın sistemden silinmeden önce aktif bir finansal taahhüde (Abonelik) bağlı olup olmadığını denetleyerek tahsilat süreçlerini koruma altına alır.
     */
    private void checkIfPaymentMethodIsInUse(Long paymentMethodId) {
        if (subscriptionService.isPaymentMethodInUseByActiveSubscription(paymentMethodId)) {
            throw new BusinessException(messageService.getMessage(
                    Messages.PaymentMethod.PAYMENT_METHOD_CANNOT_BE_DELETED_BECAUSE_ACTIVE_SUBSCRIPTION));
        }
    }
}