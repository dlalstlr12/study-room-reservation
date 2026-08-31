package com.studyroom.member.service;

import com.studyroom.common.exception.BusinessException;
import com.studyroom.common.exception.ErrorCode;
import com.studyroom.member.dto.MemberResponse;
import com.studyroom.member.dto.SignUpRequest;
import com.studyroom.member.entity.Member;
import com.studyroom.member.repository.MemberRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class MemberService {

	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;

	public MemberService(MemberRepository memberRepository, PasswordEncoder passwordEncoder) {
		this.memberRepository = memberRepository;
		this.passwordEncoder = passwordEncoder;
	}

	@Transactional
	public MemberResponse signUp(SignUpRequest request) {
		if (memberRepository.existsByEmail(request.email())) {
			throw new BusinessException(ErrorCode.EMAIL_DUPLICATED);
		}
		Member member = Member.create(
				request.email(),
				passwordEncoder.encode(request.password()),
				request.name());
		return MemberResponse.from(memberRepository.save(member));
	}

	public Member getById(Long memberId) {
		return memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(ErrorCode.MEMBER_NOT_FOUND));
	}

	public MemberResponse getMe(Long memberId) {
		return MemberResponse.from(getById(memberId));
	}
}
