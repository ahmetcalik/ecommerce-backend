package com.project.ecommerce_backend.core.configurations;

import com.project.ecommerce_backend.core.exceptions.security.SecurityExceptionHandler;
import com.project.ecommerce_backend.core.configurations.security.filters.JwtAuthenticationFilter;
import com.project.ecommerce_backend.core.configurations.security.handlers.CustomLogoutSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Uygulamanın siber güvenlik mimarisini inşa eden ve tüm HTTP trafiğini denetleyen merkezi yapılandırma sınıfıdır.
 * Spring Security altyapısını etkinleştirerek hem ağ seviyesindeki uç nokta korumalarını hem de metot seviyesindeki hassas yetkilendirme kurallarını devreye alır. Durumsuz mimari prensiplerine sadık kalarak, her isteğin bağımsız olarak doğrulandığı güvenli bir API ortamı sağlar.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final UserDetailsService userDetailsService;
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final SecurityExceptionHandler securityExceptionHandler;
    private final CustomLogoutSuccessHandler customLogoutSuccessHandler;

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_SELLER = "SELLER";
    private static final String ROLE_USER = "USER";

    private static final String PRODUCTS_API_ENDPOINT = "/api/v1/products/**";
    private static final String CATEGORIES_API_ENDPOINT = "/api/v1/categories/**";

    /**
     * Kimlik doğrulama filtrelerinden muaf tutularak misafir kullanıcıların erişimine açık bırakılan uç noktaları tanımlayan güvenli erişim listesidir.
     * API dokümantasyonu ve kayıt olma ekranları gibi herkesin ulaşması gereken kaynakları kapsar.
     */
    private static final String[] WHITE_LIST_URLS = {
            "/swagger-ui.html",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/auth/refresh-token"
    };

    /**
     * Gelen HTTP isteklerinin tabi tutulacağı güvenlik protokollerini ve erişim kısıtlamalarını yöneten ana filtre zincirini oluşturur.
     * CSRF korumasını devre dışı bırakarak durumsuz oturum yönetimi stratejisini benimser ve özel JWT doğrulama filtresini standart kimlik denetiminden önce devreye alarak modern ve güvenli bir akış sağlar. Ayrıca rol tabanlı erişim denetimlerini ve istisna yönetimi mekanizmalarını yapılandırarak yetkisiz erişim girişimlerini engeller.
     *
     * @param http Güvenlik kurallarının tanımlandığı yapılandırma nesnesidir.
     * @return Uygulanacak güvenlik politikalarını içeren filtre zincirini döner.
     * @throws Exception Yapılandırma sürecinde oluşabilecek hataları fırlatır.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(WHITE_LIST_URLS).permitAll()
                        .requestMatchers(HttpMethod.GET, PRODUCTS_API_ENDPOINT, CATEGORIES_API_ENDPOINT).permitAll()
                        .requestMatchers("/api/v1/cart/**").permitAll()
                        .requestMatchers("/api/v1/addresses/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .requestMatchers("/api/v1/countries/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .requestMatchers("/api/v1/orders/**").hasAnyRole(ROLE_USER, ROLE_SELLER, ROLE_ADMIN)
                        .requestMatchers("/api/v1/installments/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .requestMatchers("/api/v1/payment-methods/**").hasAnyRole(ROLE_USER, ROLE_ADMIN)
                        .requestMatchers("/api/v1/returns/**").hasRole(ROLE_USER)
                        .requestMatchers("/api/v1/admin/orders/**").hasAnyRole(ROLE_SELLER, ROLE_ADMIN)
                        .requestMatchers("/api/v1/admin/returns/**").hasAnyRole(ROLE_SELLER, ROLE_ADMIN)
                        .requestMatchers("/api/v1/admin/**").hasRole(ROLE_ADMIN)
                        .requestMatchers(HttpMethod.POST, PRODUCTS_API_ENDPOINT, CATEGORIES_API_ENDPOINT).hasAnyRole(ROLE_SELLER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.PUT, PRODUCTS_API_ENDPOINT, CATEGORIES_API_ENDPOINT).hasAnyRole(ROLE_SELLER, ROLE_ADMIN)
                        .requestMatchers(HttpMethod.DELETE, PRODUCTS_API_ENDPOINT, CATEGORIES_API_ENDPOINT).hasAnyRole(ROLE_SELLER, ROLE_ADMIN)
                        .anyRequest().authenticated()
                )
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authenticationProvider(authenticationProvider())
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(securityExceptionHandler)
                        .accessDeniedHandler(securityExceptionHandler)
                )
                .logout(logout -> logout
                        .logoutUrl("/api/v1/auth/logout")
                        .logoutSuccessHandler(customLogoutSuccessHandler)
                        .clearAuthentication(true)
                );

        return http.build();
    }

    /**
     * Kullanıcı parolalarının veritabanında saklanmadan önce güvenli bir şekilde karmaşıklaştırılmasını sağlayan şifreleme bileşenidir.
     * Tek yönlü şifreleme algoritması kullanarak parolaların ham haliyle saklanmasını önler ve veri ihlali durumlarında hesap güvenliğini korur.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Kimlik doğrulama sürecinde kullanıcı verilerinin veritabanından çekilmesi ve parola doğruluğunun teyit edilmesi işlemlerini yürüten veri erişim sağlayıcısını yapılandırır.
     * Kullanıcı detay servisi ve şifreleme algoritması arasındaki entegrasyonu sağlayarak güvenli giriş işleminin temelini oluşturur.
     */
    @Bean
    public AuthenticationProvider authenticationProvider() {
        var authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Uygulama genelindeki kimlik doğrulama taleplerini işleyen ve yöneten merkezi yetkilendirme yöneticisini sistem bağlamına dahil eder.
     * Farklı kimlik doğrulama sağlayıcılarını koordine ederek giriş işlemlerinin başarılı bir şekilde sonuçlandırılmasını sağlar.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}