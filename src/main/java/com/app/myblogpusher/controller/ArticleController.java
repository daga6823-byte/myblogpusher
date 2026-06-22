package com.app.myblogpusher.controller;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.app.myblogpusher.dto.ArticleWorkView;
import com.app.myblogpusher.entity.ArticleCategory;
import com.app.myblogpusher.entity.ArticleWork;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.ArticleCategoryService;
import com.app.myblogpusher.service.ArticleWorkService;
import com.app.myblogpusher.service.TypoCorrectionService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ArticleController {

	@Autowired
	private ArticleCategoryService articleCategoryService;

	@Autowired
	private ArticleWorkService articleWorkService;

	@Autowired
	private TypoCorrectionService typoCorrectionService;

	@GetMapping("/article/edit")
	public String editForm(@RequestParam(required = false) Long workId,
			@RequestParam(required = false) Boolean saved,
			HttpSession session,
			Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		List<ArticleCategory> categories = articleCategoryService.findByUserId(userId);
		model.addAttribute("categories", categories);

		if (workId != null) {
			ArticleWork work = articleWorkService.findById(workId);
			model.addAttribute("work", work);

			String categoryName = articleCategoryService.findById(work.getCategoryId())
					.map(ArticleCategory::getCategoryName)
					.orElse("");
			model.addAttribute("categoryName", categoryName);
		}

		model.addAttribute("saved", saved != null && saved);

		return "article_edit";
	}

	@PostMapping("/article/save")
	public String saveDraft(@RequestParam(required = false) Long workId,
	                         @RequestParam String categorySelect,
	                         @RequestParam(required = false) String newCategoryName,
	                         @RequestParam String title,
	                         @RequestParam String content,
	                         @RequestParam(required = false) String redirectTo,
	                         HttpSession session) {

	    Long savedWorkId = doSaveDraft(workId, categorySelect, newCategoryName, title, content, session);

	    if (savedWorkId == null) {
	        // タイトル・本文ともに空だったため保存されなかった
	        return "redirect:/article/edit";
	    }

	    if ("home".equals(redirectTo)) {
	        return "redirect:/home";
	    }
	    if ("list".equals(redirectTo)) {
	        return "redirect:/article/list";
	    }

	    return "redirect:/article/edit?workId=" + savedWorkId + "&saved=true";
	}

	//下書き一覧画面
	@GetMapping("/article/list")
	public String list(HttpSession session, Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		List<ArticleWork> works = articleWorkService.findByUserId(userId);

		List<ArticleWorkView> workViews = works.stream()
				.map(work -> {
					String categoryName = articleCategoryService.findById(work.getCategoryId())
							.map(ArticleCategory::getCategoryName)
							.orElse("（未分類）");
					return new ArticleWorkView(work.getWorkId(), work.getTitle(), categoryName, work.getUpdateDate());
				})
				.toList();

		model.addAttribute("works", workViews);

		return "article_list";
	}

	//添削画面
	@PostMapping("/article/correct")
	public String correct(@RequestParam(required = false) Long workId,
	                       @RequestParam String categorySelect,
	                       @RequestParam(required = false) String newCategoryName,
	                       @RequestParam String title,
	                       @RequestParam String content,
	                       HttpSession session,
	                       Model model) {

	    Long savedWorkId = doSaveDraft(workId, categorySelect, newCategoryName, title, content, session);

	    if (savedWorkId == null) {
	        // タイトル・本文ともに空だったため保存されなかった
	        return "redirect:/article/edit";
	    }

	    ArticleWork work = articleWorkService.findById(savedWorkId);
	    Long categoryId = work.getCategoryId();

	    List<TypoCorrectionService.TypoMatch> matches =
	        typoCorrectionService.findMatches(categoryId, content);

	    UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
	    Long userId = loginUser.getUserId();
	    model.addAttribute("categories", articleCategoryService.findByUserId(userId));
	    model.addAttribute("work", work);
	    model.addAttribute("typoMatches", matches);

	    String categoryName = articleCategoryService.findById(categoryId)
	        .map(ArticleCategory::getCategoryName)
	        .orElse("");
	    model.addAttribute("categoryName", categoryName);

	    return "article_correct";
	}

	@PostMapping("/article/typo/add")
	@ResponseBody
	public Map<String, String> addTypo(@RequestParam String wrongWord,
	                                    @RequestParam String correctWord,
	                                    @RequestParam String categorySelect,
	                                    @RequestParam(required = false) String newCategoryName,
	                                    @RequestParam(required = false) Boolean isGeneral,
	                                    HttpSession session) {

	    UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
	    Long userId = loginUser.getUserId();

	    Long targetCategoryId = null;

	    if (isGeneral == null || !isGeneral) {
	        String categoryName = "__new__".equals(categorySelect) ? newCategoryName : categorySelect;

	        if (categoryName != null && !categoryName.isBlank()) {
	            targetCategoryId = articleCategoryService.findByUserIdAndName(userId, categoryName)
	                .map(ArticleCategory::getCategoryId)
	                .orElseGet(() -> articleCategoryService.insertCategory(userId, categoryName));
	        }
	    }

	    boolean inserted = typoCorrectionService.insertTypo(targetCategoryId, wrongWord, correctWord, userId);

	    if (!inserted) {
	        return Map.of("result", "duplicate", "message", "この誤字パターンは既に登録されています");
	    }

	    return Map.of("result", "ok");

	}

	private Long doSaveDraft(Long workId, String categorySelect, String newCategoryName,
            String title, String content, HttpSession session) {

UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
Long userId = loginUser.getUserId();

String categoryName = "__new__".equals(categorySelect) ? newCategoryName : categorySelect;

Long categoryId = articleCategoryService.findByUserIdAndName(userId, categoryName)
.map(ArticleCategory::getCategoryId)
.orElseGet(() -> articleCategoryService.insertCategory(userId, categoryName));

if (workId == null) {

// 空のまま保存させない
if ((title == null || title.isBlank()) && (content == null || content.isBlank())) {
return null; // 呼び出し元で null チェックが必要
}

// 完全一致の重複チェック
Optional<ArticleWork> existing = articleWorkService.findDuplicate(userId, categoryId, title, content);
if (existing.isPresent()) {
return existing.get().getWorkId();
}

return articleWorkService.insertArticleWork(userId, categoryId, title, content);
} else {
articleWorkService.updateArticleWork(workId, categoryId, title, content, userId);
return workId;
}
}
	
	@PostMapping("/article/delete")
	public String delete(@RequestParam Long workId, HttpSession session) {

	    UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
	    Long userId = loginUser.getUserId();

	    articleWorkService.delete(workId, userId);

	    return "redirect:/article/list";
	}
}