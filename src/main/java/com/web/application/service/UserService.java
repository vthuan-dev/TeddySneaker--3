package com.web.application.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;

import com.web.application.dto.UserDTO;
import com.web.application.entity.User;
import com.web.application.model.request.ChangePasswordRequest;
import com.web.application.model.request.CreateUserRequest;
import com.web.application.model.request.UpdateProfileRequest;

import java.util.List;

@Service
public interface UserService {
	List<UserDTO> getListUsers();

	public UserDTO getUsers(long id);

	Page<User> adminListUserPages(String fullName, String phone, String email, String role, Integer page);

	User createUser(CreateUserRequest createUserRequest);

	User createAdmin(CreateUserRequest createUserRequest);

	void changePassword(User user, ChangePasswordRequest changePasswordRequest);

	User updateProfile(User user, UpdateProfileRequest updateProfileRequest);

	void deleteAccount(User user);
}
