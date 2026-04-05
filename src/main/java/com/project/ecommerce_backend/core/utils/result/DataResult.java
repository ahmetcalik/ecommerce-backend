package com.project.ecommerce_backend.core.utils.result;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Objects;

/**
 * İşlem sonucunu ve talep edilen veriyi tek bir standart protokol altında birleştiren jenerik sonuç modelidir.
 * Başarı durumu ve açıklayıcı mesajın yanı sıra, "T" tipiyle sarmalanmış asıl veriyi taşıyarak sistem genelinde tip güvenli bir iletişim katmanı oluşturur. Eşitlik kontrolü (equals) ve karma kodu (hashCode) yetenekleri sayesinde, özellikle birim testlerde dönen yanıtların içerik bazlı doğrulanmasını kolaylaştırır.
 */
@Getter
@NoArgsConstructor
public class DataResult<T> extends Result {

    private T data;

    public DataResult(T data, boolean success) {
        super(success);
        this.data = data;
    }

    public DataResult( T data, String message, boolean success) {
        super(message, success);
        this.data = data;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataResult<?> that = (DataResult<?>) o;
        return success == that.success &&
                Objects.equals(getMessage(), that.getMessage()) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode() { //Equals override ettiğimiz için hasCode da override edildi.
        return Objects.hash(success, getMessage(), data);
    }
}
