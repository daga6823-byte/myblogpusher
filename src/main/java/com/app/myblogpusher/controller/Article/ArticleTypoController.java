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
import com.app.myblogpusher.dto.Typo.TypoScanResultView;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.Article.ArticleCategory;
import com.app.myblogpusher.entity.Article.ArticleWork;
import com.app.myblogpusher.repository.CategoryRelationRepository;
import com.app.myblogpusher.service.HomophoneTypoScanService;
import com.app.myblogpusher.service.LanguageToolService;
import com.app.myblogpusher.service.TypoCorrectionService;
import com.app.myblogpusher.service.Article.ArticleCategoryService;
import com.app.myblogpusher.service.Article.ArticleWorkService;
import com.app.myblogpusher.service.Category.CategoryPathService;
import com.app.myblogpusher.service.Category.CategorySelectionService;
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

	@Autowired
	private CategoryPathService categoryPathService;

	@Autowired
	private CategorySelectionService categorySelectionService;

	@Autowired
	private CategoryRelationRepository categoryRelationRepository;

	//添削画面
	@PostMapping("/article/correct")
	public String correct(@RequestParam(required = false) Long workId,
			@RequestParam(required = false) String categorySelect,
			@RequestParam(required = false) String newCategoryName,
			@RequestParam String title,
			@RequestParam String content,
			HttpSession session,
			Model model) {

		System.out.println("=== /article/correct received ===");
		System.out.println("workId = " + workId);
		System.out.println("categorySelect = [" + categorySelect + "]");
		System.out.println("newCategoryName = [" + newCategoryName + "]");
		System.out.println("title = [" + title + "]");

		// LanguageToolのキャッシュをクリア（本文が更新されたため）
		session.removeAttribute("ltTypoResultsCache");
		session.removeAttribute("ltProofResultsCache");
		session.removeAttribute("ltCachedContent");

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		System.out.println("=== doSaveDraft start ===");

		Long savedWorkId = articleSaveUtil.doSaveDraft(
				workId, categorySelect, newCategoryName, title, content, userId);

		System.out.println("=== doSaveDraft end ===");
		System.out.println("savedWorkId = " + savedWorkId);

		if (savedWorkId == null) {
			return "redirect:/article/edit";
		}

		System.out.println("=== find work start ===");

		ArticleWork work = articleWorkService.findById(savedWorkId);

		System.out.println("=== find work end ===");
		System.out.println("workId = " + work.getWorkId());
		System.out.println("categoryGroupId = " + work.getCategoryGroupId());

		Long categoryGroupId = work.getCategoryGroupId();

		// 現在選択されているカテゴリー経路を取得する。
		// categoryGroupIdはCategoryRelation.groupIdを指す。
		System.out.println("=== find categoryPath start ===");

		String categoryPath = categorySelectionService
				.findCategoryPathByGroupId(categoryGroupId);

		System.out.println("categoryPath = [" + categoryPath + "]");

		System.out.println("=== find typoCategoryId start ===");

		Long categoryId = categoryPathService
				.findTypoCategoryIdByGroupId(categoryGroupId);

		System.out.println("typo categoryId = " + categoryId);

		System.out.println("=== findMatches start ===");

		List<TypoCorrectionService.TypoMatch> matches = typoCorrectionService.findMatches(categoryId, content);

		System.out.println("=== findMatches end ===");

		model.addAttribute("categories",
				categorySelectionService.findCategorySelects(userId));
		model.addAttribute("work", work);
		model.addAttribute("categoryGroupId", categoryGroupId);
		model.addAttribute("categoryPath", categoryPath);
		model.addAttribute("typoMatches", matches);
		model.addAttribute("categoryId", categoryId);

		String categoryName = articleCategoryService.findById(categoryId)
				.map(ArticleCategory::getCategoryName)
				.orElse("");
		model.addAttribute("categoryName", categoryName);

		return "article/article_correct";
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

			if ("__new__".equals(categorySelect)) {

				targetCategoryId = articleCategoryService.insertCategory(
						userId,
						newCategoryName,
						null,
						newCategoryName);

			} else {

				Long categoryGroupId = categoryRelationRepository
						.findByCategoryPath(categorySelect)
						.stream()
						.findFirst()
						.orElseThrow()
						.getGroupId();

				targetCategoryId = categoryPathService
						.findTypoCategoryIdByGroupId(categoryGroupId);
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

	@PostMapping("/article/typo/scan")
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

	@PostMapping("/article/proofread/scan")
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

		Long categoryId = null;

		if ("__new__".equals(categorySelect)) {

			if (newCategoryName != null && !newCategoryName.isBlank()) {
				categoryId = articleCategoryService.insertCategory(
						userId,
						newCategoryName,
						null,
						newCategoryName);
			}

		} else if (categorySelect != null && !categorySelect.isBlank()) {

			Long categoryGroupId = categoryRelationRepository
					.findByCategoryPath(categorySelect)
					.stream()
					.findFirst()
					.orElseThrow()
					.getGroupId();

			categoryId = categoryPathService
					.findTypoCategoryIdByGroupId(categoryGroupId);
		}

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