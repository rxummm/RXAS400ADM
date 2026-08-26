package com.rxas400adm.security.jwt;

import com.rxas400adm.security.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
public class JwtUtil {

    private final SecretKey key;
    private final long expireMs;
    /** P2: Refresh token 有效期（默认 7 天） */
    private final long refreshExpireMs;
    private final String issuer;
    private final String audience;

    // R7：5 处 @Value 收敛为 JwtProperties 单点绑定（rxas400.jwt.*）
    public JwtUtil(JwtProperties properties) {
        this.key = Keys.hmacShaKeyFor(properties.getSecret().getBytes(StandardCharsets.UTF_8));
        this.expireMs = properties.getExpireMs();
        this.refreshExpireMs = properties.getRefreshExpireMs();
        this.issuer = properties.getIssuer();
        this.audience = properties.getAudience();
    }

    /** Access token 有效期（毫秒），供前端计算主动刷新时机 */
    public long getExpireMs() {
        return expireMs;
    }

    /** P2-1：JWT 增加 iss/aud/jti（jti=UUID，便于后续按令牌吊销/追踪） */
    public String generateToken(String username, List<String> permissions) {
        Date now = new Date();
        return Jwts.builder()
                .issuer(issuer)
                .audience().add(audience).and()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim("permissions", permissions)
                .claim("type", "access")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + expireMs))
                .signWith(key)
                .compact();
    }

    /** P2: 生成 refresh token（有效期更长，不含 permissions） */
    public String generateRefreshToken(String username) {
        Date now = new Date();
        return Jwts.builder()
                .issuer(issuer)
                .audience().add(audience).and()
                .id(UUID.randomUUID().toString())
                .subject(username)
                .claim("type", "refresh")
                .issuedAt(now)
                .expiration(new Date(now.getTime() + refreshExpireMs))
                .signWith(key)
                .compact();
    }

    /** 判断 token 是否为 refresh token */
    public boolean isRefreshToken(String token) {
        return "refresh".equals(parse(token).get("type"));
    }

    public String getUsername(String token) {
        return parse(token).getSubject();
    }

    /** 返回 jti（吊销名单主键，P2-1） */
    public String getJti(String token) {
        return parse(token).getId();
    }

    /** 返回 token 剩余有效毫秒数（吊销登记用），已过期返回 0 */
    public long getRemainingMs(String token) {
        Date exp = parse(token).getExpiration();
        return Math.max(0, exp.getTime() - System.currentTimeMillis());
    }

    @SuppressWarnings("unchecked")
    public List<String> getPermissions(String token) {
        Object permissions = parse(token).get("permissions");
        return permissions instanceof List ? (List<String>) permissions : List.of();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parse(String token) {
        // P2-1：校验 iss/aud——非本平台签发的令牌（伪造 iss/aud）直接拒绝
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .requireAudience(audience)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
