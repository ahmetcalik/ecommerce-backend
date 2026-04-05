package com.project.ecommerce_backend.core.exceptions.types;

/**
 * Oturum yenileme sürecinde kullanılan güvenlik anahtarının geçersizliğini veya zaman aşımını temsil eden özel istisna sınıfıdır.
 * Erişim anahtarının süresi dolduğunda devreye giren yenileme mekanizması başarısız olduğunda fırlatılır; bu durum genellikle oturumun tamamen sonlandığını ve kullanıcının güvenli bir şekilde tekrar giriş ekranına yönlendirilmesi gerektiğini işaret eder.
 */
public class InvalidRefreshTokenException extends RuntimeException {
    public InvalidRefreshTokenException(String message) {
        super(message);
    }
}
