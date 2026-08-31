package com.studyroom.member.dto;

public record TokenResponse(
		String accessToken,
		String refreshToken,
		String tokenType,
		long accessTokenExpiresInMs
) {
	public static TokenResponse of(String accessToken, String refreshToken, long accessTokenExpiresInMs) {
		return new TokenResponse(accessToken, refreshToken, "Bearer", accessTokenExpiresInMs);
	}
}
