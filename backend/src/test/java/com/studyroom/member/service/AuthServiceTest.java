package com.studyroom.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.common.security.JwtTokenProvider;
import com.studyroom.member.dto.LoginRequest;
import com.studyroom.member.dto.TokenResponse;
import com.studyroom.member.entity.Member;
import com.studyroom.member.repository.MemberRepository;
import com.studyroom.member.repository.RefreshTokenStore;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

	@Mock
	private MemberRepository memberRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@Mock
	private JwtTokenProvider tokenProvider;
	@Mock
	private RefreshTokenStore refreshTokenStore;
	@InjectMocks
	private AuthService authService;

	private Member member;

	@BeforeEach
	void setUp() {
		member = Member.create("user@test.com", "ENCODED", "테스터");
		ReflectionTestUtils.setField(member, "id", 1L);
	}

	@Test
	void 로그인_성공시_토큰을_발급하고_리프레시를_저장한다() {
		when(memberRepository.findByEmail("user@test.com")).thenReturn(Optional.of(member));
		when(passwordEncoder.matches("raw", "ENCODED")).thenReturn(true);
		when(tokenProvider.createAccessToken(1L, member.getRole())).thenReturn("ACCESS");
		when(tokenProvider.createRefreshToken(1L, member.getRole())).thenReturn("REFRESH");
		when(tokenProvider.getRefreshTokenExpireMs()).thenReturn(1000L);
		when(tokenProvider.getAccessTokenExpireMs()).thenReturn(100L);

		TokenResponse response = authService.login(new LoginRequest("user@test.com", "raw"));

		assertThat(response.accessToken()).isEqualTo("ACCESS");
		assertThat(response.refreshToken()).isEqualTo("REFRESH");
		verify(refreshTokenStore).save(eq(1L), eq("REFRESH"), any(Duration.class));
	}

	@Test
	void 비밀번호_불일치시_INVALID_CREDENTIALS() {
		when(memberRepository.findByEmail("user@test.com")).thenReturn(Optional.of(member));
		when(passwordEncoder.matches("wrong", "ENCODED")).thenReturn(false);

		assertThatThrownBy(() -> authService.login(new LoginRequest("user@test.com", "wrong")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
	}

	@Test
	void 없는_이메일_로그인시_INVALID_CREDENTIALS() {
		when(memberRepository.findByEmail("none@test.com")).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.login(new LoginRequest("none@test.com", "raw")))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.INVALID_CREDENTIALS);
	}

	@Test
	void 재발급시_저장된_리프레시와_다르면_거부한다() {
		when(tokenProvider.parseRefreshToken("OLD")).thenReturn(1L);
		when(refreshTokenStore.find(1L)).thenReturn(Optional.of("CURRENT"));

		assertThatThrownBy(() -> authService.reissue("OLD"))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.TOKEN_INVALID);
	}

	@Test
	void 재발급시_저장된_리프레시가_없으면_거부한다() {
		when(tokenProvider.parseRefreshToken("R")).thenReturn(1L);
		when(refreshTokenStore.find(1L)).thenReturn(Optional.empty());

		assertThatThrownBy(() -> authService.reissue("R"))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.TOKEN_INVALID);
	}

	@Test
	void 로그아웃은_저장소에서_리프레시를_삭제한다() {
		authService.logout(1L);

		verify(refreshTokenStore).delete(1L);
	}
}
