package com.agatha.todo_api.service;

import java.util.Date;
import java.util.Optional;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

@Service
public class JwtService {

	private final SecretKey key;
	private final long expirationMs;

	public JwtService(@Value("${app.jwt.secret}") String secret,
			@Value("${app.jwt.expiration-ms}") long expirationMs) {
		this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
		this.expirationMs = expirationMs;
	}

	public String generateToken(String username) {
		Date now = new Date();
		return Jwts.builder()
				.subject(username)
				.issuedAt(now)
				.expiration(new Date(now.getTime() + expirationMs))
				.signWith(key)
				.compact();
	}

	/** Retorna o username se o token for valido (assinatura e expiracao), senao vazio. */
	public Optional<String> extractUsername(String token) {
		try {
			return Optional.ofNullable(
					Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject());
		} catch (JwtException | IllegalArgumentException e) {
			return Optional.empty();
		}
	}
}