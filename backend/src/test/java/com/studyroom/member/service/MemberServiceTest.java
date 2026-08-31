package com.studyroom.member.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.member.dto.MemberResponse;
import com.studyroom.member.dto.SignUpRequest;
import com.studyroom.member.entity.Member;
import com.studyroom.member.repository.MemberRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class MemberServiceTest {

	@Mock
	private MemberRepository memberRepository;
	@Mock
	private PasswordEncoder passwordEncoder;
	@InjectMocks
	private MemberService memberService;

	@Test
	void 회원가입_성공시_비밀번호를_인코딩해_저장한다() {
		SignUpRequest request = new SignUpRequest("user@test.com", "password1", "테스터");
		when(memberRepository.existsByEmail("user@test.com")).thenReturn(false);
		when(passwordEncoder.encode("password1")).thenReturn("ENCODED");
		when(memberRepository.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));

		MemberResponse response = memberService.signUp(request);

		assertThat(response.email()).isEqualTo("user@test.com");
		verify(passwordEncoder).encode("password1");
	}

	@Test
	void 이메일이_중복이면_예외를_던지고_저장하지_않는다() {
		SignUpRequest request = new SignUpRequest("dup@test.com", "password1", "테스터");
		when(memberRepository.existsByEmail("dup@test.com")).thenReturn(true);

		assertThatThrownBy(() -> memberService.signUp(request))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.EMAIL_DUPLICATED);
		verify(memberRepository, never()).save(any());
		verify(passwordEncoder, never()).encode(anyString());
	}

	@Test
	void 존재하지_않는_회원_조회시_예외() {
		when(memberRepository.findById(99L)).thenReturn(java.util.Optional.empty());

		assertThatThrownBy(() -> memberService.getById(99L))
				.isInstanceOf(BusinessException.class)
				.extracting("errorCode").isEqualTo(ErrorCode.MEMBER_NOT_FOUND);
	}
}
