package com.ssafy.gourming.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ssafy.gourming.model.dto.UserDto;
import com.ssafy.gourming.model.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final UserService userService;

	@PostMapping("/signup")
	public ResponseEntity<String> signup(@RequestBody @Valid UserDto.SignupRequest request) {
		userService.signup(request);
		return ResponseEntity.status(HttpStatus.CREATED).body("회원가입이 완료되었습니다.");
	}

	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody @Valid UserDto.LoginRequest request) {
		UserDto.LoginResponse response = userService.login(request);
		return ResponseEntity.ok(response);
	}
}
