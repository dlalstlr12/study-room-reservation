package com.studyroom.common.security;

import com.studyroom.member.entity.MemberRole;

/**
 * 인증된 요청의 주체. JWT 클레임에서 복원되며 DB 조회 없이 SecurityContext에 저장된다.
 * 컨트롤러에서 {@code @AuthenticationPrincipal MemberPrincipal}로 주입받는다.
 */
public record MemberPrincipal(Long memberId, MemberRole role) {
}
