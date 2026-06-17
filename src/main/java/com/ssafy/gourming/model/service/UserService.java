package com.ssafy.gourming.model.service;

import com.ssafy.gourming.model.dto.UserDto;

public interface UserService {
	void signup(UserDto.SignupRequest request);
	UserDto.LoginResponse login(UserDto.LoginRequest request);
	boolean isHandleAvailable(String handle);
	UserDto.UserProfileResponse getUserProfile(String handle, String authenticatedUserId);
	java.util.List<UserDto.UserProfileResponse> searchUsers(String keyword, String authenticatedUserId, int limit, int offset);
	void updateProfile(String id, String authenticatedEmail, UserDto.UpdateProfileRequest request);
}
