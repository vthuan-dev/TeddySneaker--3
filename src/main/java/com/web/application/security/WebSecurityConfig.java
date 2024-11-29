package com.web.application.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class WebSecurityConfig {

	private final AuthenticationEntryPoint jwtAuthenticationEntryPoint;
	private final JwtRequestFillter jwtRequestFilter;

	public WebSecurityConfig(AuthenticationEntryPoint jwtAuthenticationEntryPoint, JwtRequestFillter jwtRequestFilter) {
		this.jwtAuthenticationEntryPoint = jwtAuthenticationEntryPoint;
		this.jwtRequestFilter = jwtRequestFilter;
	}

	@Bean
	public BCryptPasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
		return config.getAuthenticationManager();
	}

	@Bean
	public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
		return http.csrf(AbstractHttpConfigurer::disable)
				.cors(AbstractHttpConfigurer::disable)
				.authorizeHttpRequests(request -> {
					request.requestMatchers("/api/order", "/tai-khoan", "/tai-khoan/**", "/api/change-password", "/api/update-profile")
							.authenticated();
					request.requestMatchers("/admin/**", "/api/admin/**")
							.hasRole("ADMIN");
					request.anyRequest().permitAll();
				})
				.logout(logout -> logout
						.logoutUrl("/api/logout")
						.logoutSuccessUrl("/")
						.deleteCookies("JWT_TOKEN"))
				.exceptionHandling(exception -> exception
						.authenticationEntryPoint(jwtAuthenticationEntryPoint))
				.sessionManagement(session -> session
						.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
				.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
				.build();
	}

//	Để lại làm tài liệu tham khảo
//	@Bean
//	public SecurityFilterChain securityFilterChain(HttpSecurity httpSecurity) throws Exception {
//		httpSecurity.cors().and().csrf().disable().authorizeRequests()
//				.requestMatchers("/api/order", "/tai-khoan", "/tai-khoan/**", "/api/change-password",
//						"/api/update-profile")
//				.authenticated().requestMatchers("/admin/**", "/api/admin/**").hasRole("ADMIN").anyRequest().permitAll()
//				.and().logout().logoutUrl("/api/logout").logoutSuccessUrl("/").deleteCookies("JWT_TOKEN").and()
//				.exceptionHandling().authenticationEntryPoint(jwtAuthenticationEntryPoint).and().sessionManagement()
//				.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
//
//		httpSecurity.addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class);
//
//		return httpSecurity.build();
//	}

}
