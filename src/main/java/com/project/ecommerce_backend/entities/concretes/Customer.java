package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.SQLRestriction;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Platformdaki kullanıcıların kimlik, iletişim ve yetkilendirme bilgilerini yöneten merkezi varlık sınıfıdır.
 * E-posta ve telefon üzerinden tekillik kısıtları uygulayarak güvenli bir hesap yapısı sunar; roller (RBAC), siparişler, kayıtlı adresler ve ödeme yöntemleri ile kurduğu kapsamlı ilişkiler sayesinde müşteri yaşam döngüsünün tüm aşamalarını birbirine bağlar.
 */
@Getter
@Setter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@SQLRestriction("is_active = true")
@Table(name = "customer", 
       uniqueConstraints = {
           @UniqueConstraint(name = "customer_email_address_key", columnNames = {"email_address"}),
           @UniqueConstraint(name = "customer_phone_number_key", columnNames = {"phone_number"})
       },
       indexes = {
           @Index(name = "idx_customer_is_active", columnList = "is_active")
       })
@SuppressWarnings("squid:S1948")
public class Customer extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "email_address", nullable = false)
    private String emailAddress;

    @Size(max = 24)
    @NotNull
    @Column(name = "phone_number", nullable = false, length = 24)
    private String phoneNumber;

    @Size(max = 255)
    @NotNull
    @Column(name = "password_hash", nullable = false)
    private String passwordHash;

    @Size(max = 255)
    @Column(name = "company_name")
    private String companyName;

    @Size(max = 255)
    @Column(name = "contact_name")
    private String contactName;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "customer")
    @JsonIgnore
    private Set<CustomerAddress> customerAddresses = new LinkedHashSet<>();

    @OneToMany(mappedBy = "customer")
    @JsonIgnore
    private Set<CustomerReview> customerReviews = new LinkedHashSet<>();

    @OneToMany(mappedBy = "customer")
    @JsonIgnore
    private Set<Order> orders = new LinkedHashSet<>();

    @OneToMany(mappedBy = "customer")
    @JsonIgnore
    private Set<PaymentMethod> paymentMethods = new LinkedHashSet<>();

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "customer_role",
            joinColumns = @JoinColumn(name = "customer_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @JsonIgnore
    private Set<Role> roles = new LinkedHashSet<>();
}