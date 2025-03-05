package com.web.application.controller.client;

import java.util.List;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import com.web.application.config.Contant;
import com.web.application.dto.UserDTO;
import com.web.application.entity.User;
import com.web.application.exception.BadRequestExp;
import com.web.application.model.mapper.UserMapper;
import com.web.application.model.request.ChangePasswordRequest;
import com.web.application.model.request.CreateUserRequest;
import com.web.application.model.request.LoginRequest;
import com.web.application.model.request.UpdateProfileRequest;
import com.web.application.security.CustomUserDetails;
import com.web.application.security.JwtTokenUtil;
import com.web.application.service.UserService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;

@Controller
public class UserController {

	@Autowired
	private UserService userService;

	@Autowired
	private JwtTokenUtil jwtTokenUtil;

	@Autowired
	private AuthenticationConfiguration authenticationConfiguration;

	@GetMapping("/users")
	public ResponseEntity<Object> getListUsers() {
		List<UserDTO> userDTOS = userService.getListUsers();
		return ResponseEntity.ok(userDTOS);
	}

	@PostMapping("/api/admin/users")
	public ResponseEntity<Object> createUser(@Valid @RequestBody CreateUserRequest createUserRequest) {
		User user = userService.createUser(createUserRequest);
		return ResponseEntity.ok(UserMapper.toUserDTO(user));
	}

	@PostMapping("/api/register")
	public ResponseEntity<Object> register(@Valid @RequestBody CreateUserRequest createUserRequest,
			HttpServletResponse response) {
		// Create user
		User user = userService.createUser(createUserRequest);

		// Gen token
		UserDetails principal = new CustomUserDetails(user);
		String token = jwtTokenUtil.generateToken(principal);

		// Add token on cookie to login
		Cookie cookie = new Cookie("JWT_TOKEN", token);
		cookie.setMaxAge(Contant.MAX_AGE_COOKIE);
		cookie.setPath("/");
		response.addCookie(cookie);

		return ResponseEntity.ok(UserMapper.toUserDTO(user));
	}

	@PostMapping("/api/login")
	public ResponseEntity<Object> login(@Valid @RequestBody LoginRequest loginRequest, HttpServletResponse response) {
		try {
			AuthenticationManager authenticationManager = authenticationConfiguration.getAuthenticationManager();

			Authentication authentication = authenticationManager.authenticate(
					new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));
			CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
			User user = userDetails.getUser();
			System.out.println(user.isStatus());
			if (!user.isStatus()) {
				throw new BadRequestExp("Tài khoản này đã xóa!");
			}
			String token = jwtTokenUtil.generateToken((CustomUserDetails) authentication.getPrincipal());

			Cookie cookie = new Cookie("JWT_TOKEN", token);
			cookie.setMaxAge(Contant.MAX_AGE_COOKIE);
			cookie.setPath("/");
			response.addCookie(cookie);

			return ResponseEntity
					.ok(UserMapper.toUserDTO(((CustomUserDetails) authentication.getPrincipal()).getUser()));
		} catch (BadCredentialsException ex) {
			throw new BadRequestExp("Email hoặc mật khẩu không chính xác!");
		} catch (Exception e) {
			throw new BadRequestExp("Tài khoản này đã xóa!");
		}
	}

	@GetMapping("/tai-khoan")
	public String getProfilePage(Model model) {
		return "shop/account";
	}

	@PostMapping("/api/change-password")
	public ResponseEntity<Object> changePassword(@Valid @RequestBody ChangePasswordRequest passwordReq) {
		User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
				.getUser();
		userService.changePassword(user, passwordReq);
		return ResponseEntity.ok("Đổi mật khẩu thành công");
	}

	@PutMapping("/api/update-profile")
	public ResponseEntity<Object> updateProfile(@Valid @RequestBody UpdateProfileRequest profileReq) {
		User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
				.getUser();
		System.out.println(user.getFullName());
		user = userService.updateProfile(user, profileReq);
		UserDetails userDetails = new CustomUserDetails(user);
		Authentication authentication = new UsernamePasswordAuthenticationToken(userDetails, null,
				userDetails.getAuthorities());
		SecurityContextHolder.getContext().setAuthentication(authentication);

		return ResponseEntity.ok("Cập nhật thành công");
	}

	@PutMapping("/api/delete-account")
	public ResponseEntity<Object> deleteAccount() {
		User user = ((CustomUserDetails) SecurityContextHolder.getContext().getAuthentication().getPrincipal())
				.getUser();
		System.out.println(user.getFullName());
		userService.deleteAccount(user);
		return ResponseEntity.ok("Hủy tài khoản thành công");
	}
}
