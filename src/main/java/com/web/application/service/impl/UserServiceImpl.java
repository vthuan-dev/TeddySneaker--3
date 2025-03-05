package com.web.application.service.impl;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.stereotype.Component;

import com.web.application.config.Contant;
import com.web.application.dto.UserDTO;
import com.web.application.entity.User;
import com.web.application.exception.BadRequestExp;
import com.web.application.model.mapper.UserMapper;
import com.web.application.model.request.ChangePasswordRequest;
import com.web.application.model.request.CreateUserRequest;
import com.web.application.model.request.UpdateProfileRequest;
import com.web.application.repository.UserRepository;
import com.web.application.service.UserService;

import java.util.ArrayList;
import java.util.List;

@Component
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepository userRepository;

	@Override
	public List<UserDTO> getListUsers() {
		List<User> users = userRepository.findAll();
		List<UserDTO> userDTOS = new ArrayList<>();
		for (User user : users) {
			userDTOS.add(UserMapper.toUserDTO(user));
		}
		return userDTOS;
	}

	@Override
	public UserDTO getUsers(long id) {
		return userRepository.findById(id).map(UserMapper::toUserDTO).orElse(null);
	}

	@Override
	public Page<User> adminListUserPages(String fullName, String phone, String email, String role, Integer page) {
		page--;
		if (page < 0) {
			page = 0;
		}
		Pageable pageable = PageRequest.of(page, Contant.LIMIT_USER, Sort.by("created_at").descending());
		System.out.println(role);
		return userRepository.adminListUserPages(fullName, phone, email, role, pageable);
	}

	@Override
	public User createUser(CreateUserRequest createUserRequest) {
		User user = userRepository.findByEmail(createUserRequest.getEmail());
		if (user != null) {
			throw new BadRequestExp("Email đã tồn tại trong hệ thống. Vui lòng sử dụng email khác!");
		}
		user = UserMapper.toUser(createUserRequest);
		userRepository.save(user);
		return user;
	}

	@Override
	public void changePassword(User user, ChangePasswordRequest changePasswordRequest) {
		// Kiểm tra mật khẩu
		if (!BCrypt.checkpw(changePasswordRequest.getOldPassword(), user.getPassword())) {
			throw new BadRequestExp("Mật khẩu cũ không chính xác");
		}

		String hash = BCrypt.hashpw(changePasswordRequest.getNewPassword(), BCrypt.gensalt(12));
		user.setPassword(hash);
		userRepository.save(user);
	}

	@Override
	public User updateProfile(User user, UpdateProfileRequest updateProfileRequest) {
		user.setFullName(updateProfileRequest.getFullName());
		user.setPhone(updateProfileRequest.getPhone());
		user.setAddress(updateProfileRequest.getAddress());

		return userRepository.save(user);
	}

	@Override
	public void deleteAccount(User user) {
		user.setStatus(false);
		userRepository.save(user);
	}

	@Override
	public User createAdmin(CreateUserRequest createUserRequest) {
		User user = userRepository.findByEmail(createUserRequest.getEmail());
		if (user != null) {
			throw new BadRequestExp("Email đã tồn tại trong hệ thống. Vui lòng sử dụng email khác!");
		}
		user = UserMapper.toAdmin(createUserRequest);
		userRepository.save(user);
		return user;
	}

}
