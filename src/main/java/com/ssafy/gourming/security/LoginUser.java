package com.ssafy.gourming.security;

// 인증된 사용자의 ID와 JWT subject인 이메일을 보관한다.
public record LoginUser(
	String id,
	String email
) {
}
