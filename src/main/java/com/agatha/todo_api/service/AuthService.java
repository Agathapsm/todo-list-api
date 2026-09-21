package com.agatha.todo_api.service;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.agatha.todo_api.dto.AuthRequest;
import com.agatha.todo_api.dto.TokenResponse;
import com.agatha.todo_api.entity.User;
import com.agatha.todo_api.repository.UserRepository;

@Service
public class AuthService {

	private final UserRepository userRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtService jwtService;

	public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, JwtService jwtService) {
		this.userRepository = userRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtService = jwtService;
	}

	public void register(AuthRequest request) {
		if (userRepository.existsByUsername(request.username())) {
			throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already in use");
		}
		userRepository.save(new User(request.username(), passwordEncoder.encode(request.password())));
	}

	public TokenResponse login(AuthRequest request) {
		User user = userRepository.findByUsername(request.username())
				.filter(u -> passwordEncoder.matches(request.password(), u.getPassword()))
				.orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));
		return new TokenResponse(jwtService.generateToken(user.getUsername()));
	}
}