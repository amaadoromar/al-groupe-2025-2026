package org.eSante.identity.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.Key;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final Key key;

    public JwtAuthFilter(@Value("${identity.jwt.secret}") String secret) {
        byte[] secretBytes;
        try { secretBytes = Decoders.BASE64.decode(secret); } catch (Exception e) { secretBytes = secret.getBytes(); }
        this.key = Keys.hmacShaKeyFor(secretBytes.length >= 32 ? secretBytes : pad(secretBytes));
    }

    private byte[] pad(byte[] in) {
        byte[] out = new byte[32];
        for (int i = 0; i < out.length; i++) out[i] = (i < in.length ? in[i] : (byte) (i * 31));
        return out;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
                String subject = claims.getSubject();
                @SuppressWarnings("unchecked")
                List<String> roles = (List<String>) claims.getOrDefault("roles", List.of());
                Collection<SimpleGrantedAuthority> auths = new ArrayList<>();
                for (String r : roles) auths.add(new SimpleGrantedAuthority("ROLE_" + r));
                var authentication = new UsernamePasswordAuthenticationToken(subject, null, auths);
                authentication.setDetails(claims);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (Exception ignored) {}
        }
        filterChain.doFilter(request, response);
    }
}

