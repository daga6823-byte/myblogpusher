package com.app.myblogpusher.controller;

import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.LoginService;

import jakarta.servlet.http.HttpSession;

@Controller
public class LoginController {

    @Autowired
    private LoginService loginService;

    @GetMapping("/login")
    public String loginForm() {
        return "login";
    }

    @PostMapping("/login")
    public String login(@RequestParam String loginId,
                         @RequestParam String password,
                         HttpSession session,
                         Model model) {

        Optional<UserMaster> userOpt = loginService.findAuthenticatedUser(loginId, password);

        if (userOpt.isPresent()) {
            session.setAttribute("loginUser", userOpt.get());
            return "redirect:/home";
        } else {
            model.addAttribute("error", "ログインIDまたはパスワードが間違っています");
            return "login";
        }
    }

}