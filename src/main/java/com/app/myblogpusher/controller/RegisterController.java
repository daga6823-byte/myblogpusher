package com.app.myblogpusher.controller;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.repository.UserMasterRepository;

@Controller
public class RegisterController {

    @Autowired
    private UserMasterRepository userMasterRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @GetMapping("/register")
    public String registerForm() {
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String loginId,
                            @RequestParam String password,
                            @RequestParam String userName,
                            @RequestParam(required = false) String email,
                            Model model) {

        if (userMasterRepository.findByLoginId(loginId).isPresent()) {
            model.addAttribute("error", "そのログインIDは既に使用されています");
            return "register";
        }

        UserMaster user = new UserMaster();
        user.setLoginId(loginId);
        user.setPassword(passwordEncoder.encode(password));
        user.setUserName(userName);
        user.setEmail(email);
        user.setRole(0);
        user.setStatus("PENDING");
        user.setCreateDate(LocalDateTime.now());
        user.setUpdateDate(LocalDateTime.now());

        userMasterRepository.save(user);
        model.addAttribute("message", "登録が完了しました。管理者の承認をお待ちください。");
        return "register_done";
    }
}