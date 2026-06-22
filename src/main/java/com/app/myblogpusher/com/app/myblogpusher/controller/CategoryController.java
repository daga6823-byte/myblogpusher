package com.app.myblogpusher.controller;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.app.myblogpusher.dto.CategoryDictionaryView;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.ArticleCategoryService;

import jakarta.servlet.http.HttpSession;

@Controller
public class CategoryController {

    @Autowired
    private ArticleCategoryService articleCategoryService;

    @GetMapping("/category/list")
    public String list(HttpSession session, Model model) {

        UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
        Long userId = loginUser.getUserId();

        List<CategoryDictionaryView> categories = articleCategoryService.findDictionaryView(userId);
        model.addAttribute("categories", categories);

        return "category_list";
    }
    
    @PostMapping("/category/add")
    @ResponseBody
    public Map<String, String> add(@RequestParam String categoryName, HttpSession session) {

        UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
        Long userId = loginUser.getUserId();

        if (categoryName == null || categoryName.isBlank()) {
            return Map.of("result", "error", "message", "カテゴリー名を入力してください");
        }

        if (articleCategoryService.findByUserIdAndName(userId, categoryName).isPresent()) {
            return Map.of("result", "error", "message", "同じ名前のカテゴリーが既に存在します");
        }

        articleCategoryService.insertCategory(userId, categoryName);

        return Map.of("result", "ok");
    }
    
    @PostMapping("/category/rename")
    @ResponseBody
    public Map<String, String> rename(@RequestParam Long categoryId,
                                       @RequestParam String newName,
                                       HttpSession session) {

        UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
        Long userId = loginUser.getUserId();

        if (newName == null || newName.isBlank()) {
            return Map.of("result", "error", "message", "カテゴリー名を入力してください");
        }

        if (articleCategoryService.findByUserIdAndName(userId, newName).isPresent()) {
            return Map.of("result", "error", "message", "同じ名前のカテゴリーが既に存在します");
        }

        articleCategoryService.rename(categoryId, userId, newName);

        return Map.of("result", "ok");
    }

    @PostMapping("/category/delete")
    public String delete(@RequestParam Long categoryId, HttpSession session) {

        UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
        Long userId = loginUser.getUserId();

        articleCategoryService.delete(categoryId, userId);

        return "redirect:/category/list";
    }
}