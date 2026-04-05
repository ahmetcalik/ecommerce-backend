package com.project.ecommerce_backend.core.security.concretes;

import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.security.abstracts.UserContextService;
import com.project.ecommerce_backend.core.security.details.CustomerDetails;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

/**
 * Uygulama genelindeki aktif kullanıcı bilgilerine güvenli ve merkezi bir erişim noktası sağlayan yönetim servisidir.
 * Spring Security bağlamında tutulan karmaşık kimlik doğrulama nesnelerini işleyerek, sistemin diğer katmanlarının ihtiyaç duyduğu ham kullanıcı verilerini sunar. Güvenlik kütüphanesi ile iş mantığı katmanları arasında bir köprü görevi görerek bağımlılıkları minimize eder.
 */
@Service
@RequiredArgsConstructor
public class UserContextManager implements UserContextService {

    private final MessageService messageService;

    /**
     * O anki oturumda işlem yapan kullanıcının sistemdeki benzersiz kimlik numarasını tespit eden metodur.
     * Güvenlik bağlamından asıl kullanıcı nesnesini ayrıştırır ve beklenen formatta olup olmadığını denetler; kimlik bilgisi doğrulanamazsa veya geçersiz bir oturum söz konusuysa iş kuralı istisnası fırlatarak işlemin güvenli bir şekilde durdurulmasını sağlar.
     */
    @Override
    public Long getAuthenticatedUserId() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        if (principal instanceof CustomerDetails) {
            return ((CustomerDetails) principal).getId();
        }

        throw new BusinessException(messageService.getMessage(
                Messages.Auth.AUTHENTICATION_PRINCIPAL_INVALID));
    }
}