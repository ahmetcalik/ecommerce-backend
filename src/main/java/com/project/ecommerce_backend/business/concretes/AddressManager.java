package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.AddressService;
import com.project.ecommerce_backend.business.abstracts.CountryService;
import com.project.ecommerce_backend.business.abstracts.CustomerService;
import com.project.ecommerce_backend.business.abstracts.OrderService;
import com.project.ecommerce_backend.business.dtos.requests.address.AddressRequest;
import com.project.ecommerce_backend.business.dtos.responses.address.AddressDetailResponse;
import com.project.ecommerce_backend.business.dtos.responses.address.ListAddressResponse;
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
import com.project.ecommerce_backend.repositories.abstracts.AddressRepository;
import com.project.ecommerce_backend.repositories.abstracts.CustomerAddressRepository;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Adres ve Müşteri-Adres ilişkilerinin yönetildiği, sistemin coğrafi veri trafiğini kontrol eden merkezi iş mantığı servisidir.
 * Bu sınıf; fiziksel adres verilerinin tekilleştirilerek kaydedilmesi, bu adreslerin ilgili müşterilerle güvenli bir şekilde ilişkilendirilmesi, varsayılan kargo ve fatura adresi seçimlerinin dinamik yönetimi ve Redis üzerinden gerçekleştirilen yüksek performanslı önbellekleme stratejilerinin tüm süreçlerini koordine eder.
 */
@Service
public class AddressManager implements AddressService {

    private final AddressRepository addressRepository;
    private final CustomerAddressRepository customerAddressRepository;
    private final CustomerService customerService;
    private final CountryService countryService;
    private final ModelMapperService modelMapperService;
    private final MessageService messageService;
    private final OrderService orderService;
    private final CacheHelper cacheHelper;

    public AddressManager(AddressRepository addressRepository,
                          CustomerAddressRepository customerAddressRepository,
                          CustomerService customerService,
                          CountryService countryService,
                          @Lazy OrderService orderService,
                          ModelMapperService modelMapperService,
                          MessageService messageService,
                          CacheHelper cacheHelper) {
        this.addressRepository = addressRepository;
        this.customerAddressRepository = customerAddressRepository;
        this.customerService = customerService;
        this.countryService = countryService;
        this.orderService = orderService;
        this.modelMapperService = modelMapperService;
        this.messageService = messageService;
        this.cacheHelper = cacheHelper;
    }

