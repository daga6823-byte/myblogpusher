package com.app.myblogpusher.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.app.myblogpusher.entity.MenuMaster;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.MenuMasterService;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    @Autowired
    private MenuMasterService menuMasterService;

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {

        UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

        List<MenuMaster> menus = menuMasterService.findVisibleMenus(loginUser.getRole());
        model.addAttribute("menus", menus);
        model.addAttribute("userName", loginUser.getUserName());
        model.addAttribute("isGuest", loginUser.getRole() == 0);

        return "home";
    }
}