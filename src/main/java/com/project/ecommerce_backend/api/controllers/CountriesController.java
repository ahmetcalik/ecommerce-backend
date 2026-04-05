package com.project.ecommerce_backend.api.controllers;

import com.project.ecommerce_backend.business.abstracts.CountryService;
import com.project.ecommerce_backend.business.dtos.responses.country.CountryResponse;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Bu controller, sistemimizde desteklenen ülkelerin listesine erişim noktasıdır.
 * Genellikle kullanıcılarımızın adres tanımlama süreçlerinde, onlara doğru seçenekleri  sunmak ve veri tutarlılığını sağlamak için bu bilgileri kullanıyoruz.
 * Statik bir veri gibi görünse de, uluslararası gönderim ağımızın temelini oluşturur.
 */
@RestController
@RequestMapping("/api/v1/countries")
@RequiredArgsConstructor
@Validated
public class CountriesController {

    private final CountryService countryService;

    /**
     * Sistemde kayıtlı ve aktif olan tüm ülkelerin listesini döndürür.
     */
    @GetMapping
    public DataResult<List<CountryResponse>> getAll() {
        return this.countryService.getAll();
    }
}