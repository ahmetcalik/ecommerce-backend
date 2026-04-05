package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.CustomerService;
import com.project.ecommerce_backend.core.security.abstracts.JwtService;
import com.project.ecommerce_backend.business.abstracts.RoleService;
import com.project.ecommerce_backend.business.dtos.requests.auth.RegisterRequest;
import com.project.ecommerce_backend.business.dtos.responses.auth.AuthenticationResponse;
import com.project.ecommerce_backend.business.helpers.CacheHelper;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.security.details.CustomerDetails;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.Result;
import com.project.ecommerce_backend.core.utils.result.SuccessDataResult;
import com.project.ecommerce_backend.core.utils.result.SuccessResult;
import com.project.ecommerce_backend.entities.concretes.Customer;
import com.project.ecommerce_backend.entities.concretes.Role;
import com.project.ecommerce_backend.repositories.abstracts.CustomerRepository;
import org.springframework.cache.Cache;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

/**
 * Sistemin güvenlik ve kullanıcı yönetim merkezidir; kullanıcıların kayıt (registration), yetkilendirme (authorization) ve yaşam döngüsü süreçlerini Spring Security protokolleriyle uyumlu bir şekilde yönetir.
 * Bu sınıf; şifrelerin güvenli bir şekilde hashlenmesi, JWT tabanlı oturum yönetimi, rol tabanlı erişim kontrolü (RBAC) ve sistem bütünlüğünü korumak adına son adminin silinmesini engelleyen iş kurallarını koordine ederken, kullanıcı verilerini hem kimlik (ID) hem de e-posta bazlı olarak Redis üzerinde yüksek performansla önbelleğe alır.
 */
@Service
public class CustomerManager implements CustomerService, UserDetailsService {

    private final CustomerRepository customerRepository;
    private final RoleService roleService;
    private final MessageService messageService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final CacheManager cacheManager;
    private final CacheHelper cacheHelper;

    public CustomerManager(
            CustomerRepository customerRepository,
            RoleService roleService,
            @Lazy PasswordEncoder passwordEncoder,
            JwtService jwtService,
            MessageService messageService,
            CacheManager cacheManager,
            CacheHelper cacheHelper) {
        this.customerRepository = customerRepository;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.messageService = messageService;
        this.cacheManager = cacheManager;
        this.cacheHelper = cacheHelper;
    }

    private final String role = "ROLE_";

    /**
     * Yeni bir kullanıcının sisteme ilk giriş kaydını oluşturur ve oturum açabilmesi için gerekli güvenlik anahtarlarını (JWT) üretir.
     * İş akışı kapsamında e-posta adresinin tekilliği doğrulandıktan sonra şifre güvenli bir şekilde hashlenir, kullanıcıya varsayılan 'ROLE_USER' yetkisi atanır ve kayıt işlemi tamamlandığında kullanıcıya anında oturum açabilmesi için erişim (access) ve yenileme (refresh) token'ları sunulur.
     *
     * @param request Kullanıcının iletişim bilgilerini ve giriş şifresini barındıran kayıt talep nesnesidir.
     * @return Başarıyla oluşturulan kullanıcı hesabı için üretilen JWT ve süre bilgilerini içeren AuthenticationResponse nesnesini döner.
     * @throws BusinessException E-posta adresi sistemde zaten kayıtlıysa fırlatılır.
     */
    @Override
    @Transactional
    public DataResult<AuthenticationResponse> register(RegisterRequest request) {
        checkIfEmailAlreadyExists(request.getEmailAddress());

        Role userRole = roleService.findRoleByName("ROLE_USER");
        Set<Role> roles = new HashSet<>();
        roles.add(userRole);

        Customer customer = Customer.builder()
                .contactName(request.getContactName())
                .emailAddress(request.getEmailAddress())
                .phoneNumber(request.getPhoneNumber())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .isActive(true)
                .roles(roles)
                .build();

        Customer savedCustomer = customerRepository.save(customer);
        UserDetails userDetails = new CustomerDetails(savedCustomer);

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        Date expirationDate = jwtService.extractExpiration(accessToken);

        AuthenticationResponse response = AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .issuedAt(new Date())
                .accessTokenExpiresAt(expirationDate)
                .build();

        return new SuccessDataResult<>(response, messageService.getMessage(
                Messages.Auth.REGISTER_SUCCESSFUL));
    }

