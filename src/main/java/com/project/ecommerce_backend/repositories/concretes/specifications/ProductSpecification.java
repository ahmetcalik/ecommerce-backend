package com.project.ecommerce_backend.repositories.concretes.specifications;

import com.project.ecommerce_backend.entities.concretes.Product;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;

/**
 * Ürün listeleme ve arama işlemlerinde dinamik filtreleme koşullarını oluşturan spesifikasyon sınıfıdır.
 * JPA Criteria API kullanarak kategori, tedarikçi ve isim bazlı filtreleri çalışma zamanında birleştirilebilir bileşenler (Predicate) olarak üretir; böylece karmaşık ve opsiyonel filtreleme senaryolarının esnek bir şekilde yönetilmesini sağlar.
 */
@Component
public class ProductSpecification {

    private ProductSpecification() {}

    /**
     * Ürünün bağlı olduğu kategorilere göre filtreleme yapar.
     * Çok-çok ilişkiyi (Many-to-Many) join operasyonları ile çözümleyerek, ürünün belirtilen kategori ID'sine sahip olup olmadığını kontrol eder.
     */
    public static Specification<Product> hasCategory(Long categoryId) {
        return (Root<Product> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) -> {
            if (categoryId == null) {
                return criteriaBuilder.isTrue(criteriaBuilder.literal(true));
            }

            applyDistinctSafely(query);

            return criteriaBuilder.equal(root.join("productCategories").join("category").get("id"), categoryId);
        };
    }

    /**
     * Ürünü sağlayan tedarikçiye göre filtreleme gerçekleştirir.
     * Ürün ve tedarikçi arasındaki ilişki üzerinden, sadece belirli bir firmanın ürünlerini listelemek için kullanılır.
     */
    public static Specification<Product> hasSupplier(Long supplierId) {
        return (root, query, criteriaBuilder) -> {
            if (supplierId == null) {
                return criteriaBuilder.isTrue(criteriaBuilder.literal(true));
            }
            return criteriaBuilder.equal(root.get("supplier").get("id"), supplierId);
        };
    }

    /**
     * Ürün adı içerisinde metin tabanlı arama yapar.
     * Kullanıcıdan gelen arama terimini ve veritabanındaki ürün ismini küçük harfe (lower) çevirerek, büyük-küçük harf duyarsız ve kısmi eşleşmeli (LIKE) arama sonuçları üretir.
     */
    public static Specification<Product> nameContains(String nameSearch) {
        return (root, query, criteriaBuilder) -> {
            if (nameSearch == null || nameSearch.trim().isEmpty()) {
                return criteriaBuilder.isTrue(criteriaBuilder.literal(true));
            }
            return criteriaBuilder.like(
                    criteriaBuilder.lower(root.get("name")),
                    "%" + nameSearch.toLowerCase() + "%"
            );
        };
    }

    /**
     * Criteria sorgularında mükerrer kayıt oluşumunu engeller.
     * Özellikle join içeren sorgularda aynı ürünün birden fazla kez dönmesini 'distinct' anahtar kelimesiyle engellerken, Long dönen sayfalama (count) sorgularının çalışma yapısını bozmamak için tür kontrolü yapar.
     */
    private static void applyDistinctSafely(CriteriaQuery<?> query) {
        if (query != null) {
            Class<?> resultType = query.getResultType();
            if (resultType != null && resultType != Long.class && resultType != long.class) {
                query.distinct(true);
            }
        }
    }
}
