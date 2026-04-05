package com.project.ecommerce_backend.core.exceptions.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.utils.result.ErrorResult;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.AllArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Uygulamanın güvenlik bariyerlerine takılan istekleri karşılayan ve API standartlarına uygun hata formatına dönüştüren merkezi güvenlik istisna işleyicisidir.
 * Bu sınıf; hem kimlik doğrulama eksikliklerini hem de yetkilendirme ihlallerini tek bir çatı altında yöneterek, Spring Security tarafından fırlatılan ham hatalar yerine istemciye tutarlı, anlaşılır ve çoklu dil destekli JSON yanıtları üretir.
 */
@Component
@AllArgsConstructor
public class SecurityExceptionHandler implements AuthenticationEntryPoint, AccessDeniedHandler {

    public final MessageService messageService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Kimlik doğrulama sürecinin başarısız olduğu veya eksik kaldığı durumlarda devreye giren güvenlik giriş noktasıdır.
     * Geçersiz token kullanımı, hatalı şifre girişi veya anonim erişim denemelerinde HTTP akışını keserek istemciye 401 Yetkisiz durum kodunu ve hatanın teknik detayını içermeyen güvenli bir açıklama metnini iletir.
     */
    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException authException) throws IOException, ServletException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ErrorResult errorResult = new ErrorResult(messageService.getMessageWithParams(
                Messages.Errors.AUTHENTICATION_FAILED,  authException.getMessage()));

        objectMapper.writeValue(response.getOutputStream(), errorResult);
    }

    /**
     * Kimliği doğrulanmış ancak erişmek istediği kaynak için yeterli yetkiye sahip olmayan kullanıcıları engelleyen erişim denetim mekanizmasıdır.
     * Rol tabanlı güvenlik ihlallerinde veya kısıtlı alanlara erişim girişimlerinde devreye girerek HTTP 403 Yasaklı durum kodunu üretir ve kullanıcıya mevcut yetki seviyesinin bu işlem için yetersiz olduğunu bildiren standart bir hata yanıtı döner.
     */
    @Override
    public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException accessDeniedException) throws IOException, ServletException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(HttpServletResponse.SC_FORBIDDEN);

        ErrorResult errorResult = new ErrorResult(messageService.getMessageWithParams(
                Messages.Errors.ACCESS_DENIED, accessDeniedException.getMessage()));

        objectMapper.writeValue(response.getOutputStream(), errorResult);
    }
}