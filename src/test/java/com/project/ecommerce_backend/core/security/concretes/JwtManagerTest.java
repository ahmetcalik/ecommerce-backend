package com.project.ecommerce_backend.core.security.concretes;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import javax.crypto.SecretKey;
import java.time.Duration;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class JwtManagerTest {

    @InjectMocks
    private JwtManager jwtManager;

    // Use a real, secure key for testing, matching the bit length required by the algorithm (HS256)
    private final String SECRET_KEY = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private final long EXPIRATION_MS = 3600000; // 1 hour
    private final long REFRESH_EXPIRATION_MS = 86400000; // 24 hours

    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        // Inject private fields using Spring's ReflectionTestUtils
        ReflectionTestUtils.setField(jwtManager, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtManager, "jwtExpiration", Duration.ofMillis(EXPIRATION_MS));
        ReflectionTestUtils.setField(jwtManager, "refreshExpiration", Duration.ofMillis(REFRESH_EXPIRATION_MS));

        List<GrantedAuthority> authorities = Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
        userDetails = new User("testuser@example.com", "password", authorities);
    }

    @Test
    void generateToken_shouldCreateValidTokenWithCorrectUsernameAndAuthorities() {
        String token = jwtManager.generateToken(userDetails);
        Claims claims = jwtManager.extractAllClaims(token);

        assertNotNull(token);
        assertEquals(userDetails.getUsername(), claims.getSubject());
        
        @SuppressWarnings("unchecked")
        List<String> authorities = claims.get("authorities", List.class);
        assertNotNull(authorities);
        assertEquals(1, authorities.size());
        assertEquals("ROLE_USER", authorities.get(0));
    }

    @Test
    void generateRefreshToken_shouldCreateValidTokenWithoutExtraClaims() {
        String token = jwtManager.generateRefreshToken(userDetails);
        Claims claims = jwtManager.extractAllClaims(token);

        assertNotNull(token);
        assertEquals(userDetails.getUsername(), claims.getSubject());
        assertNull(claims.get("authorities")); // Refresh token should not contain authorities
    }

    @Test
    void isTokenValid_shouldReturnTrue_forValidAndUnexpiredToken() {
        String token = jwtManager.generateToken(userDetails);
        assertTrue(jwtManager.isTokenValid(token, userDetails));
    }

    @Test
    void isTokenValid_shouldReturnFalse_whenUsernameDoesNotMatch() {
        String token = jwtManager.generateToken(userDetails);
        UserDetails otherUser = new User("anotheruser@example.com", "password", userDetails.getAuthorities());

        assertFalse(jwtManager.isTokenValid(token, otherUser));
    }

    @Test
    void isTokenValid_shouldThrowExpiredJwtException_forExpiredToken() {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(SECRET_KEY));
        String expiredToken = Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis() - 2000))
                .expiration(new Date(System.currentTimeMillis() - 1000)) // Expired 1 second ago
                .signWith(key)
                .compact();

        // isTokenValid calls extractUsername, which in turn calls extractAllClaims.
        // The parsing in extractAllClaims is what throws the exception.
        assertThrows(ExpiredJwtException.class, () -> jwtManager.isTokenValid(expiredToken, userDetails));
    }

    @Test
    void extractUsername_shouldReturnCorrectUsername() {
        String token = jwtManager.generateToken(userDetails);
        String username = jwtManager.extractUsername(token);
        assertEquals(userDetails.getUsername(), username);
    }

    @Test
    void extractExpiration_shouldReturnCorrectDate() {
        long now = System.currentTimeMillis();
        String token = jwtManager.generateToken(userDetails);
        Date expiration = jwtManager.extractExpiration(token);

        assertTrue(expiration.after(new Date(now)));
        assertTrue(expiration.before(new Date(now + EXPIRATION_MS + 1000))); // Within a reasonable range
    }

    @Test
    void extractAllClaims_shouldThrowException_forInvalidToken() {
        String invalidToken = "this.is.not.a.valid.token";

        assertThrows(MalformedJwtException.class, () -> {
            jwtManager.extractAllClaims(invalidToken);
        });
    }

    @Test
    void extractAllClaims_shouldThrowException_forTokenSignedWithWrongKey() {
        // Create a token with a different secret key
        SecretKey wrongKey = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);
        String tokenWithWrongKey = Jwts.builder()
                .subject(userDetails.getUsername())
                .signWith(wrongKey)
                .compact();

        assertThrows(SignatureException.class, () -> {
            jwtManager.extractAllClaims(tokenWithWrongKey);
        });
    }
}
