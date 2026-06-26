package com.app.myblogpusher.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.repository.UserMasterRepository;
import com.app.myblogpusher.service.TokenCipherService;

@Component
public class CipherKeyInitializer {

	private final UserMasterRepository userMasterRepository;
	private final TokenCipherService tokenCipherService;

	public CipherKeyInitializer(UserMasterRepository userMasterRepository,
			TokenCipherService tokenCipherService) {
		this.userMasterRepository = userMasterRepository;
		this.tokenCipherService = tokenCipherService;
	}

	@EventListener(ApplicationReadyEvent.class)
	public void initializeCipherKeys() {
		var usersWithoutKey = userMasterRepository.findByCipherKeyIsNull();

		for (UserMaster user : usersWithoutKey) {
			user.setCipherKey(tokenCipherService.generateNewKey());
			userMasterRepository.save(user);
		}
	}
}