package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.business.abstracts.BinLookupService;
import com.project.ecommerce_backend.entities.concretes.BinNumber;
import com.project.ecommerce_backend.repositories.abstracts.BinNumberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

/**
 * Ödeme süreçlerinde kart bilgilerinin doğrulanması ve sınıflandırılması için kullanılan, BIN (Bank Identification Number) sorgulama mekanizmalarını yöneten servistir.
 * Bu sınıf; kullanıcının girdiği kart numarasının ilk altı veya sekiz hanesine dayanarak kartın hangi bankaya, hangi şemaya ve hangi kart ailesine ait olduğunu tespit eden mantıksal sorguları yürütür ve bu yüksek frekanslı sorguları Redis üzerinde önbelleğe alarak ödeme adımındaki gecikmeleri (latency) minimize eder.
 */
@Service
@RequiredArgsConstructor
public class BinLookupManager implements BinLookupService {

    private final BinNumberRepository binNumberRepository;

    /**
     * Verilen BIN numarasıyla eşleşen en spesifik kart ailesi ismini tespit ederek ödeme sistemine geri bildirir.
     * İş akışı kapsamında, veritabanındaki en uzun eşleşen BIN numarası (Longest Matching Bin) algoritması kullanılarak kartın ait olduğu marka veya banka grubu belirlenir; bu işlem ödeme sayfasında taksit seçeneklerinin veya banka özelindeki kampanyaların dinamik olarak gösterilmesi için kritik bir veridir ve performans kaybını önlemek adına doğrudan BIN numarası üzerinden Redis'te önbelleğe alınır.
     *
     * @param binNumber Kart numarasının kart ailesini belirlemek için kullanılan başlangıç haneleridir.
     * @return Eşleşen bir kayıt bulunması durumunda kart ailesinin adını, aksi takdirde sistemin hata vermeden devam edebilmesi için "DEFAULT" değerini döner.
     */
    @Override
    @Cacheable(value = "binlookup", key = "#binNumber")
    public String getCardFamilyByBinNumber(String binNumber) {
        return binNumberRepository.findLongestMatchingBin(binNumber)
                .map(BinNumber::getCardFamilyName)
                .orElse("DEFAULT");
    }
}