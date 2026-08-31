package com.studyroom.member.controller;

import com.studyroom.common.security.MemberPrincipal;
import com.studyroom.member.dto.LoginRequest;
import com.studyroom.member.dto.MemberResponse;
import com.studyroom.member.dto.ReissueRequest;
import com.studyroom.member.dto.SignUpRequest;
import com.studyroom.member.dto.TokenResponse;
import com.studyroom.member.service.AuthService;
import com.studyroom.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Auth", description = "회원가입 · 로그인 · 토큰 관리")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

	private final MemberService memberService;
	private final AuthService authService;

	public AuthController(MemberService memberService, AuthService authService) {
		this.memberService = memberService;
		this.authService = authService;
	}

	@Operation(summary = "회원가입")
	@PostMapping("/signup")
	public ResponseEntity<MemberResponse> signUp(@Valid @RequestBody SignUpRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(memberService.signUp(request));
	}

	@Operation(summary = "로그인", description = "액세스/리프레시 토큰을 발급한다.")
	@PostMapping("/login")
	public TokenResponse login(@Valid @RequestBody LoginRequest request) {
		return authService.login(request);
	}

	@Operation(summary = "토큰 재발급", description = "유효한 리프레시 토큰으로 새 토큰 쌍을 발급한다(회전).")
	@PostMapping("/reissue")
	public TokenResponse reissue(@Valid @RequestBody ReissueRequest request) {
		return authService.reissue(request.refreshToken());
	}

	@Operation(summary = "로그아웃", description = "서버에 저장된 리프레시 토큰을 폐기한다.")
	@PostMapping("/logout")
	public ResponseEntity<Void> logout(@AuthenticationPrincipal MemberPrincipal principal) {
		authService.logout(principal.memberId());
		return ResponseEntity.noContent().build();
	}
}
