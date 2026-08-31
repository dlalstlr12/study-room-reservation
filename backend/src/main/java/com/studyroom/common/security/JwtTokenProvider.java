package com.studyroom.common.security;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.member.entity.MemberRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/**
 * JWT 액세스/리프레시 토큰 발급·검증. jjwt 0.12.x API 사용.
 * 토큰에는 subject(memberId), {@code role}, {@code type}(access|refresh) 클레임이 담긴다.
 */
@Component
public class JwtTokenProvider {

	private static final String CLAIM_ROLE = "role";
	private static final String CLAIM_TYPE = "type";
	private static final String TYPE_ACCESS = "access";
	private static final String TYPE_REFRESH = "refresh";

	private final SecretKey key;
	private final long accessTokenExpireMs;
	private final long refreshTokenExpireMs;

	public JwtTokenProvider(JwtProperties properties) {
		byte[] secretBytes = properties.secret().getBytes(StandardCharsets.UTF_8);
		if (secretBytes.length < 32) {
			throw new IllegalStateException("jwt.secret 은 최소 32바이트여야 합니다. 현재 " + secretBytes.length + "바이트");
		}
		this.key = Keys.hmacShaKeyFor(secretBytes);
		this.accessTokenExpireMs = properties.accessTokenExpireMs();
		this.refreshTokenExpireMs = properties.refreshTokenExpireMs();
	}

	public String createAccessToken(Long memberId, MemberRole role) {
		return build(memberId, role, TYPE_ACCESS, accessTokenExpireMs);
	}

	public String createRefreshToken(Long memberId, MemberRole role) {
		return build(memberId, role, TYPE_REFRESH, refreshTokenExpireMs);
	}

	public long getAccessTokenExpireMs() {
		return accessTokenExpireMs;
	}

	public long getRefreshTokenExpireMs() {
		return refreshTokenExpireMs;
	}

	/**
	 * 서명·만료를 검증하고 주체를 복원한다. 실패 시 {@link BusinessException}(TOKEN_INVALID).
	 */
	public MemberPrincipal parse(String token) {
		Claims claims = parseClaims(token);
		Long memberId = Long.valueOf(claims.getSubject());
		MemberRole role = MemberRole.valueOf(claims.get(CLAIM_ROLE, String.class));
		return new MemberPrincipal(memberId, role);
	}

	/** 리프레시 토큰 검증 후 memberId 반환. type 이 refresh 가 아니면 거부. */
	public Long parseRefreshToken(String token) {
		Claims claims = parseClaims(token);
		if (!TYPE_REFRESH.equals(claims.get(CLAIM_TYPE, String.class))) {
			throw new BusinessException(ErrorCode.TOKEN_INVALID);
		}
		return Long.valueOf(claims.getSubject());
	}

	public boolean isValid(String token) {
		try {
			parseClaims(token);
			return true;
		} catch (BusinessException e) {
			return false;
		}
	}

	private String build(Long memberId, MemberRole role, String type, long expireMs) {
		Date now = new Date();
		Date expiry = new Date(now.getTime() + expireMs);
		return Jwts.builder()
				.subject(String.valueOf(memberId))
				.claim(CLAIM_ROLE, role.name())
				.claim(CLAIM_TYPE, type)
				.issuedAt(now)
				.expiration(expiry)
				.signWith(key)
				.compact();
	}

	private Claims parseClaims(String token) {
		try {
			return Jwts.parser()
					.verifyWith(key)
					.build()
					.parseSignedClaims(token)
					.getPayload();
		} catch (JwtException | IllegalArgumentException e) {
			throw new BusinessException(ErrorCode.TOKEN_INVALID);
		}
	}
}
