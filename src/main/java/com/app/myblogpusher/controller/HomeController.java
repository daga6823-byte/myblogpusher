package com.app.myblogpusher.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.app.myblogpusher.entity.MenuMaster;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.HomeService;

import jakarta.servlet.http.HttpSession;

@Controller
public class HomeController {

    @Autowired
    private HomeService menuMasterService;

    @GetMapping("/home")
    public String home(HttpSession session, Model model) {

        UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

        long start = System.nanoTime();

        List<MenuMaster> menus = menuMasterService.findVisibleMenus(loginUser.getRole());

        System.out.println(
                "[HomeTiming] メニュー取得: "
                + (System.nanoTime() - start) / 1_000_000
                + " ms");

        model.addAttribute("menus", menus);
        model.addAttribute("userName", loginUser.getUserName());
        model.addAttribute("isGuest", loginUser.getRole() == 0);

        return "home";
    }
}