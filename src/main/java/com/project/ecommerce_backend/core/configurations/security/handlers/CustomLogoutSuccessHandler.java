package com.project.ecommerce_backend.core.configurations.security.handlers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.utils.result.SuccessResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Spring Security tarafından yönetilen oturum sonlandırma sürecini özelleştirerek varsayılan sayfa yönlendirmesi yerine modern API standartlarına uygun JSON tabanlı bir yanıt mekanizması sunar.
 * Bu sınıf; durumsuz mimarilerde istemci uygulamaların çıkış işlemini programatik olarak algılamasını ve kullanıcı arayüzünü buna göre güncellemesini sağlayan sonlandırma mantığını yönetir.
 */
@Component
@RequiredArgsConstructor
public class CustomLogoutSuccessHandler implements LogoutSuccessHandler {

    private final MessageService messageService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Oturum kapatma işlemi güvenlik filtreleri tarafından başarıyla tamamlandığında tetiklenen ve istemciye işlemin sonucunu ileten olay işleyicisidir.
     * Yanıt içeriğini JSON formatında yapılandırarak HTTP 200 durum kodu ve çoklu dil desteğine sahip bir başarı mesajı ile birlikte döner; böylece ön yüz uygulamaları oturumun sonlandığını doğrulayarak yerel depolama temizliği gibi gerekli işlemleri başlatabilir.
     */
    @Override
    public void onLogoutSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {

        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_OK);

        SuccessResult successResult = new SuccessResult(messageService.getMessage(
                Messages.Auth.LOGOUT_SUCCESSFUL));

        objectMapper.writeValue(response.getOutputStream(), successResult);
    }
}
