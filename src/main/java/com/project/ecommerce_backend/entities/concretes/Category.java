package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Ürünlerin hiyerarşik bir yapıda sınıflandırılmasını sağlayan ve özyinelemeli (recursive) ilişki desteğiyle sınırsız derinlikte kategori ağacı yönetimini mümkün kılan varlık sınıfıdır.
 * SQLRestriction özelliği sayesinde uygulama genelinde sadece aktif kategorilerin işlenmesini sağlar; indekslenmiş ebeveyn-çocuk ilişkileriyle kategori ağacı üzerinde hızlı arama ve listeleme optimizasyonu sunar.
 */
@Getter
@Setter
@Entity
@SQLRestriction("is_active = true")
@Table(name = "category", indexes = {
        @Index(name = "idx_category_parent_category_id", columnList = "parent_category_id")
}, uniqueConstraints = {
        @UniqueConstraint(name = "category_name_key", columnNames = {"name"})
})
public class Category extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "name", nullable = false)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_category_id")
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    private Category parentCategory;

    @OneToMany(mappedBy = "parentCategory", fetch = FetchType.LAZY)
    @JsonIgnore
    private Set<Category> categories = new LinkedHashSet<>();

    @OneToMany(mappedBy = "category")
    @JsonIgnore
    private Set<ProductCategory> productCategories = new LinkedHashSet<>();

    @Column(name = "description")
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = true;

}