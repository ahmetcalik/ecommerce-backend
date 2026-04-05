package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.InstallmentService;
import com.project.ecommerce_backend.business.dtos.responses.installment.InstallmentOptionResponse;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.entities.concretes.InstallmentOption;
import com.project.ecommerce_backend.repositories.abstracts.InstallmentOptionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

/**
 * Ödeme süreçlerinde kart ailelerine özel sunulan taksit seçeneklerinin hesaplanması ve yönetilmesinden sorumlu finansal iş mantığı servisidir.
 * Bu sınıf; dinamik faiz oranlarını baz alarak toplam geri ödeme ve aylık taksit tutarlarını hesaplayan bir motor görevi görür; hesaplama sonuçlarını ana tutar, kart ailesi ve taksit sayısı gibi değişkenleri harmanlayan eşsiz anahtarlar üzerinden Redis üzerinde önbelleğe alarak ödeme adımlarındaki yanıt sürelerini (response time) minimize eder.
 */
@Service
@RequiredArgsConstructor
public class InstallmentManager implements InstallmentService {

    private final InstallmentOptionRepository installmentOptionRepository;
    private final MessageService messageService;

    /**
     * Kullanıcının sepet tutarı ve ödeme yapacağı kartın markasına (Bonus, Maximum vb.) göre kullanabileceği tüm aktif taksit planlarını hesaplayarak sunar.
     * Sorgulama aşamasında hem karta özel tanımlanmış kampanyalar hem de genel (DEFAULT) taksit seçenekleri taranır; ardından her seçenek için faiz oranları dahil edilerek kullanıcıya sunulacak nihai rakamlar oluşturulur. Finansal tutarlılığı korumak adına bu veriler tutar ve kart ailesi bazında Redis üzerinde önbelleğe alınır.
     *
     * @param baseAmount Taksitlendirmeye tabi tutulacak ana sepet tutarıdır.
     * @param cardFamily Ödeme yapılacak kartın ait olduğu banka kart ailesi bilgisidir.
     * @return Hesaplanan taksit seçeneklerini (taksit sayısı, aylık ödeme, toplam tutar) içeren bir liste döner.
     */
    @Override
    @Cacheable(value = "installment", key = "#baseAmount + ':' + #cardFamily")
    public List<InstallmentOptionResponse> getAvailableOptions(BigDecimal baseAmount, String cardFamily) {
        List<InstallmentOption> options = installmentOptionRepository.findByCardFamilyNameInAndIsActiveTrue(
                List.of(cardFamily, "DEFAULT")
        );

        return options.stream()
                .map(option -> calculateOption(baseAmount, option))
                .toList();
    }

    /**
     * Ödeme onayı aşamasında kullanıcının seçtiği belirli bir taksit sayısına ait finansal detayları yeniden hesaplar ve doğrular.
     * Bu metot; özellikle ödeme emri bankaya gönderilmeden önce seçilen taksit planının hala aktif olup olmadığını denetlemek ve son toplam tutarı teyit etmek için kullanılır; geçersiz bir taksit sayısı talep edildiğinde sistem veri güvenliğini korumak adına bir iş kuralı istisnası (BusinessException) fırlatır.
     *
     * @param baseAmount İşleme esas olan ana tutar.
     * @param cardFamily Kullanılan kartın marka grubu.
     * @param installmentCount Doğrulanmak istenen taksit sayısı.
     * @return Seçilen taksit planının faiz dahil edilmiş son hesaplama detaylarını döner.
     * @throws BusinessException Talep edilen taksit sayısı ilgili kart ailesi için tanımlı veya aktif değilse fırlatılır.
     */
    @Override
    @Cacheable(value = "installment", key = "#baseAmount + ':' + #cardFamily + ':' + #installmentCount")
    public InstallmentOptionResponse getSpecificOption(BigDecimal baseAmount, String cardFamily, int installmentCount) {
        InstallmentOption option = installmentOptionRepository
                .findByCardFamilyNameInAndNumberOfInstallmentsAndIsActiveTrue(
                        List.of(cardFamily, "DEFAULT"), installmentCount)
                .orElseThrow(() -> new BusinessException(messageService.getMessage(
                        Messages.Installment.INSTALLMENT_OPTIONS_INVALID)));

        return calculateOption(baseAmount, option);
    }

    // --- YARDIMCI METOTLAR ---

    /**
     * Veritabanından gelen ham taksit tanımını, ana tutar ile birleştirerek matematiksel hesaplama sürecinden geçirir.
     * Toplam tutarı "Ana Tutar * (1 + Faiz Oranı)" formülüyle hesaplar ve aylık ödeme miktarını belirlemek için yarım yukarı yuvarlama (HALF_UP) stratejisini kullanarak virgülden sonra iki hane hassasiyetiyle finansal veriyi şekillendirir.
     */
    private InstallmentOptionResponse calculateOption(BigDecimal baseAmount, InstallmentOption option) {
        int installments = option.getNumberOfInstallments();
        BigDecimal interestRate = option.getInterestRate();
        BigDecimal totalAmount = baseAmount.multiply(BigDecimal.ONE.add(interestRate));
        BigDecimal monthlyPayment = totalAmount.divide(new BigDecimal(installments), 2, RoundingMode.HALF_UP);

        return new InstallmentOptionResponse(installments, monthlyPayment, totalAmount.setScale(2, RoundingMode.HALF_UP), interestRate);
    }


}