package com.app.myblogpusher.service;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.repository.UserMasterRepository;

@Service
public class LoginService {

	@Autowired
	private UserMasterRepository userMasterRepository;
	@Autowired
	private PasswordEncoder passwordEncoder;

	public Optional<UserMaster> findAuthenticatedUser(String loginId, String password) {
		Optional<UserMaster> userOpt = userMasterRepository.findByLoginId(loginId);

		if (userOpt.isEmpty()) {
			return Optional.empty();
		}

		UserMaster user = userOpt.get();

		if ("BANNED".equals(user.getStatus())) {
			return Optional.empty();
		}

		if (!passwordEncoder.matches(password, user.getPassword())) {
			return Optional.empty();
		}

		return Optional.of(user);
	}

	public Optional<UserMaster> findUserForPasswordReset(String loginId, String email) {
		Optional<UserMaster> userOpt = userMasterRepository.findByLoginIdAndEmail(loginId, email);

		if (userOpt.isEmpty()) {
			return Optional.empty();
		}

		UserMaster user = userOpt.get();
		if ("BANNED".equals(user.getStatus())) {
			return Optional.empty();
		}

		return Optional.of(user);
	}

	public boolean resetPassword(Long userId, String newPassword) {
		Optional<UserMaster> userOpt = userMasterRepository.findById(userId);

		if (userOpt.isEmpty()) {
			return false;
		}

		UserMaster user = userOpt.get();
		user.setPassword(passwordEncoder.encode(newPassword));
		userMasterRepository.save(user);
		return true;
	}
}