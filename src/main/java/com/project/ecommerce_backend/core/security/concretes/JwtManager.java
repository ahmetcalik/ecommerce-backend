package com.project.ecommerce_backend.core.security.concretes;

import com.project.ecommerce_backend.core.security.abstracts.JwtService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Duration;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Sistemin güvenli erişim anahtarlarını üreten ve doğrulayan kriptografik servis merkezidir.
 * Bu sınıf; kullanıcı kimlik bilgilerini ve yetkilerini barındıran Access Token'ların oluşturulması, daha uzun ömürlü olan ve oturum tazeleme imkanı sunan Refresh Token'ların yönetilmesi ve gelen her isteğin geçerliliğinin doğrulanması (parsing/verification) süreçlerinden sorumludur. Modern şifreleme algoritmaları (HMAC-SHA) kullanarak veri bütünlüğünü ve kullanıcı gizliliğini garanti altına alır.
 */
@Service
public class JwtManager implements JwtService {

    @Value("${application.security.jwt.secret-key}")
    private String secretKey;
    @Value("${application.security.jwt.expiration}")
    private Duration jwtExpiration;
    @Value("${application.security.jwt.refresh-token.expiration}")
    private Duration refreshExpiration;

    /**
     * Gelen bir JWT anahtarının içerisinden kullanıcının benzersiz kimliğini (username/email) ayrıştırarak geri döndürür.
     * Bu işlem, token içerisindeki "Subject" (konu) alanına odaklanarak, her istekte kullanıcının kim olduğunu veritabanına gitmeden hızlıca tespit etmemizi sağlar.
     *
     * @param token Çözümlenmek istenen JSON Web Token dizisidir.
     * @return Token içerisine gömülmüş olan kullanıcı adını döner.
     */
    @Override
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    /**
     * Elimizdeki token'ın hala güvenli ve kullanılabilir olup olmadığını titizlikle denetler.
     * Denetleme süreci iki aşamalıdır: İlk olarak token içerisindeki kullanıcı isminin sistemdeki kullanıcıyla eşleşip eşleşmediği kontrol edilir, ardından token'ın "Son Kullanma Tarihi" (Expiration) incelenerek süresinin geçip geçmediği doğrulanır.
     *
     * @param token Doğrulanacak olan aktif token.
     * @param userDetails Sistemde kayıtlı olan kullanıcının güvenlik detayları.
     * @return Eğer kullanıcı eşleşiyor ve token süresi dolmamışsa 'true' döner.
     */
    @Override
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    /**
     * Kullanıcının oturumunu uzun süre açık tutabilmesini sağlayan, düşük yetkili ama uzun ömürlü bir Refresh Token üretir.
     * Güvenlik prensipleri gereği bu token içerisinde roller (roles/authorities) gibi ekstra hassas bilgilere yer verilmez; sadece kullanıcının kimliğini ve oturum sürekliliğini temsil eden verileri barındırarak Access Token süresi dolduğunda yeni bir anahtar alabilmek için kullanılır.
     *
     * @param userDetails Token'ın adına üretileceği kullanıcı nesnesidir.
     * @return Yapılandırma dosyasındaki uzun süreli geçerliliğe sahip Refresh Token dizisini döner.
     */
    @Override
    public String generateRefreshToken(UserDetails userDetails) {
        return buildToken(new HashMap<>(), userDetails, refreshExpiration.toMillis());
    }

    /**
     * Kullanıcının sistem kaynaklarına erişebilmesi için gerekli olan, rollerle zenginleştirilmiş "Access Token" anahtarını üretir.
     * Kimlik doğrulama işlemi sırasında kullanıcının sahip olduğu tüm yetkiler (authorities) birer "Claim" (iddia) olarak token içerisine gömülür; bu sayede sistem, her istekte kullanıcının hangi yetkilere sahip olduğunu token'ı okuyarak anında anlar ve yüksek performanslı bir yetki kontrolü sağlar.
     *
     * @param userDetails Yetki bilgileri token içerisine eklenecek olan kullanıcıdır.
     * @return Kısa süreli geçerliliğe sahip, rollerle zenginleştirilmiş JWT anahtarını döner.
     */
    @Override
    public String generateToken(UserDetails userDetails) {
        Map<String, Object> extraClaims = new HashMap<>();
        extraClaims.put("authorities", userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList());
        return buildToken(extraClaims, userDetails, jwtExpiration.toMillis());
    }

    /**
     * Verilen bir token'ın son kullanma tarihini (Expiration Date) Claims yapısı içerisinden ayrıştırarak getirir.
     * Bu veri, oturumun sonlanıp sonlanmadığını kontrol etmek ve kullanıcının ne zaman sistemden otomatik olarak çıkarılacağını belirlemek için kullanılır.
     *
     * @param token İncelenecek token dizisi.
     * @return Token'ın geçerliliğini yitireceği tarih bilgisini döner.
     */
    @Override
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    /**
     * Gelen token'ın dijital imzasını sistemdeki gizli anahtar (Secret Key) ile doğrular ve içindeki tüm veri paketlerini (Claims) çözümler.
     * Bu metot, token'ın yolda değiştirilip değiştirilmediğini (Tampering) kontrol eden en kritik güvenlik noktasıdır; eğer imza uyuşmazsa veya token bozulmuşsa işlem güvenlik protokolleri gereği reddedilir.
     *
     * @param token Çözümlenecek ve doğrulanacak olan imzalı token.
     * @return Token içerisindeki kullanıcı adı, roller ve tarihler gibi tüm veri yükünü (Payload) döner.
     */
    @Override
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith((SecretKey) getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // --- YARDIMCI METOTLAR ---

    /**
     * Token üretme süreçlerindeki kod tekrarını önleyen merkezi bir inşa metodudur.
     * Verilen kullanıcı bilgilerini, ek yetkileri (claims) ve süre bilgisini Jwts kütüphanesi standartlarında birleştirerek, sistemin gizli anahtarıyla dijital olarak imzalanmış son hali oluşturur.
     */
    private String buildToken(Map<String, Object> extraClaims, UserDetails userDetails, long expiration) {
        return Jwts.builder()
                .claims(extraClaims)
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSignInKey())
                .compact();
    }

    /**
     * Token'ın geçerlilik süresinin dolup dolmadığını kontrol eden güvenlik filtresidir.
     * Token içerisindeki son kullanma tarihini (Expiration) mevcut sistem saatiyle karşılaştırarak, anahtarın hala yetkilendirme süreçlerinde kullanılıp kullanılamayacağını tespit eder.
     */
    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    /**
     * Token içerisindeki belirli bir veri parçasını (Claim), generic bir yapı kullanarak esnek bir şekilde ayrıştırır.
     * Bu metodun public olarak tasarlanma sebebi, sistemin gelecekteki ihtiyaçlarına karşı "genişletilebilir" (extensible) kalmasını sağlamaktır; böylece JwtService arayüzünde önceden tanımlanmamış olan özel verilere (Custom Claims), sınıfın iç yapısını değiştirmeden dış servislerden güvenli bir şekilde erişilebilir.
     *
     * @param <T> Çekilmek istenen verinin tipi.
     * @param token İşlem yapılacak JWT dizisi.
     * @param claimsResolver Veriyi Claims içerisinden nasıl ayıklayacağımızı belirleyen fonksiyonel mantık.
     * @return Belirtilen veri tipinde çözümlenmiş değeri döner.
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Sistem yapılandırmasında Base64 formatında saklanan ham gizli anahtarı, kriptografik işlemlerde (HMAC) kullanılabilecek güvenli bir anahtar nesnesine dönüştürür.
     */
    private Key getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

