package com.web.application.controller.admin;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import com.web.application.dto.UserDTO;
import com.web.application.entity.User;
import com.web.application.model.mapper.UserMapper;
import com.web.application.model.request.CreateUserRequest;
import com.web.application.model.request.UpdateProfileRequest;
import com.web.application.service.UserService;

@Controller
public class AdminUserController {

	@Autowired
	private UserService userService;

	@GetMapping("/admin/users")
	public String homePages(Model model, @RequestParam(defaultValue = "", required = false) String fullName,
			@RequestParam(defaultValue = "", required = false) String phone,
			@RequestParam(defaultValue = "", required = false) String email,
			@RequestParam(defaultValue = "", required = false) String role,
			@RequestParam(defaultValue = "", required = false) String address,
			@RequestParam(defaultValue = "1", required = false) Integer page) {
		Page<User> users = userService.adminListUserPages(fullName, phone, email, role, page);
		model.addAttribute("users", users.getContent());
		model.addAttribute("totalPages", users.getTotalPages());
		model.addAttribute("currentPage", users.getPageable().getPageNumber() + 1);
		return "admin/user/list";
	}

	@GetMapping("/api/admin/users/list")
	public ResponseEntity<Object> getListUserPages(@RequestParam(defaultValue = "", required = false) String fullName,
			@RequestParam(defaultValue = "", required = false) String phone,
			@RequestParam(defaultValue = "", required = false) String email,
			@RequestParam(defaultValue = "", required = false) String role,
			@RequestParam(defaultValue = "", required = false) String address,
			@RequestParam(defaultValue = "1", required = false) Integer page) {
		Page<User> users = userService.adminListUserPages(fullName, phone, email, role, page);
		return ResponseEntity.ok(users);
	}

	@PostMapping("/api/admin/users/create")
	public ResponseEntity<Object> createUser(@Valid @RequestBody CreateUserRequest createUserRequest) {
		System.out.println(createUserRequest);
		User newUser = userService.createAdmin(createUserRequest);
		return ResponseEntity.ok(UserMapper.toUserDTO(newUser));
	}

	@GetMapping("/admin/users/create")
	public String createUserPage(Model model) {
		return "admin/user/create";
	}

}
