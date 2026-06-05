package com.ssafy.gourming.model.service;

import com.ssafy.gourming.model.dto.UserDto;

public interface UserService {
	void signup(UserDto.SignupRequest request);
	UserDto.LoginResponse login(UserDto.LoginRequest request);
	UserDto.UserProfileResponse getUserProfile(String handle);
}
