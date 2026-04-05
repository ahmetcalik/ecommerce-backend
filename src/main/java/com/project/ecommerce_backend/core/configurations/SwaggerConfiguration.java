package com.project.ecommerce_backend.core.configurations;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

/**
 * Uygulamanın RESTful servislerini uluslararası OpenAPI standartlarına uygun şekilde belgeleyen ve geliştiricilere interaktif bir keşif ortamı sunan merkezi yapılandırma sınıfıdır.
 * Bu sınıf; API dokümantasyonunun genel kimlik bilgilerini tanımlamanın yanı sıra, test süreçlerinde kullanılacak güvenlik şemalarını ve çoklu dil desteği gibi küresel başlık parametrelerini sisteme entegre eder.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "E-Commerce API",
                version = "1.0",
                description = "Bu, E-Ticaret projesinin API dokümantasyonudur."
        ),
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name = "bearerAuth",
        type = SecuritySchemeType.HTTP,
        scheme = "bearer",
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER,
        description = "JWT Authorization başlığı Bearer şemasını kullanır. Örnek: \"Authorization: Bearer {token}\""
)
public class SwaggerConfiguration {

    /**
     * Swagger arayüzündeki tüm API operasyonlarına uluslararasılaştırma desteği için gerekli olan dil seçim parametresini otomatik olarak ekleyen operasyon özelleştiricisidir.
     * Geliştiricilerin test süreçlerinde farklı dil seçeneklerini kolayca simüle edebilmelerini sağlayarak çoklu dil altyapısının görünürlüğünü ve kullanılabilirliğini artırır.
     *
     * @return Tüm endpoint tanımlarına başlık parametresi ekleyen özelleştirme nesnesini döner.
     */
    @Bean
    public OperationCustomizer customGlobalHeaders(){
        return (Operation operation, HandlerMethod handlerMethod) -> {
            Parameter headerParameter = new Parameter()
                    .in(ParameterIn.HEADER.toString())
                    .schema(new StringSchema())
                    .name("Accept-Language")
                    .description("Bu alan multi-dil desteği getirilmesi adına kullanılmaktadır. (en, tr). Opsiyoneldir.")
                    .required(false);

            operation.addParametersItem(headerParameter);

            return operation;
        };
    }
}

