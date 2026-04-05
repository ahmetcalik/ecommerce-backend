package com.project.ecommerce_backend.core.configurations;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.hibernate6.Hibernate6Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.jackson2.SimpleGrantedAuthorityMixin;

/**
 * Uygulama genelindeki veri önbellekleme stratejilerini ve serileştirme kurallarını yöneten merkezi yapılandırma sınıfıdır.
 * Bu sınıf; Java nesnelerinin Redis belleğine yazılırken JSON formatına dönüştürülmesi sürecini özelleştirir. Özellikle Hibernate varlıkları, tarih formatları ve jenerik veri tipleri gibi karmaşık yapıların veri kaybı olmadan saklanmasını ve tekrar okunabilmesini garanti altına alır.
 */
@Configuration
public class RedisCacheConfiguration {

    @Value("${spring.cache.redis.key-prefix:}")
    private String keyPrefix;

    @Bean
    public RedisCacheManagerBuilderCustomizer redisCacheManagerBuilderCustomizer() {
        return (builder) -> {
            ObjectMapper objectMapper = new ObjectMapper()
                    .registerModule(new JavaTimeModule())
                    .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

            Hibernate6Module hibernateModule = new Hibernate6Module();
            hibernateModule.disable(Hibernate6Module.Feature.FORCE_LAZY_LOADING);
            objectMapper.registerModule(hibernateModule);
            objectMapper.addMixIn(SimpleGrantedAuthority.class, SimpleGrantedAuthorityMixin.class);
            objectMapper.activateDefaultTyping(
                    LaissezFaireSubTypeValidator.instance,
                    ObjectMapper.DefaultTyping.NON_FINAL,
                    JsonTypeInfo.As.PROPERTY
            );

            GenericJackson2JsonRedisSerializer jsonRedisSerializer =
                    new GenericJackson2JsonRedisSerializer(objectMapper);

            org.springframework.data.redis.cache.RedisCacheConfiguration config =
                    org.springframework.data.redis.cache.RedisCacheConfiguration.defaultCacheConfig()
                            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonRedisSerializer));

            if (keyPrefix != null && !keyPrefix.isEmpty()) {
                config = config.prefixCacheNameWith(keyPrefix);
            }

            builder.cacheDefaults(config);
        };
    }
}