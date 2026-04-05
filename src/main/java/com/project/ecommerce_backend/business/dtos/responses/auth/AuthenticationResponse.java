package com.project.ecommerce_backend.business.dtos.responses.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.Date;

/**
 * Kimlik doğrulama (login/register) işlemleri sonucunda frontend'e döndürülecek
 * olan veri yapısını temsil eder.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AuthenticationResponse {
    /**
     * Kısa ömürlü Erişim Token'ı (Access Token). Ana API isteklerinde kullanılır.
     */
    private String accessToken;

    /**
     * Uzun ömürlü Yenileme Token'ı (Refresh Token). Yeni bir erişim token'ı almak için kullanılır.
     */
    private String refreshToken;

    /**
     * Erişim token'ının ne zaman oluşturulduğu.
     */
    private Date issuedAt;

    /**
     * Erişim token'ının ne zaman sona ereceği.
     */
    private Date accessTokenExpiresAt;
}
