package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Platformdaki ürünlerin kaynağı olan ticari kuruluşların bilgilerini ve operasyonel durumlarını yöneten varlık sınıfıdır.
 * Şirket bilgilerini, iletişim kanallarını ve fiziksel adres verilerini saklayarak; ürünlerin tedarikçi bazlı gruplandırılmasına, sipariş sonrası tedarik süreçlerinin yürütülmesine ve müşteri yorumlarına verilen kurumsal yanıtların takibine olanak tanır.
 */
@Getter
@Setter
@Entity
@Table(name = "supplier", indexes = {
        @Index(name = "idx_supplier_address_id", columnList = "address_id")
})
public class Supplier extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Size(max = 255)
    @Column(name = "contact_name")
    private String contactName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "address_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Address address;

    @Size(max = 24)
    @Column(name = "phone_number", length = 24)
    private String phoneNumber;

    @Size(max = 255)
    @Column(name = "email")
    private String email;

    @NotNull
    @ColumnDefault("true")
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @OneToMany(mappedBy = "supplier")
    @JsonIgnore
    private Set<Product> products = new LinkedHashSet<>();

    @OneToMany(mappedBy = "supplier")
    @JsonIgnore
    private Set<SupplierResponse> supplierResponses = new LinkedHashSet<>();
}