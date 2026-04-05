package com.project.ecommerce_backend.core.security.abstracts;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.Date;

public interface JwtService {

    String extractUsername(String token);

    Claims extractAllClaims(String token);

    String generateToken(UserDetails userDetails);

    String generateRefreshToken(UserDetails userDetails);

    boolean isTokenValid(String token, UserDetails userDetails);

    Date extractExpiration(String token);

}

