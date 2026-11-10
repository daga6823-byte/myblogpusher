/**
 * 記事の添削画面と誤字・校正チェックを担当するController
 *
 * 添削画面の表示、誤字辞書への登録、
 * LanguageToolによる校正チェックおよび誤字チェックを処理する。
 */

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
import com.app.myblogpusher.service.HomophoneTypoScanService;
import com.app.myblogpusher.service.LanguageToolService;
import com.app.myblogpusher.service.TypoCorrectionService;
import com.app.myblogpusher.service.Article.ArticleCategoryService;
import com.app.myblogpusher.service.Article.ArticleWorkService;
import com.app.myblogpusher.service.Category.CategorySelectionService;
import com.app.myblogpusher.util.ArticleSaveUtil;

import jakarta.servlet.http.HttpSession;

@Controller
public class ArticleCorrectController {

	@Autowired
	private ArticleCategoryService articleCategoryService;

	@Autowired
	private ArticleWorkService articleWorkService;

	@Autowired
	private TypoCorrectionService typoCorrectionService;

	@Autowired
	private ArticleSaveUtil articleSaveUtil;

	@Autowired
	private CategorySelectionService categorySelectionService;

	private static final String SESSION_KEY_WORK_ID = "ltCachedWorkId";

	//添削画面
	@PostMapping("/article/correct")
	public String correct(@RequestParam(required = false) Long workId,
			@RequestParam(required = false) String categorySelect,
			@RequestParam(required = false) String newCategoryName,
			@RequestParam String title,
			@RequestParam String content,
			HttpSession session,
			Model model) {

		System.out.println("=== ArticleTypoController.correct called ===");

		// LanguageToolのキャッシュをクリア（本文が更新されたため）
		session.removeAttribute("ltTypoResultsCache");
		session.removeAttribute("ltProofResultsCache");
		session.removeAttribute("ltCachedContent");

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		Long savedWorkId = articleSaveUtil.doSaveDraft(
				workId,
				categorySelect,
				newCategoryName,
				null,
				title,
				content,
				userId);

		if (savedWorkId == null) {
			return "redirect:/article/edit";
		}

		ArticleWork work = articleWorkService.findById(savedWorkId, userId);

		Long categoryGroupId = work.getCategoryGroupId();
		String categoryPath = work.getHugoPath();

		// 添削・誤字チェック用カテゴリーIDはArticleWorkから取得する。
		// カテゴリー未設定（汎用）の場合はnullになる。
		Long categoryId = work.getTypoCategoryId();

		List<TypoCorrectionService.TypoMatch> matches = typoCorrectionService.findMatches(categoryId, content);

		model.addAttribute("categories",
				categorySelectionService.findCategorySelects(userId));
		model.addAttribute("work", work);
		model.addAttribute("categoryGroupId", categoryGroupId);
		model.addAttribute("categoryPath", categoryPath);
		model.addAttribute("typoMatches", matches);
		model.addAttribute("categoryId", categoryId);

		// カテゴリー未設定（汎用）の場合はfindByIdを呼ばず、カテゴリー名を空にする。
		String categoryName = "";

		if (categoryId != null) {
			categoryName = articleCategoryService.findById(categoryId)
					.map(ArticleCategory::getCategoryName)
					.orElse("");
		}

		model.addAttribute("categoryName", categoryName);

		return "article/article_correct";
	}

	@PostMapping("/article/typo/add")
	@ResponseBody
	public Map<String, String> addTypo(
			@RequestParam Long workId,
			@RequestParam String wrongWord,
			@RequestParam String correctWord,
			@RequestParam(required = false) Boolean isGeneral,
			HttpSession session) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		Long targetCategoryId = null;

		if (isGeneral == null || !isGeneral) {
			ArticleWork work = articleWorkService.findById(workId, userId);
			targetCategoryId = work.getTypoCategoryId();
		}

		boolean inserted = typoCorrectionService.insertTypo(
				targetCategoryId,
				wrongWord,
				correctWord,
				userId);

		if (!inserted) {
			return Map.of(
					"result", "duplicate",
					"message", "この誤字パターンは既に登録されています");
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
	public List<TypoScanResultView> scanTypos(
			@RequestParam String content,
			@RequestParam Long workId,
			HttpSession session) {

		ensureLanguageToolCache(content, workId, session);

		@SuppressWarnings("unchecked")
		List<TypoScanResultView> cached = (List<TypoScanResultView>) session.getAttribute(SESSION_KEY_TYPO_RESULTS);

		return cached;
	}

	@PostMapping("/article/proofread/scan")
	@ResponseBody
	public List<ProofreadResultView> scanProofreading(
			@RequestParam String content,
			@RequestParam Long workId,
			HttpSession session) {

		ensureLanguageToolCache(content, workId, session);

		@SuppressWarnings("unchecked")
		List<ProofreadResultView> cached = (List<ProofreadResultView>) session.getAttribute(SESSION_KEY_PROOF_RESULTS);

		return cached;
	}

	/**
	 * LanguageToolと誤字スキャンの結果をキャッシュする。
	 *
	 * 本文とWork IDが前回と同じ場合は再解析しない。
	 * 誤字チェック用カテゴリーIDはArticleWorkから取得する。
	 */
	private void ensureLanguageToolCache(
			String content,
			Long workId,
			HttpSession session) {

		String cachedContent = (String) session.getAttribute(SESSION_KEY_LT_CONTENT);
		Long cachedWorkId = (Long) session.getAttribute(SESSION_KEY_WORK_ID);

		// 本文とWorkが前回と同じであれば再解析しない
		if (content.equals(cachedContent)
				&& workId.equals(cachedWorkId)
				&& session.getAttribute(SESSION_KEY_TYPO_RESULTS) != null
				&& session.getAttribute(SESSION_KEY_PROOF_RESULTS) != null) {
			return;
		}

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		Long userId = loginUser.getUserId();

		ArticleWork work = articleWorkService.findById(workId, userId);
		Long categoryId = work.getTypoCategoryId();

		List<LanguageToolService.LanguageToolMatch> allMatches = languageToolService.checkText(content);

		List<LanguageToolService.LanguageToolMatch> typoMatches = homophoneTypoScanService.scan(content);

		List<LanguageToolService.LanguageToolMatch> filteredTypos = typoCorrectionService.excludeKnownTypos(categoryId,
				typoMatches);

		List<TypoScanResultView> typoResults = filteredTypos.stream()
				.map(m -> new TypoScanResultView(
						m.getMatchedText(),
						m.getSuggestion(),
						m.getMessage()))
				.toList();

		List<LanguageToolService.LanguageToolMatch> proofMatches = languageToolService.filterProofreading(allMatches);

		List<ProofreadResultView> proofResults = new ArrayList<>();
		int idx = 0;

		for (LanguageToolService.LanguageToolMatch m : proofMatches) {
			proofResults.add(new ProofreadResultView(
					idx,
					m.getFromPos(),
					m.getToPos(),
					m.getMatchedText(),
					m.getMessage(),
					m.getSuggestion()));
			idx++;
		}

		session.setAttribute(SESSION_KEY_LT_CONTENT, content);
		session.setAttribute(SESSION_KEY_WORK_ID, workId);
		session.setAttribute(SESSION_KEY_TYPO_RESULTS, typoResults);
		session.setAttribute(SESSION_KEY_PROOF_RESULTS, proofResults);
	}
}