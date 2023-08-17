package ru.toughdev.ates.account.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.util.ObjectUtils;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    @Value("${jwt.token.secret}")
    private String secret;

    @PostConstruct
    protected void init() {
        secret = Base64.getEncoder().encodeToString(secret.getBytes());
    }

    public Authentication getAuthentication(String token) {
        UserDetails userDetails = parseUserDetails(token);
        return new UsernamePasswordAuthenticationToken(userDetails, "", userDetails.getAuthorities());
    }

    public UserDetails parseUserDetails(String token) {
        Jws<Claims> claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
        var role = (String) claims.getBody().get("roles");
        var userPublicId = UUID.fromString((String) claims.getBody().get("userId"));

        return new JwtUser(
                userPublicId,
                claims.getBody().getSubject(),
                List.of(new SimpleGrantedAuthority(role))
        );
    }

    public String resolveToken(HttpServletRequest req) {
        String bearerToken = req.getHeader("Authorization");
        if (bearerToken != null && bearerToken.startsWith("Bearer ")) {
            return bearerToken.substring(7);
        }
        return null;
    }

    public boolean validateToken(String token) {
        if (ObjectUtils.isEmpty(token)) {
            throw new RuntimeException("Token not found");
        }

        Jws<Claims> claims = Jwts.parser().setSigningKey(secret).parseClaimsJws(token);
        if (claims.getBody().getExpiration().before(new Date())) {
            return false;
        }

        return true;
    }
}
