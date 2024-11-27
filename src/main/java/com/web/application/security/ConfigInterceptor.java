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
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (modelAndView != null) { // Kiểm tra nếu modelAndView không phải là null
			if (!authentication.getAuthorities().contains(new SimpleGrantedAuthority("ROLE_ANONYMOUS"))) {
				CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();
				modelAndView.addObject("user_fullname", principal.getUser().getFullName());
				modelAndView.addObject("user_phone", principal.getUser().getPhone());
				modelAndView.addObject("user_email", principal.getUser().getEmail());
				modelAndView.addObject("user_address", principal.getUser().getAddress());
				modelAndView.addObject("user_roles", principal.getUser().getRoles());
				modelAndView.addObject("isLogined", true);
			} else {
				modelAndView.addObject("isLogined", false);
			}
		}
	}

}