    /**
     * Kimliği doğrulanmış kullanıcının sisteminde kayıtlı olan özel bir adresin tüm teknik ve fiziksel detaylarını getirir.
     * Veri çekme işlemi sırasında güvenlik protokolü gereği önce talep edilen adresin gerçekten o kullanıcıya ait olup olmadığı kontrol edilir; performans optimizasyonu amacıyla sorgu sonucu kullanıcının eşsiz kimliği ve adres ID'si ile birleştirilerek Redis üzerinde önbelleğe alınır.
     *
     * @param id Detaylı bilgileri görüntülenmek istenen adresin veritabanındaki eşsiz anahtarı (ID).
     * @return İşlem başarılı ise adresin tüm hiyerarşik verilerini içeren AddressDetailResponse nesnesini veri sonuç kalıbı içinde döner.
     * @throws NotFoundException Belirtilen ID ile bir adres bulunamazsa veya bulunan adres giriş yapmış kullanıcıyla eşleşmezse fırlatılır.
     */
    @Override
    @Cacheable(value = "customerAddress", key = "'detail:' + @cacheHelper.getAuthenticatedUserId() + ':' + #id")
    public DataResult<AddressDetailResponse> getById(Long id) {
        Long authenticatedUserId = cacheHelper.getAuthenticatedUserId();

        CustomerAddress customerAddressLink = customerAddressRepository.findByAddressIdAndCustomerId(id, authenticatedUserId)
                .orElseThrow(() -> new NotFoundException(
                        messageService.getMessageWithParams(
                                Messages.Address.ADDRESS_NOT_FOUND_WITH_GIVEN_ID_OR_DOES_NOT_BELONG_TO_CUSTOMER, id)));

        AddressDetailResponse response = modelMapperService.getMapper().map(customerAddressLink, AddressDetailResponse.class);
        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.Address.ADDRESS_DETAIL_SUCCESSFULLY_LISTED));
    }

    /**
     * Sisteme giriş yapmış olan kullanıcının hesabına tanımlı tüm adres kayıtlarını, performans dostu bir sayfalama yapısıyla liste halinde sunar.
     * Jackson serileştirme süreçlerinde yaşanabilecek polimorfik yapı hatalarını kökten engellemek için veritabanından gelen sayfa içeriği standart Java List yapısına dönüştürülür; sonuçlar kullanıcı kimliği, sayfa indeksi ve sıralama tercihleri gibi parametrelerle eşleşecek şekilde Redis üzerinde saklanır.
     *
     * @param pageable Sayfa numarası, sayfa boyutu ve verilerin hangi kritere göre sıralanacağını belirleyen konfigürasyon nesnesi.
     * @return Kullanıcının adres koleksiyonunu, sistem mesajıyla birlikte başarılı bir veri transfer nesnesi (DataResult) içinde döner.
     */
    @Override
    @Cacheable(value = "customerAddress", key = "'list:' + @cacheHelper.getAuthenticatedUserId() + ':' + #pageable.pageNumber + ':' + #pageable.pageSize + ':' + #pageable.sort.toString()")
    public DataResult<List<ListAddressResponse>> getAll(Pageable pageable) {
        Long authenticatedUserId = cacheHelper.getAuthenticatedUserId();

        Slice<ListAddressResponse> addressSlice = addressRepository.findSliceByCustomerIdAsDto(authenticatedUserId, pageable);

        List<ListAddressResponse> response = addressSlice.getContent().stream()
                .map(address -> modelMapperService.getMapper().map(address, ListAddressResponse.class))
                .toList();

        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.Address.ADDRESSES_SUCCESSFULLY_LISTED));
    }

    /**
     * Sisteme yeni bir fiziksel adres tanımlar veya mevcut bir adresi kullanıcı portföyüne güvenli bir şekilde dahil eder.
     * İş akışı kapsamında önce adresin sistemde mükerrer kaydı olup olmadığı kontrol edilerek veri tekilliği sağlanır; eğer yeni eklenen kayıt "varsayılan" olarak işaretlenmişse kullanıcının eski varsayılan bayrakları otomatik olarak temizlenir ve işlem sonunda kullanıcının güncelliğini yitirmiş tüm adres önbellek listeleri (Cache Evict) sistemden temizlenir.
     *
     * @param request Yeni eklenecek adresin sokak, şehir, posta kodu ve ülke gibi detaylarını barındıran veri transfer nesnesi.
     * @return İşlem sonucunda oluşturulan veya eşleşen adresin güncel detaylarını başarılı sonuç kalıbıyla döner.
     */
    @Caching(evict = {
            @CacheEvict(value = "customerAddress", key = "'list:' + @cacheHelper.getAuthenticatedUserId()", allEntries = true),
            @CacheEvict(value = "customerAddress", key = "'detail:' + @cacheHelper.getAuthenticatedUserId() + ':' + #result.data.id", condition = "#result.success")
    })
    @Override
    @Transactional
    public DataResult<AddressDetailResponse> add(AddressRequest request) {
        Long authenticatedUserId = cacheHelper.getAuthenticatedUserId();
        Customer customer = customerService.getByIdAsEntity(authenticatedUserId);

        Address addressToLink = findOrCreateAddressForCustomer(request, authenticatedUserId);
        handleDefaultAddressFlags(request, authenticatedUserId);

        CustomerAddressId customerAddressId = new CustomerAddressId(customer.getId(), addressToLink.getId());
        CustomerAddress customerAddressLink = CustomerAddress.builder()
                .id(customerAddressId)
                .customer(customer)
                .address(addressToLink)
                .isShippingAddress(request.getIsShippingAddress())
                .isBillingAddress(request.getIsBillingAddress())
                .build();
        CustomerAddress savedCustomerAddressLink = customerAddressRepository.save(customerAddressLink);

        AddressDetailResponse response = modelMapperService.getMapper().map(savedCustomerAddressLink, AddressDetailResponse.class);
        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.Address.ADDRESS_SUCCESSFULLY_ADDED));
    }

    /**
     * Kullanıcının mevcut bir adres kaydı üzerindeki bilgileri güncelleyerek veri tabanı ve önbellek sistemini senkronize eder.
     * Güvenlik katmanı sadece kullanıcının kendi sahipliğindeki adresleri değiştirmesine izin verirken, güncelleme işlemi tamamlandığında hem genel liste önbelleği hem de o adrese özel detay önbelleği otomatik olarak geçersiz kılınarak kullanıcıya her zaman en güncel bilginin sunulması garanti edilir.
     *
     * @param addressId Bilgileri revize edilecek olan hedef adres kaydının kimlik numarası.
     * @param request Adresin güncellenecek yeni değerlerini (şehir, adres satırı vb.) içeren talep nesnesi.
     * @return Güncelleme sonrası adresin son halini içeren detay nesnesini başarı geri bildirimiyle döner.
     */
    @Caching(evict = {
            @CacheEvict(value = "customerAddress", key = "'list:' + @cacheHelper.getAuthenticatedUserId()", allEntries = true),
            @CacheEvict(value = "customerAddress", key = "'detail:' + @cacheHelper.getAuthenticatedUserId() + ':' + #addressId")
    })
    @Override
    @Transactional
    public DataResult<AddressDetailResponse> update(Long addressId, AddressRequest request) {
        Long authenticatedUserId = cacheHelper.getAuthenticatedUserId();
        CustomerAddress customerAddressLink = customerAddressRepository.findByAddressIdAndCustomerId(addressId, authenticatedUserId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Address.ADDRESS_NOT_FOUND_WITH_GIVEN_ID_OR_DOES_NOT_BELONG_TO_CUSTOMER, addressId)));

        handleDefaultAddressFlags(request, authenticatedUserId);

        Address addressToUpdate = customerAddressLink.getAddress();
        Country country = countryService.getByIdAsEntity(request.getCountryId());

        modelMapperService.getMapper().map(request, addressToUpdate);
        addressToUpdate.setCountry(country);
        addressRepository.save(addressToUpdate);

        customerAddressLink.setIsShippingAddress(request.getIsShippingAddress());
        customerAddressLink.setIsBillingAddress(request.getIsBillingAddress());
        CustomerAddress updatedCustomerAddressLink = customerAddressRepository.save(customerAddressLink);

        AddressDetailResponse response = modelMapperService.getMapper().map(updatedCustomerAddressLink, AddressDetailResponse.class);
        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.Address.ADDRESS_SUCCESSFULLY_UPDATED));
    }

    /**
     * Belirtilen adres kaydını kullanıcının listesinden güvenli bir şekilde kaldırır ve sistem genelinde veri temizliği yapar.
     * Silme işlemi öncesinde adresin aktif bir siparişte kullanılıp kullanılmadığı kontrol edilerek ticari veri bütünlüğü korunur; silinen kayıt varsayılan bir adres ise sistem otomatik olarak kullanıcının diğer adreslerinden birini yeni varsayılan olarak atar ve eğer adres başka hiçbir kullanıcı tarafından referans gösterilmiyorsa fiziksel olarak da sistemden temizlenir.
     *
     * @param addressId Sistemden kaldırılması talep edilen adresin kimlik numarası.
     * @return Silme işleminin başarıyla tamamlandığını veya olası kısıtlamalar nedeniyle iptal edildiğini bildiren sonuç nesnesi.
     */
    @Caching(evict = {
            @CacheEvict(value = "customerAddress", key = "'list:' + @cacheHelper.getAuthenticatedUserId()", allEntries = true),
            @CacheEvict(value = "customerAddress", key = "'detail:' + @cacheHelper.getAuthenticatedUserId() + ':' + #addressId")
    })
    @Override
    @Transactional
    public Result delete(Long addressId) {
        checkIfAddressIsInUseByAnOrder(addressId);

        Long authenticatedUserId = cacheHelper.getAuthenticatedUserId();
        CustomerAddress linkToDelete = customerAddressRepository.findByAddressIdAndCustomerId(addressId, authenticatedUserId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Address.ADDRESS_NOT_FOUND_WITH_GIVEN_ID_OR_DOES_NOT_BELONG_TO_CUSTOMER, addressId)));

        boolean wasDefaultShipping = linkToDelete.getIsShippingAddress();
        boolean wasDefaultBilling = linkToDelete.getIsBillingAddress();
        Long deletedAddressId = linkToDelete.getAddress().getId();

        customerAddressRepository.delete(linkToDelete);

        handleDefaultAddressReassignment(wasDefaultShipping, wasDefaultBilling, authenticatedUserId, deletedAddressId);

        deleteAddressIfOrphaned(deletedAddressId);

        return new SuccessResult(messageService.getMessage(
                Messages.Address.ADDRESS_SUCCESSFULLY_DELETED));
    }

    /**
     * Veritabanında kayıtlı olan bir adresin ham fiziksel bilgilerini, herhangi bir kullanıcı ilişkisinden bağımsız olarak entity formatında getirir.
     * Bu metot genellikle sistem içi içsel sorgularda ve diğer servislerin adres verisine ham erişim sağlaması gereken senaryolarda kullanılır; performans için sonuçlar doğrudan adres ID'si üzerinden önbelleğe alınır.
     * * @param id Veritabanında sorgulanacak adresin eşsiz kimliği.
     * @return Sorgulanan adresi temsil eden Address entity nesnesini döner.
     */
    @Override
    @Cacheable(value = "address", key = "#id")
    public Address getByIdAsEntity(Long id) {
        return addressRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Address.ADDRESS_NOT_FOUND_WITH_GIVEN_ID_OR_DOES_NOT_BELONG_TO_CUSTOMER, id)));
    }

    /**
     * Belirli bir adresin belirli bir müşteriye ait olup olmadığını doğrulayarak, o adrese ait fiziksel entity nesnesini döner.
     * Bu işlem, özellikle güvenlik kontrollerinin manuel olarak işletilmesi gereken veya müşteri bazlı adres doğrulaması yapılması gereken iş akışlarında referans olarak kullanılır.
     * * @param id Sorgulanan adresin kimlik numarası.
     * @param customerId Adresin sahibi olduğu varsayılan müşterinin kimlik numarası.
     * @return Doğrulama başarılı ise ilgili Address entity nesnesini döner.
     */
    @Override
    public Address getByIdAndCustomerId(Long id, Long customerId) {
        return customerAddressRepository.findByAddressIdAndCustomerId(id, customerId)
                .map(CustomerAddress::getAddress)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Address.ADDRESS_NOT_FOUND_WITH_GIVEN_ID_OR_DOES_NOT_BELONG_TO_CUSTOMER, id)));
    }

    // --- YARDIMCI METOTLAR ---

    /**
     * Yeni bir adres kaydı oluşturulmadan önce sistemde aynı detaylara sahip fiziksel bir adresin olup olmadığını araştırır.
     * Eğer sistemde aynı verilerle kayıtlı bir adres varsa onu geri döndürür, aksi halde yeni bir fiziksel adres kaydı oluşturur; bu süreç veritabanında adres verilerinin gereksiz yere çoğalmasını (data duplication) engelleyen bir tekilleştirme filtresi görevi görür.
     */
    private Address findOrCreateAddressForCustomer(AddressRequest request, Long customerId) {
        Optional<Address> existingAddressOpt = addressRepository.findByAddressLine1AndAddressLine2AndCityAndPostalCodeAndCountryId(
                request.getAddressLine1(), request.getAddressLine2(), request.getCity(),
                request.getPostalCode(), request.getCountryId());

        if (existingAddressOpt.isPresent()) {
            Address foundAddress = existingAddressOpt.get();
            boolean isAlreadyLinked = customerAddressRepository.existsByCustomerIdAndAddressId(
                    customerId, foundAddress.getId());
            if (isAlreadyLinked) {
                throw new BusinessException(messageService.getMessage(
                        Messages.Address.ADDRESS_ALREADY_EXISTS));
            }
            return foundAddress;
        } else {
            Country country = countryService.getByIdAsEntity(request.getCountryId());
            Address newAddress = modelMapperService.getMapper().map(request, Address.class);
            newAddress.setId(null);
            newAddress.setCountry(country);
            return addressRepository.save(newAddress);
        }
    }

    /**
     * Kullanıcının kargo veya fatura adresi seçimlerini kontrol ederek, sistemde aynı anda sadece bir tane "varsayılan" kargo ve fatura adresi bulunmasını sağlar.
     * Eğer kullanıcı yeni bir adresi varsayılan olarak seçerse, bu metot eski varsayılan adreslerin üzerindeki bayrakları temizleme sürecini koordine eder.
     */
    private void handleDefaultAddressFlags(AddressRequest request, Long customerId) {
        if (request.getIsShippingAddress()) {
            unsetDefaultShippingAddress(customerId);
        }
        if (request.getIsBillingAddress()) {
            unsetDefaultBillingAddress(customerId);
        }
    }

    /**
     * Kullanıcının mevcut adresleri arasından "varsayılan kargo adresi" olarak işaretlenmiş olan kaydı bulur ve bu önceliği kaldırır.
     */
    private void unsetDefaultShippingAddress(Long customerId) {
        customerAddressRepository.findByCustomerIdAndIsShippingAddressTrue(customerId)
                .forEach(oldDefault -> {
                    oldDefault.setIsShippingAddress(false);
                    customerAddressRepository.save(oldDefault);
                });
    }

    /**
     * Kullanıcının mevcut adresleri arasından "varsayılan fatura adresi" olarak işaretlenmiş olan kaydı bulur ve bu önceliği kaldırır.
     */
    private void unsetDefaultBillingAddress(Long customerId) {
        customerAddressRepository.findByCustomerIdAndIsBillingAddressTrue(customerId)
                .forEach(oldDefault -> {
                    oldDefault.setIsBillingAddress(false);
                    customerAddressRepository.save(oldDefault);
                });
    }

    /**
     * Kullanıcının varsayılan olarak kullandığı bir adres silindiğinde, geride kalan adresleri analiz ederek sisteme yeni bir varsayılan adres atama sürecini başlatır.
     */
    private void handleDefaultAddressReassignment(boolean wasDefaultShipping, boolean wasDefaultBilling, Long customerId, Long deletedAddressId) {
        if (wasDefaultShipping) {
            assignNewDefaultAddress(customerId, deletedAddressId, true);
        }
        if (wasDefaultBilling) {
            assignNewDefaultAddress(customerId, deletedAddressId, false);
        }
    }

    /**
     * Bir adres silindiğinde kullanıcı deneyiminin kesintiye uğramaması adına, sistemde kalan diğer adresler arasından uygun olanı otomatik olarak yeni varsayılan kargo veya fatura adresi olarak belirler.
     */
    private void assignNewDefaultAddress(Long customerId, Long deletedAddressId, boolean isShipping) {
        List<CustomerAddress> potentialNewDefaults = customerAddressRepository.findPotentialNewDefaultAddresses(customerId, deletedAddressId);

        potentialNewDefaults.stream().findFirst().ifPresent(newDefaultLink -> {
            if (isShipping) {
                newDefaultLink.setIsShippingAddress(true);
            } else {
                newDefaultLink.setIsBillingAddress(true);
            }
            customerAddressRepository.save(newDefaultLink);
        });
    }

    /**
     * Sistemde artık hiçbir kullanıcı tarafından referans gösterilmeyen "yetim" (orphan) adres kayıtlarını tespit eder ve veritabanı temizliği için fiziksel olarak siler.
     */
    private void deleteAddressIfOrphaned(Long addressId) {
        long remainingLinks = customerAddressRepository.countByAddressId(addressId);
        if (remainingLinks == 0) {
            addressRepository.deleteById(addressId);
        }
    }

    /**
     * Kritik veri bütünlüğü kontrolü kapsamında, bir adresin geçmiş veya mevcut herhangi bir siparişle bağlantılı olup olmadığını denetleyerek faturalandırma ve geçmiş veri takibi süreçlerinin güvenliğini sağlar.
     */
    private void checkIfAddressIsInUseByAnOrder(Long addressId) {
        if (orderService.existsByShippingAddressId(addressId)) {
            throw new BusinessException(messageService.getMessage(
                    Messages.Errors.ADDRESS_CAN_NOT_BE_DELETED_BECAUSE_OF_ACTIVE_ORDER));
        }
    }
}