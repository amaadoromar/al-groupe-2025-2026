package org.eSante.identity.security;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.time.Instant;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {
    private final Key key;
    private final String issuer;
    private final long ttlSeconds;

    public JwtService(
            @Value("${identity.jwt.secret}") String secret,
            @Value("${identity.jwt.issuer}") String issuer,
            @Value("${identity.jwt.ttl-seconds}") long ttlSeconds
    ) {
        // Allow raw string or base64 secret
        byte[] secretBytes;
        try {
            secretBytes = Decoders.BASE64.decode(secret);
        } catch (Exception e) {
            secretBytes = secret.getBytes();
        }
        this.key = Keys.hmacShaKeyFor(secretBytes.length >= 32 ? secretBytes : pad(secretBytes));
        this.issuer = issuer;
        this.ttlSeconds = ttlSeconds;
    }

    private byte[] pad(byte[] in) {
        byte[] out = new byte[32];
        for (int i = 0; i < out.length; i++) out[i] = (i < in.length ? in[i] : (byte) (i * 31));
        return out;
    }

    public String create(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setSubject(subject)
                .addClaims(claims)
                .setIssuer(issuer)
                .setIssuedAt(Date.from(now))
                .setExpiration(Date.from(now.plusSeconds(ttlSeconds)))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }
}

