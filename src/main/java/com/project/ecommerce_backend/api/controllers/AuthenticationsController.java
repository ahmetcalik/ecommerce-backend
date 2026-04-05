package com.project.ecommerce_backend.api.controllers;

import com.project.ecommerce_backend.business.abstracts.CustomerService;
import com.project.ecommerce_backend.core.security.abstracts.JwtService;
import com.project.ecommerce_backend.business.abstracts.ShoppingCartService;
import com.project.ecommerce_backend.business.dtos.requests.auth.LoginRequest;
import com.project.ecommerce_backend.business.dtos.requests.auth.RefreshTokenRequest;
import com.project.ecommerce_backend.business.dtos.requests.auth.RegisterRequest;
import com.project.ecommerce_backend.business.dtos.responses.auth.AuthenticationResponse;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.InvalidRefreshTokenException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.security.details.CustomerDetails;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.SuccessDataResult;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.UUID;

/**
 * Bu controller, sistemimize giriş yapmak isteyen herkesin uğraması gereken ilk duraktır.
 * Kullanıcıların kayıt olma, giriş yapma ve oturumlarını yenileme gibi temel güvenlik işlemlerini burada yönetiyoruz.
 * Sadece bir kimlik doğrulama merkezi değil, aynı zamanda misafir kullanıcıların sepetlerini gerçek hesaplarıyla senkronize ettiğimiz bir köprü görevi görür.
 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Validated
public class AuthenticationsController {

    private final CustomerService customerService;
    private final ShoppingCartService shoppingCartService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final JwtService jwtService;
    private final MessageService messageService;

    private static final String SESSION_ID_COOKIE = "SESSION-ID";

    /**
     * Aramıza yeni katılacak kullanıcıları bu kapıdan içeri alıyoruz.
     * Kullanıcı bilgilerini kaydedip, onlara hemen kullanabilecekleri bir oturum anahtarı teslim ediyoruz.
     */
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public DataResult<AuthenticationResponse> register(
            @Valid @RequestBody RegisterRequest request) {
        return this.customerService.register(request);
    }

    /**
     * Kullanıcının kimliğini doğrulayıp sisteme dahil ediyoruz.
     * Bu aşamada eğer kullanıcı daha önce giriş yapmadan sepetine ürün eklediyse, o ürünleri kaybetmemesi için sepetini kalıcı hesabıyla birleştiriyor ve misafir çerezini temizliyoruz.
     */
    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    public DataResult<AuthenticationResponse> login(
            @Valid @RequestBody LoginRequest request,
            @CookieValue(name = SESSION_ID_COOKIE, required = false) UUID sessionId,
            HttpServletResponse response
    ) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        UserDetails userDetails = userDetailsService.loadUserByUsername(request.getEmail());

        if (sessionId != null && userDetails instanceof CustomerDetails customerDetails) {
            shoppingCartService.mergeCarts(sessionId, customerDetails.getId());

            Cookie cookie = new Cookie(SESSION_ID_COOKIE, null);
            cookie.setPath("/");
            cookie.setMaxAge(0);
            response.addCookie(cookie);
        }

        String accessToken = jwtService.generateToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        Date expirationDate = jwtService.extractExpiration(accessToken);

        AuthenticationResponse authResponse = AuthenticationResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .issuedAt(new Date())
                .accessTokenExpiresAt(expirationDate)
                .build();

        return new SuccessDataResult<>(authResponse, messageService.getMessage(
                Messages.Auth.LOGIN_SUCCESSFUL));
    }

    /**
     * Access token süresi dolduğunda, kullanıcının tekrar şifre girmesine gerek kalmadan oturumunu tazelemek için bu metodu kullanıyoruz.
     * Güvenlik için refresh token'ın geçerliliğini titizlikle kontrol ediyoruz.
     */
    @PostMapping("/refresh-token")
    @ResponseStatus(HttpStatus.OK)
    public DataResult<AuthenticationResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request) {
        String username = jwtService.extractUsername(request.getRefreshToken());
        UserDetails userDetails = userDetailsService.loadUserByUsername(username);

        if (!jwtService.isTokenValid(request.getRefreshToken(), userDetails)) {
            throw new InvalidRefreshTokenException(messageService.getMessage(Messages.Auth.INVALID_REFRESH_TOKEN));
        }

        String newAccessToken = jwtService.generateToken(userDetails);
        Date newExpirationDate = jwtService.extractExpiration(newAccessToken);

        AuthenticationResponse authResponse = AuthenticationResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(request.getRefreshToken())
                .issuedAt(new Date())
                .accessTokenExpiresAt(newExpirationDate)
                .build();

        return new SuccessDataResult<>(authResponse, messageService.getMessage(
                Messages.Auth.TOKEN_SUCCESSFULLY_REFRESHED));
    }
}