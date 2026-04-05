package com.project.ecommerce_backend.entities.concretes;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.project.ecommerce_backend.entities.abstracts.BaseEntity;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Uygulamanın coğrafi kapsamını belirleyen ve adres bilgilerinin en üst seviyede standartlaştırılmasını sağlayan varlık sınıfıdır.
 * Fiziksel adreslerle (Address) kurduğu bire-çok ilişki sayesinde, sipariş sevkiyatlarından vergi hesaplamalarına kadar ülkeye özgü iş kurallarının doğru lokasyonlarla eşleştirilmesini sağlar.
 */
@Getter
@Setter
@Entity
@Table(name = "country", uniqueConstraints = {
        @UniqueConstraint(name = "country_country_name_key", columnNames = {"country_name"})
})
public class Country extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 255)
    @NotNull
    @Column(name = "country_name", nullable = false)
    private String countryName;

    @OneToMany(mappedBy = "country")
    @JsonIgnore
    private Set<Address> addresses = new LinkedHashSet<>();

}