    /**
     * Spring Security'nin kimlik doğrulama sürecinde kullandığı merkezi metottur; kullanıcıyı e-posta adresi üzerinden sorgulayarak güvenlik detaylarını yükler.
     * Giriş denemeleri yüksek trafik oluşturduğu için, sorgulama sonucu kullanıcının e-posta adresi üzerinden Redis'te önbelleğe alınır; böylece her istekte veritabanına gitmek yerine kullanıcı yetkileri ve kimlik bilgileri bellekten ışık hızında doğrulanır.
     *
     * @param username Kullanıcının sisteme giriş yaparken kullandığı benzersiz e-posta adresidir.
     * @return Kullanıcının kimlik bilgilerini ve yetkilerini içeren UserDetails nesnesini döner.
     * @throws UsernameNotFoundException Belirtilen e-posta adresiyle eşleşen aktif bir kullanıcı bulunamadığında fırlatılır.
     */
    @Override
    @Cacheable(value = "customer", key = "'email:' + #username")
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Customer customer = this.customerRepository.findByEmailAddress(username)
                .orElseThrow(() -> new UsernameNotFoundException(messageService.getMessageWithParams(
                        Messages.Auth.USER_NOT_FOUND_WITH_EMAIL_ADDRESS, username)));

        return new CustomerDetails(customer);
    }

    /**
     * Mevcut bir kullanıcıya sistem üzerinde ek yetkiler tanıyan yeni bir rol ataması gerçekleştirir.
     * İşlem sırasında rol ismi standart formatlara (ROLE_ prefix) dönüştürülür ve veritabanı kaydı güncellendiği an kullanıcının önbellekteki (cache) yetki bilgileri temizlenerek (Eviction) bir sonraki isteğinde yeni yetkilerinin aktif olması sağlanır.
     *
     * @param customerId Yetkilendirme yapılacak kullanıcının benzersiz numarasıdır.
     * @param roleName Atanacak rolün ismi (Örn: ADMIN, EDITOR).
     * @return İşlemin başarıyla tamamlandığını bildiren sonuç nesnesini döner.
     */
    @Override
    @Transactional
    @CacheEvict(value = "customer", key = "#customerId")
    public Result grantRoleToUser(Long customerId, String roleName) {
        String formattedRoleName = roleName.toUpperCase().startsWith(role) ? roleName.toUpperCase() : role + roleName.toUpperCase();

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Auth.USER_NOT_FOUND_WITH_ID, customerId)));

        Role roleToGrant = roleService.findRoleByName(formattedRoleName);

        customer.getRoles().add(roleToGrant);
        customerRepository.save(customer);

        return new SuccessResult(messageService.getMessage(
                Messages.Auth.ROLE_GRANTED_SUCCESSFULLY));
    }

    /**
     * Kullanıcının sahip olduğu belirli bir yetkiyi (rol) geri alır ve kullanıcının erişim sınırlarını yeniden düzenler.
     * Yetki iptali öncesinde kullanıcının gerçekten o role sahip olup olmadığı denetlenir ve işlem sonunda veri tutarlılığını korumak adına kullanıcının önbellekteki bilgileri geçersiz kılınır.
     *
     * @param customerId Yetkisi kısıtlanacak kullanıcının benzersiz numarasıdır.
     * @param roleName Geri alınacak rolün ismidir.
     * @return Rolün başarıyla iptal edildiğini bildiren sonuç nesnesini döner.
     */
    @Override
    @Transactional
    @CacheEvict(value = "customer", key = "#customerId")
    public Result revokeRoleFromUser(Long customerId, String roleName) {
        String formattedRoleName = roleName.toUpperCase().startsWith(role) ? roleName.toUpperCase() : role + roleName.toUpperCase();

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Auth.USER_NOT_FOUND_WITH_ID, customerId)));

        Role roleToRevoke = roleService.findRoleByName(formattedRoleName);

        checkIfUserHasRoleBeforeRevoke(customer, roleToRevoke);
        customer.getRoles().remove(roleToRevoke);
        customerRepository.save(customer);

        return new SuccessResult(messageService.getMessage(
                Messages.Auth.ROLE_REVOKED_SUCCESSFULLY));
    }

    /**
     * Kullanıcıyı sistemden (soft delete yöntemiyle) kaldırır ve tüm erişim haklarını dondurur.
     * Güvenlik protokolleri gereği; adminin kendi hesabını silmesi veya sistemdeki son yöneticinin (admin) kaldırılması gibi kritik hatalar bu aşamada engellenir; silme işlemi başarılı olduğunda ise kullanıcının hem ID hem de E-posta bazlı tüm önbellek kayıtları sistemden tamamen temizlenir.
     *
     * @param customerId Sistemden kaldırılacak olan kullanıcının benzersiz kimlik numarasıdır.
     * @return Silme işleminin başarı durumunu veya kısıtlamalar nedeniyle oluşan iptal gerekçesini döner.
     */
    @Override
    @Transactional
    @CacheEvict(value = "customer", key = "#customerId")
    public Result deleteUser(Long customerId) {
        Customer customerToDelete = customerRepository.findById(customerId)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Auth.USER_NOT_FOUND_WITH_ID, customerId)));

        String emailAddress = customerToDelete.getEmailAddress();

        checkDeleteConstraints(customerId, customerToDelete);

        customerToDelete.setIsActive(false);
        customerRepository.save(customerToDelete);

        evictCustomerEmailCache(emailAddress);

        return new SuccessResult(messageService.getMessage(
                Messages.Auth.USER_DELETED_SUCCESSFULLY));
    }

    /**
     * Kullanıcı bilgilerini veritabanından ham entity formatında, tüm nitelikleriyle birlikte sorgular.
     * Genellikle sistem içi ilişkilerin kurulması ve kullanıcı profili yönetimi gibi derinlemesine veri ihtiyacı duyulan senaryolarda kullanılır; performans kazanımı için sonuçlar kullanıcı ID'si üzerinden Redis'te önbelleğe alınır.
     *
     * @param id Bilgileri sorgulanacak kullanıcının eşsiz anahtarıdır.
     * @return Sorgulanan kullanıcıyı temsil eden somut Customer nesnesini döner.
     */
    @Override
    @Cacheable(value = "customer", key = "#id")
    public Customer getByIdAsEntity(Long id) {
        return customerRepository.findById(id)
                .orElseThrow(() -> new NotFoundException(messageService.getMessageWithParams(
                        Messages.Auth.USER_NOT_FOUND_WITH_ID, id)));
    }

    // --- YARDIMCI METOTLAR ---

    /**
     * Veri bütünlüğü ve kullanıcı deneyimi için, bir e-posta adresinin sistemde mükerrer kaydının oluşmasını engeller.
     */
    private void checkIfEmailAlreadyExists(String emailAddress) {
        if (this.customerRepository.findByEmailAddress(emailAddress).isPresent()) {
            throw new BusinessException(messageService.getMessage(
                    Messages.Auth.EMAIL_ALREADY_EXISTS));
        }
    }

    /**
     * Yetki iptal süreçlerinde, olmayan bir rolün silinmeye çalışılmasını engelleyerek tutarlı bir hata yönetimi sağlar.
     */
    private void checkIfUserHasRoleBeforeRevoke(Customer customer, Role roleToRevoke) {
        if (!customer.getRoles().contains(roleToRevoke)) {
            throw new BusinessException(messageService.getMessage(
                    Messages.Auth.USER_DOES_NOT_HAVE_ROLE));
        }
    }

    /**
     * Sistemin sürekliliğini koruyan kritik bir kontrol noktasıdır; yöneticilerin kendilerini silmesini veya sistemin "adminsiz" kalmasını engelleyerek yönetimsel kilitlenmelerin önüne geçer.
     */
    private void checkDeleteConstraints(Long customerId, Customer customerToDelete) {
        Long authenticatedUserId = cacheHelper.getAuthenticatedUserId();

        if (authenticatedUserId.equals(customerId)) {
            throw new BusinessException(messageService.getMessage(
                    Messages.Auth.ADMIN_CANNOT_DELETE_HIMSELF));
        }

        boolean isDeletingAdmin = customerToDelete.getRoles().stream()
                .anyMatch(userRole -> userRole.getName().equals("ROLE_ADMIN"));

        if (isDeletingAdmin && customerRepository.countByRoles_Name("ROLE_ADMIN") <= 1) {
            throw new BusinessException(messageService.getMessage(
                    Messages.Auth.CANNOT_DELETE_LAST_ADMIN));
        }
    }

    /**
     * Kullanıcı silindiğinde veya bilgileri değiştiğinde, e-posta tabanlı önbellek anahtarlarını null-safe bir şekilde temizleyerek güvenlik açıklarını ve bayat veri (stale data) kullanımını önler.
     */
    private void evictCustomerEmailCache(String emailAddress) {
        if (cacheManager != null) {
            Cache customerCache = cacheManager.getCache("customer");

            if (customerCache != null) {
                customerCache.evict("email:" + emailAddress);
            }
        }
    }
}