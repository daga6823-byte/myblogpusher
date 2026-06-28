package com.app.myblogpusher.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.myblogpusher.dto.EnglishDictionaryForm;
import com.app.myblogpusher.entity.EnglishDictionary;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.repository.EnglishDictionaryRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class EnglishDictionaryController {

	private final EnglishDictionaryRepository englishDictionaryRepository;

	public EnglishDictionaryController(EnglishDictionaryRepository englishDictionaryRepository) {
		this.englishDictionaryRepository = englishDictionaryRepository;
	}

	@GetMapping("/english-dictionary/list")
	public String list(Model model) {
		List<EnglishDictionary> dictionaries = englishDictionaryRepository.findAll();
		model.addAttribute("dictionaries", dictionaries);
		return "english_dictionary_list";
	}

	@GetMapping("/english-dictionary/form")
	public String form(@RequestParam(required = false) Long id, Model model) {
		EnglishDictionaryForm form = new EnglishDictionaryForm();

		if (id != null) {
			Optional<EnglishDictionary> existing = englishDictionaryRepository.findById(id);
			if (existing.isPresent()) {
				EnglishDictionary dict = existing.get();
				form.setId(dict.getId());
				form.setJapanese(dict.getJapanese());
				form.setEnglish(dict.getEnglish());
			}
		}

		model.addAttribute("form", form);
		return "english_dictionary_form";
	}

	@PostMapping("/english-dictionary/save")
	public String save(@ModelAttribute("form") EnglishDictionaryForm form, HttpSession session) {
		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		EnglishDictionary dictionary;

		if (form.getId() != null) {
			dictionary = englishDictionaryRepository.findById(form.getId()).orElseThrow();
			dictionary.setEnglish(form.getEnglish());
			dictionary.setUpdateDate(LocalDateTime.now());
			dictionary.setUpdateUser(userId);
		} else {
			dictionary = new EnglishDictionary();
			dictionary.setJapanese(form.getJapanese());
			dictionary.setEnglish(form.getEnglish());
			dictionary.setCreateDate(LocalDateTime.now());
			dictionary.setCreateUser(userId);
			dictionary.setUpdateDate(LocalDateTime.now());
			dictionary.setUpdateUser(userId);
		}

		englishDictionaryRepository.save(dictionary);

		return "redirect:/english-dictionary/list?saved";
	}

	@PostMapping("/english-dictionary/delete")
	public String delete(@RequestParam Long id) {
		englishDictionaryRepository.deleteById(id);
		return "redirect:/english-dictionary/list";
	}
}