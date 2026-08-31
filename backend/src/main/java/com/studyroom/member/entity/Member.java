package com.studyroom.member.entity;

import com.studyroom.common.entity.BaseTimeEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "members")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(nullable = false, unique = true, length = 320)
	private String email;

	@Column(nullable = false)
	private String password;

	@Column(nullable = false, length = 50)
	private String name;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private MemberRole role;

	private Member(String email, String encodedPassword, String name, MemberRole role) {
		this.email = email;
		this.password = encodedPassword;
		this.name = name;
		this.role = role;
	}

	public static Member create(String email, String encodedPassword, String name) {
		return new Member(email, encodedPassword, name, MemberRole.USER);
	}

	/** 시드/부트스트랩 전용 관리자 계정 생성. 일반 가입 경로에서는 USER 만 만들어진다. */
	public static Member createAdmin(String email, String encodedPassword, String name) {
		return new Member(email, encodedPassword, name, MemberRole.ADMIN);
	}
}
