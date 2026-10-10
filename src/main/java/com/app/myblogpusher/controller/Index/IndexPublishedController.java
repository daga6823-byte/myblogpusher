
/**
 * Hugoカテゴリーのインデックス投稿画面を担当するコントローラー
 *
 * 投稿前確認画面の表示と、確定したインデックスの投稿処理を担当する。
 * 投稿処理そのものはIndexPublishServiceへ委譲する。
 */
package com.app.myblogpusher.controller.Index;

import java.io.IOException;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.app.myblogpusher.entity.CategoryRelation;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.entity.Index.IndexWork;
import com.app.myblogpusher.repository.CategoryRelationRepository;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.repository.Index.IndexRepository;
import com.app.myblogpusher.service.Index.IndexEditService;
import com.app.myblogpusher.service.Index.IndexPublishService;
import com.app.myblogpusher.service.Index.IndexWorkService;

import jakarta.servlet.http.HttpSession;

@Controller
public class IndexPublishedController {

	@Autowired
	private IndexEditService indexEditService;

	@Autowired
	private IndexWorkService indexWorkService;

	@Autowired
	private UserRepositoryRepository userRepositoryRepository;

	@Autowired
	private CategoryRelationRepository categoryRelationRepository;

	@Autowired
	private IndexPublishService indexPublishService;

	@Autowired
	private IndexRepository indexRepository;

	/**
	 * 投稿前確認画面を表示する。
	 */
	@PostMapping("/index/publish/preview")
	public String showPublishPreview(
			@RequestParam Long groupId,
			@RequestParam String content,
			HttpSession session,
			Model model,
			RedirectAttributes redirectAttributes) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login";
		}

		Long userId = loginUser.getUserId();

		try {
			// 編集対象のカテゴリー階層がユーザー所有か確認する。
			boolean ownsCategoryPath = indexEditService
					.findCategoryPaths(userId)
					.stream()
					.anyMatch(category -> category.getGroupId().equals(groupId));

			if (!ownsCategoryPath) {
				throw new IllegalArgumentException(
						"指定されたカテゴリー階層を編集する権限がありません。");
			}

			// 投稿用データを保存し、確認画面で使用する。
			IndexWork work = indexWorkService.saveDraft(
					userId,
					groupId,
					extractFrontMatterValue(content, "title"),
					content);

			UserRepositoryEntity repository = userRepositoryRepository
					.findByUserId(userId)
					.orElseThrow(() -> new IllegalArgumentException(
							"リポジトリが設定されていません。"));

			CategoryRelation relation = categoryRelationRepository
					.findByGroupId(groupId)
					.stream()
					.findFirst()
					.orElseThrow(() -> new IllegalArgumentException(
							"カテゴリー階層が見つかりません。"));

			String categoryPath = relation.getCategoryPath();

			if (categoryPath == null || categoryPath.isBlank()) {
				throw new IllegalArgumentException(
						"カテゴリー経路が設定されていません。");
			}

			model.addAttribute("groupId", groupId);
			model.addAttribute("title", work.getTitle());
			model.addAttribute("content", work.getContent());
			model.addAttribute("categoryPath", categoryPath);
			model.addAttribute("repoOwner", repository.getRepoOwner());
			model.addAttribute("repoName", repository.getRepoName());

			return "index/index_publish_preview";

		} catch (IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute(
					"error",
					"投稿前確認画面を表示できませんでした: " + e.getMessage());

			return "redirect:/index/list";
		}
	}

	/**
	 * 投稿前確認画面で確定したインデックスをGitHubへ反映する。
	 */
	@PostMapping("/index/publish/execute")
	public String publishIndex(
			@RequestParam Long groupId,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login";
		}

		try {
			indexPublishService.publish(
					loginUser.getUserId(),
					loginUser.getCipherKey(),
					groupId);

			redirectAttributes.addFlashAttribute(
					"message", "インデックスをGitHubへ反映しました。");

		} catch (IOException | GitAPIException | RuntimeException e) {
			redirectAttributes.addFlashAttribute(
					"error", "インデックスの反映に失敗しました: " + e.getMessage());
		}

		return "redirect:/index/list";
	}

	/**
	 * Markdownのフロントマターから指定キーの値を取得する。
	 */
	private String extractFrontMatterValue(String markdown, String key) {
		if (markdown == null || markdown.isBlank()) {
			return null;
		}

		java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
				"(?m)^" + java.util.regex.Pattern.quote(key)
						+ "\\s*:\\s*[\"']?(.*?)[\"']?\\s*$");

		java.util.regex.Matcher matcher = pattern.matcher(markdown);
		return matcher.find() ? matcher.group(1).trim() : null;
	}

	/**
	 * 公開済みインデックス一覧を表示する。
	 */
	@GetMapping("/index/published")
	public String publishedList(
			HttpSession session,
			Model model) {

		long start = System.currentTimeMillis();

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login";
		}

		Long userId = loginUser.getUserId();

		model.addAttribute(
				"indexes",
				indexRepository.findByUserIdOrderByUpdateDateDesc(userId));

		model.addAttribute(
				"categoryPaths",
				indexEditService.findCategoryPaths(userId));

		System.out.println(
				"公開済みインデックス一覧の処理時間: "
						+ (System.currentTimeMillis() - start) + "ms");

		return "index/index_published_list";
	}

	/**
	 * 公開済みインデックスを編集用データに読み込み、編集画面を開く。
	 */
	@GetMapping("/index/published/edit")
	public String editPublished(
			@RequestParam Long groupId,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login";
		}

		Long userId = loginUser.getUserId();

		// 指定されたカテゴリー階層がユーザー所有か確認する。
		boolean ownsCategoryPath = indexEditService
				.findCategoryPaths(userId)
				.stream()
				.anyMatch(category -> category.getGroupId().equals(groupId));

		if (!ownsCategoryPath) {
			redirectAttributes.addFlashAttribute(
					"error", "指定されたカテゴリー階層が見つかりません。");
			return "redirect:/index/published";
		}

		// 公開済みのマスターデータを取得する。
		com.app.myblogpusher.entity.Index.Index index = indexRepository.findByUserIdAndGroupId(userId, groupId)
				.orElse(null);

		if (index == null) {
			redirectAttributes.addFlashAttribute(
					"error", "公開済みインデックスが見つかりません。");
			return "redirect:/index/published";
		}

		// 既存の編集中データがあれば再利用し、なければ公開済み内容から作成する。
		indexWorkService.findByUserIdAndGroupId(userId, groupId)
				.orElseGet(() -> indexWorkService.saveDraft(
						userId,
						groupId,
						index.getTitle(),
						index.getContent()));

		return "redirect:/index/edit?groupId=" + groupId;
	}
}
