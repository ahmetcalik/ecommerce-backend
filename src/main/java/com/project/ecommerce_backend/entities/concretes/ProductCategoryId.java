package com.project.ecommerce_backend.entities.concretes;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.Hibernate;

import java.io.Serializable;
import java.util.Objects;

/**
 * Ürün ve kategori arasındaki çoka-çok ilişkiyi veritabanı seviyesinde tekilleştiren kompozit anahtar sınıfıdır.
 * Ürün ve kategori kimlik numaralarını birleştirerek benzersiz bir anahtar oluşturur; JPA katmanında bu ilişkinin kimlik yönetimini üstlenerek veri bütünlüğünü sağlar ve mükerrer kategori atamalarını engeller.
 */
@Getter
@Setter
@Embeddable
@AllArgsConstructor
@NoArgsConstructor
public class ProductCategoryId implements Serializable {

    private static final long serialVersionUID = 6662647419921397072L;

    @NotNull
    @Column(name = "product_id", nullable = false)
    private Long productId;

    @NotNull
    @Column(name = "category_id", nullable = false)
    private Long categoryId;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || Hibernate.getClass(this) != Hibernate.getClass(o)) return false;
        ProductCategoryId entity = (ProductCategoryId) o;
        return Objects.equals(this.productId, entity.productId) &&
                Objects.equals(this.categoryId, entity.categoryId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(productId, categoryId);
    }

}