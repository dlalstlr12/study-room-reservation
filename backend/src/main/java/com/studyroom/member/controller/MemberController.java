package com.studyroom.member.controller;

import com.studyroom.common.security.MemberPrincipal;
import com.studyroom.member.dto.MemberResponse;
import com.studyroom.member.service.MemberService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member", description = "회원 정보")
@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/api/members")
public class MemberController {

	private final MemberService memberService;

	public MemberController(MemberService memberService) {
		this.memberService = memberService;
	}

	@Operation(summary = "내 정보 조회")
	@GetMapping("/me")
	public MemberResponse me(@AuthenticationPrincipal MemberPrincipal principal) {
		return memberService.getMe(principal.memberId());
	}
}
