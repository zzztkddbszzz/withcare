package com.withcare.util;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

public class JwtToken {

    public static class JwtUtils {
        private static SecretKey pri_key=null;
        
        public static SecretKey getPri_key() {
            return pri_key;
        }

        public static void setPri_key() {
            JwtUtils.pri_key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
        }

        public static String setToken(Map<String, Object> map) {
            return Jwts.builder()
                    .setHeaderParam("alg", "HS256")
                    .setHeaderParam("typ", "JWT")
                    .setClaims(map)
                    .setIssuedAt(new Date())
                    .setExpiration(new Date(System.currentTimeMillis() + (1000 * 60 * 60 * 10)))  // 10시간 유효
                    .signWith(pri_key)
                    .compact();
        }

        public static String setToken(String key, Object value) {
            Map<String, Object> map = new HashMap<>();
            map.put(key, value);
            return setToken(map);
        }

        public static Map<String, Object> readToken(String token) throws JwtException {
            if (token == null || token.isEmpty()) {
                throw new JwtException("토큰이 없습니다.");
            }

            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(pri_key)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();
                
                Map<String, Object> resp = new HashMap<>();
                for (String key : claims.keySet()) {
                    resp.put(key, claims.get(key));
                }
                return resp;
            } catch (ExpiredJwtException e) {
                throw new JwtException("토큰이 만료되었습니다.");
            } catch (Exception e) {
                throw new JwtException("유효하지 않은 토큰입니다.");
            }
        }
    }
}
