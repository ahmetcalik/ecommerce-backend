package com.project.ecommerce_backend.entities.abstracts;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.OffsetDateTime;

/**
 * Veritabanı varlıkları için gelişmiş denetim (auditing) ve veri bütünlüğü protokollerini tanımlayan temel katmandır.
 * Her bir kaydın kim tarafından, ne zaman oluşturulduğunu ve güncellendiğini otomatik olarak izleyen denetim mekanizmasını devreye sokar. Ayrıca iyimser kilitleme (Optimistic Locking) stratejisiyle aynı veri üzerinde eşzamanlı yapılan çakışan güncellemeleri engelleyerek veri tutarlılığını en üst seviyede garanti altına alır.
 */
@MappedSuperclass
@Setter
@Getter
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@JsonIgnoreProperties(value = {"cdate", "udate"}, allowGetters = true)
public abstract class BaseEntity implements Serializable {

    @CreatedDate
    @Column(name = "cdate", updatable = false)
    private OffsetDateTime cDate;

    @CreatedBy
    @Column(name = "cuser", updatable = false)
    private Long cUser;

    @LastModifiedDate
    @Column(name = "udate")
    private OffsetDateTime uDate;

    @LastModifiedBy
    @Column(name = "uuser")
    private Long uUser;

    @Version
    private Long version;
}
