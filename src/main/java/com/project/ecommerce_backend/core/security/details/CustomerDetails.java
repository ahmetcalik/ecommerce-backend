package com.project.ecommerce_backend.core.security.details;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.ecommerce_backend.entities.concretes.Customer;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * Spring Security çerçevesinin kimlik doğrulama ve yetkilendirme süreçlerinde kullandığı kullanıcı bilgilerini sarmalayan temsilci sınıfıdır.
 * Veritabanındaki müşteri varlığını güvenlik katmanının anlayabileceği standart yapıya dönüştürür. Dağıtık önbellek sistemleriyle tam uyumlu çalışabilmesi için JSON serileştirme yetenekleriyle donatılmış olup, kullanıcının sistemdeki kimlik bilgilerini ve yetki seviyelerini güvenli bir şekilde taşır.
 */
@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CustomerDetails implements UserDetails {

    private Long id;
    private String email;
    private String password;
    private Collection<? extends GrantedAuthority> authorities;

    /**
     * Dağıtık önbellek sistemlerinden veri okunurken nesnenin doğru şekilde yeniden yapılandırılmasını sağlayan serileştirme yapıcısıdır.
     * JSON verisindeki alanları sınıf özellikleriyle eşleştirerek, tip güvenliğini korur ve yetki bilgilerinin serileştirme süreçlerinde kaybolmasını önler.
     */
    @JsonCreator
    public CustomerDetails(
            @JsonProperty("id") Long id,
            @JsonProperty("email") String email,
            @JsonProperty("password") String password,
            @JsonProperty("authorities") Collection<SimpleGrantedAuthority> authorities) {

        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
    }

    /**
     * Alan modelindeki müşteri varlığını güvenlik detaylarına dönüştüren uygulama içi yapılandırıcıdır.
     * Veritabanından gelen kullanıcı rollerini sistem yetkilerine eşleyerek güvenlik filtrelerinin kullanabileceği bir yetki kümesi oluşturur.
     */
    public CustomerDetails(Customer customer) {
        this.id = customer.getId();
        this.email = customer.getEmailAddress();
        this.password = customer.getPasswordHash();
        this.authorities = customer.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .collect(Collectors.toList());
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}