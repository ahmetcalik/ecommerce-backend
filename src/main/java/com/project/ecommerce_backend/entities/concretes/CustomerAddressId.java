package com.project.ecommerce_backend.entities.concretes;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.hibernate.Hibernate;

import java.io.Serial;
import java.io.Serializable;
import java.util.Objects;

/**
 * Müşteri ve adres arasındaki ilişkiyi veritabanı seviyesinde tekilleştiren kompozit anahtar (Composite Key) sınıfıdır.
 * @Embeddable anotasyonu sayesinde CustomerAddress varlığı içerisinde bir kimlik bileşeni olarak kullanılır; müşteri ve adres ID değerlerinin kombinasyonunu benzersiz bir birincil anahtar olarak tanımlayarak veri bütünlüğünü sağlar.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class CustomerAddressId implements Serializable {

    @Serial
    private static final long serialVersionUID = 1741566363740249700L;

    @NotNull
    @Column(name = "customer_id", nullable = false)
    private Long customerId;

    @NotNull
    @Column(name = "address_id", nullable = false)
    private Long addressId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        CustomerAddressId entity = (CustomerAddressId) o;
        return Objects.equals(this.customerId, entity.customerId) &&
                Objects.equals(this.addressId, entity.addressId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(customerId, addressId);
    }

}