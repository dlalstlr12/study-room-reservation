package com.studyroom.member.service;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.common.security.JwtTokenProvider;
import com.studyroom.member.dto.LoginRequest;
import com.studyroom.member.dto.TokenResponse;
import com.studyroom.member.entity.Member;
import com.studyroom.member.repository.MemberRepository;
import com.studyroom.member.repository.RefreshTokenStore;
import java.time.Duration;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AuthService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider tokenProvider;
	private final RefreshTokenStore refreshTokenStore;

	public AuthService(MemberRepository memberRepository, PasswordEncoder passwordEncoder,
			JwtTokenProvider tokenProvider, RefreshTokenStore refreshTokenStore) {
		this.memberRepository = memberRepository;
		this.passwordEncoder = passwordEncoder;
		this.tokenProvider = tokenProvider;
		this.refreshTokenStore = refreshTokenStore;
	}

	public TokenResponse login(LoginRequest request) {
		Member member = memberRepository.findByEmail(request.email())
				.orElseThrow(() -> new BusinessException(ErrorCode.INVALID_CREDENTIALS));
		if (!passwordEncoder.matches(request.password(), member.getPassword())) {
			throw new BusinessException(ErrorCode.INVALID_CREDENTIALS);
		}
		return issueTokens(member);
	}

	/**
	 * 리프레시 토큰 회전. 서명·타입 검증 후 Redis 저장본과 일치할 때만 새 토큰 쌍을 발급하고
	 * 저장본을 교체한다. 이미 사용된(교체된) 토큰으로는 재발급되지 않는다.
	 */
	public TokenResponse reissue(String refreshToken) {
		Long memberId = tokenProvider.parseRefreshToken(refreshToken);
		String stored = refreshTokenStore.find(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.TOKEN_INVALID));
		if (!stored.equals(refreshToken)) {
			throw new BusinessException(ErrorCode.TOKEN_INVALID);
		}
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
		return issueTokens(member);
	}

	public void logout(Long memberId) {
		refreshTokenStore.delete(memberId);
	}

	private TokenResponse issueTokens(Member member) {
		String accessToken = tokenProvider.createAccessToken(member.getId(), member.getRole());
		String refreshToken = tokenProvider.createRefreshToken(member.getId(), member.getRole());
		refreshTokenStore.save(member.getId(), refreshToken,
				Duration.ofMillis(tokenProvider.getRefreshTokenExpireMs()));
		return TokenResponse.of(accessToken, refreshToken, tokenProvider.getAccessTokenExpireMs());
	}
}
