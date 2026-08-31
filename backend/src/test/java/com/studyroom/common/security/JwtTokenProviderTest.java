package com.studyroom.common.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.member.entity.MemberRole;
import org.junit.jupiter.api.Test;

class JwtTokenProviderTest {

	private static final String SECRET = "test-secret-test-secret-test-secret-0123456789";

	private final JwtTokenProvider provider = new JwtTokenProvider(
			new JwtProperties(SECRET, 3_600_000L, 604_800_000L));

	@Test
	void 액세스_토큰_생성_후_파싱하면_주체가_복원된다() {
		String token = provider.createAccessToken(42L, MemberRole.ADMIN);

		MemberPrincipal principal = provider.parse(token);

		assertThat(principal.memberId()).isEqualTo(42L);
		assertThat(principal.role()).isEqualTo(MemberRole.ADMIN);
	}

	@Test
	void 리프레시_토큰은_parseRefreshToken으로_memberId를_돌려준다() {
		String refresh = provider.createRefreshToken(7L, MemberRole.USER);

		assertThat(provider.parseRefreshToken(refresh)).isEqualTo(7L);
	}

	@Test
	void 액세스_토큰을_parseRefreshToken에_넣으면_거부된다() {
		String access = provider.createAccessToken(7L, MemberRole.USER);

		assertThatThrownBy(() -> provider.parseRefreshToken(access))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.TOKEN_INVALID);
	}

	@Test
	void 다른_시크릿으로_서명된_토큰은_검증에_실패한다() {
		String otherSecret = "another-secret-another-secret-another-9876543210";
		JwtTokenProvider attacker = new JwtTokenProvider(
				new JwtProperties(otherSecret, 3_600_000L, 604_800_000L));
		String forged = attacker.createAccessToken(1L, MemberRole.ADMIN);

		assertThat(provider.isValid(forged)).isFalse();
		assertThatThrownBy(() -> provider.parse(forged))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.TOKEN_INVALID);
	}

	@Test
	void 만료된_토큰은_검증에_실패한다() {
		JwtTokenProvider expiring = new JwtTokenProvider(new JwtProperties(SECRET, -60_000L, -60_000L));
		String token = expiring.createAccessToken(1L, MemberRole.USER);

		assertThat(expiring.isValid(token)).isFalse();
		assertThatThrownBy(() -> expiring.parse(token)).isInstanceOf(BusinessException.class);
	}

	@Test
	void 짧은_시크릿은_생성_시점에_거부된다() {
		assertThatThrownBy(() -> new JwtTokenProvider(new JwtProperties("short", 1000L, 1000L)))
				.isInstanceOf(IllegalStateException.class);
	}
}
