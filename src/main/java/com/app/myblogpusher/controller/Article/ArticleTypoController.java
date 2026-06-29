package com.app.myblogpusher.controller.Article;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.app.myblogpusher.dto.ProofreadResultView;
import com.app.myblogpusher.dto.TypoScanResultView;
import com.app.myblogpusher.entity.ArticleCategory;
import com.app.myblogpusher.entity.ArticleWork;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.ArticleCategoryService;
import com.app.myblogpusher.service.ArticleWorkService;
import com.app.myblogpusher.service.HomophoneTypoScanService;
import com.app.myblogpusher.service.LanguageToolService;
import com.app.myblogpusher.service.TypoCorrectionService;
import com.app.myblogpusher.util.ArticleSaveUtil;

import jakarta.servlet.http.HttpSession;

@Controller
public class ArticleTypoController {

	@Autowired
	private ArticleCategoryService articleCategoryService;

	@Autowired
	private ArticleWorkService articleWorkService;

	@Autowired
	private TypoCorrectionService typoCorrectionService;

	@Autowired
	private ArticleSaveUtil articleSaveUtil;

	//添削画面
	@PostMapping("/article/article/correct")
	public String correct(@RequestParam(required = false) Long workId,
			@RequestParam String categorySelect,
			@RequestParam(required = false) String newCategoryName,
			@RequestParam String title,
			@RequestParam String content,
			HttpSession session,
			Model model) {

		// LanguageToolのキャッシュをクリア（本文が更新されたため）
		session.removeAttribute("ltTypoResultsCache");
		session.removeAttribute("ltProofResultsCache");
		session.removeAttribute("ltCachedContent");

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		Long savedWorkId = articleSaveUtil.doSaveDraft(workId, categorySelect, newCategoryName, title, content, userId);

		if (savedWorkId == null) {
			// タイトル・本文ともに空だったため保存されなかった
			return "redirect:/article/article/edit";
		}

		ArticleWork work = articleWorkService.findById(savedWorkId);
		Long categoryId = work.getCategoryId();

		List<TypoCorrectionService.TypoMatch> matches = typoCorrectionService.findMatches(categoryId, content);

		model.addAttribute("categories", articleCategoryService.findByUserId(userId));
		model.addAttribute("work", work);
		model.addAttribute("typoMatches", matches);

		String categoryName = articleCategoryService.findById(categoryId)
				.map(ArticleCategory::getCategoryName)
				.orElse("");
		model.addAttribute("categoryName", categoryName);

		return "article_correct";
	}

	@PostMapping("/article/article/typo/add")
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

	@Autowired
	private LanguageToolService languageToolService;

	private static final String SESSION_KEY_TYPO_RESULTS = "ltTypoResultsCache";
	private static final String SESSION_KEY_PROOF_RESULTS = "ltProofResultsCache";
	private static final String SESSION_KEY_LT_CONTENT = "ltCachedContent";
	@Autowired
	private HomophoneTypoScanService homophoneTypoScanService;

	@PostMapping("/article/article/typo/scan")
	@ResponseBody
	public List<TypoScanResultView> scanTypos(@RequestParam String content,
			@RequestParam String categorySelect,
			@RequestParam(required = false) String newCategoryName,
			HttpSession session) {

		ensureLanguageToolCache(content, categorySelect, newCategoryName, session);

		@SuppressWarnings("unchecked")
		List<TypoScanResultView> cached = (List<TypoScanResultView>) session.getAttribute(SESSION_KEY_TYPO_RESULTS);
		return cached;
	}

	@PostMapping("/article/article/proofread/scan")
	@ResponseBody
	public List<ProofreadResultView> scanProofreading(@RequestParam String content,
			@RequestParam String categorySelect,
			@RequestParam(required = false) String newCategoryName,
			HttpSession session) {

		ensureLanguageToolCache(content, categorySelect, newCategoryName, session);

		@SuppressWarnings("unchecked")
		List<ProofreadResultView> cached = (List<ProofreadResultView>) session.getAttribute(SESSION_KEY_PROOF_RESULTS);
		return cached;
	}

	private void ensureLanguageToolCache(String content, String categorySelect, String newCategoryName,
			HttpSession session) {

		String cachedContent = (String) session.getAttribute(SESSION_KEY_LT_CONTENT);

		// 本文が前回と同じであれば再解析しない
		if (content.equals(cachedContent)
				&& session.getAttribute(SESSION_KEY_TYPO_RESULTS) != null
				&& session.getAttribute(SESSION_KEY_PROOF_RESULTS) != null) {
			return;
		}

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		String categoryName = "__new__".equals(categorySelect) ? newCategoryName : categorySelect;

		Long categoryId = (categoryName == null || categoryName.isBlank())
				? null
				: articleCategoryService.findByUserIdAndName(userId, categoryName)
						.map(ArticleCategory::getCategoryId)
						.orElse(null);

		List<LanguageToolService.LanguageToolMatch> allMatches = languageToolService.checkText(content);

		List<LanguageToolService.LanguageToolMatch> typoMatches = homophoneTypoScanService.scan(content);
		List<LanguageToolService.LanguageToolMatch> filteredTypos = typoCorrectionService.excludeKnownTypos(categoryId,
				typoMatches);

		List<TypoScanResultView> typoResults = filteredTypos.stream()
				.map(m -> new TypoScanResultView(m.getMatchedText(), m.getSuggestion(), m.getMessage()))
				.toList();

		List<LanguageToolService.LanguageToolMatch> proofMatches = languageToolService.filterProofreading(allMatches);

		List<ProofreadResultView> proofResults = new ArrayList<>();
		int idx = 0;
		for (LanguageToolService.LanguageToolMatch m : proofMatches) {
			proofResults.add(new ProofreadResultView(idx, m.getFromPos(), m.getToPos(), m.getMatchedText(),
					m.getMessage(), m.getSuggestion()));
			idx++;
		}

		session.setAttribute(SESSION_KEY_LT_CONTENT, content);
		session.setAttribute(SESSION_KEY_TYPO_RESULTS, typoResults);
		session.setAttribute(SESSION_KEY_PROOF_RESULTS, proofResults);
	}

}