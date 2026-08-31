package com.studyroom.member.dto;

import com.studyroom.member.entity.Member;
import com.studyroom.member.entity.MemberRole;
import java.time.LocalDateTime;

public record MemberResponse(
		Long id,
		String email,
		String name,
		MemberRole role,
		LocalDateTime createdAt
) {
	public static MemberResponse from(Member member) {
		return new MemberResponse(
				member.getId(),
				member.getEmail(),
				member.getName(),
				member.getRole(),
				member.getCreatedAt()
		);
	}
}
