package com.web.application.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

public class ConfigInterceptor implements HandlerInterceptor {
	@Override
	public void postHandle(HttpServletRequest request, HttpServletResponse response, Object handler,
						   ModelAndView modelAndView) throws Exception {
		// Lấy thông tin Authentication từ Spring Security
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		// Kiểm tra nếu modelAndView không phải null
		if (modelAndView != null) {
			// Kiểm tra nếu người dùng không thuộc vai trò ROLE_ANONYMOUS (người chưa đăng nhập)
			if (!authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
				// Lấy thông tin chi tiết của người dùng từ Authentication
				CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();

				// Thêm thông tin người dùng vào modelAndView để truyền dữ liệu sang giao diện
				modelAndView.addObject("user_fullname", principal.getUser().getFullName()); // Họ tên người dùng
				modelAndView.addObject("user_phone", principal.getUser().getPhone());       // Số điện thoại
				modelAndView.addObject("user_email", principal.getUser().getEmail());       // Email
				modelAndView.addObject("user_address", principal.getUser().getAddress());   // Địa chỉ
				modelAndView.addObject("user_roles", principal.getUser().getRoles());       // Danh sách vai trò
				modelAndView.addObject("isLogined", true);                                  // Cờ xác định đã đăng nhập
			} else {
				// Nếu người dùng chưa đăng nhập, đặt biến isLogined là false
				modelAndView.addObject("isLogined", false);
			}
		}
	}
}
