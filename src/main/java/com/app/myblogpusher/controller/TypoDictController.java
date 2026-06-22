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

import com.app.myblogpusher.dto.TypoDictionaryView;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.TypoCorrectionService;

import jakarta.servlet.http.HttpSession;

@Controller
public class TypoDictController {

	@Autowired
	private TypoCorrectionService typoCorrectionService;

	@GetMapping("/typo-dict/list")
	public String list(HttpSession session, Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		List<TypoDictionaryView> typos = typoCorrectionService.findDictionaryView(userId);
		model.addAttribute("typos", typos);

		List<String> categoryNames = typos.stream()
				.map(TypoDictionaryView::getCategoryName)
				.distinct()
				.sorted()
				.toList();
		model.addAttribute("categoryNames", categoryNames);

		return "typo_dict";
	}

	@PostMapping("/typo-dict/update")
	@ResponseBody
	public Map<String, String> update(@RequestParam Long typoId,
			@RequestParam String wrongWord,
			@RequestParam String correctWord,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		typoCorrectionService.update(typoId, wrongWord, correctWord, userId);

		return Map.of("result", "ok");
	}

	@PostMapping("/typo-dict/delete")
	public String delete(@RequestParam Long typoId, HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		typoCorrectionService.delete(typoId, userId);

		return "redirect:/typo-dict/list";
	}
}