package com.project.ecommerce_backend.core.configurations.security.filters;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.ecommerce_backend.core.security.abstracts.JwtService;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.utils.result.ErrorResult;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Uygulama genelindeki her bir HTTP isteğini yakalayarak JWT tabanlı kimlik doğrulama sürecini yöneten güvenlik filtresidir.
 * Bu sınıf; isteğin başlığındaki yetkilendirme bilgisini ayrıştırır, token geçerliliğini ve süresini kontrol eder ve başarılı doğrulama sonucunda kullanıcıyı Spring Security bağlamına dahil eder. Kimlik doğrulama gerektirmeyen uç noktaları muaf tutarak sistemin erişim güvenliğini merkezi bir noktadan koordine eder.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final MessageService messageService;

    /**
     * Gelen her isteğin kimlik doğrulama katmanından geçmesini sağlayan ana filtreleme mekanizmasıdır.
     * Metot; isteğin yolunu kontrol ederek doğrulama gerekmeyen alanları atlar, ardından yetkilendirme başlığındaki JWT verisini çıkartarak kullanıcı bilgisini analiz eder. Eğer kullanıcı daha önce doğrulanmamışsa ve token geçerliyse, kullanıcıyı sistemin aktif oturumuna dahil ederek isteğin bir sonraki denetleyiciye güvenle geçmesini sağlar. Ayrıca token süresinin dolması veya imza uyuşmazlığı gibi hataları özel olarak yakalayarak istemciye standart bir JSON hata mesajı üretir.
     */
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {

        if (request.getServletPath().contains("/api/v1/auth")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String authHeader = request.getHeader("Authorization");

        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        final String jwt = authHeader.substring(7);
        String username = null;

        try {
            username = jwtService.extractUsername(jwt);
        } catch (ExpiredJwtException e) {
            handleException(response, HttpStatus.UNAUTHORIZED, messageService.getMessage(
                    Messages.Auth.TOKEN_EXPIRED));
            return;
        } catch (JwtException e) {
            handleException(response, HttpStatus.UNAUTHORIZED, messageService.getMessage(
                    Messages.Auth.TOKEN_INVALID));
            return;
        }

        if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            UserDetails userDetails = this.userDetailsService.loadUserByUsername(username);

            if (jwtService.isTokenValid(jwt, userDetails)) {
                UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                        userDetails,
                        null,
                        userDetails.getAuthorities()
                );
                authToken.setDetails(
                        new WebAuthenticationDetailsSource().buildDetails(request)
                );
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }
        filterChain.doFilter(request, response);
    }

    /**
     * Filtreleme sürecinde yakalanan istisnai durumları son kullanıcıya anlamlı ve standart bir formatta ileten yanıt oluşturucudur.
     * Token süresinin dolması veya geçersiz olması gibi kritik güvenlik hatalarında, HTTP akışını keserek istemciye uluslararasılaştırma destekli ve JSON formatında bir hata raporu yazar.
     */
    private void handleException(HttpServletResponse response, HttpStatus status, String messageKey) throws IOException {
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setStatus(status.value());
        ErrorResult errorResult = new ErrorResult(messageService.getMessage(messageKey));
        new ObjectMapper().writeValue(response.getOutputStream(), errorResult);
    }
}

