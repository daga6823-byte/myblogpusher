package com.app.myblogpusher.config;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.repository.UserMasterRepository;

@Component
public class PasswordMigrationRunner implements CommandLineRunner {

    @Autowired
    private UserMasterRepository userMasterRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        List<UserMaster> users = userMasterRepository.findAll();

        for (UserMaster user : users) {
            String pw = user.getPassword();
            if (pw != null && !isAlreadyHashed(pw)) {
                user.setPassword(passwordEncoder.encode(pw));
                userMasterRepository.save(user);
            }
        }
    }

    private boolean isAlreadyHashed(String pw) {
        return pw.startsWith("$2a$") || pw.startsWith("$2b$") || pw.startsWith("$2y$");
    }
}