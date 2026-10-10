/**
 * Hugoカテゴリーのインデックス管理画面を担当するコントローラー
 *
 * カテゴリー階層の選択、_index.mdの読み込み、
 * 編集内容のGitHub保存を管理する。
 */
package com.app.myblogpusher.controller;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.app.myblogpusher.entity.CategoryRelation;
import com.app.myblogpusher.entity.Index;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.repository.CategoryRelationRepository;
import com.app.myblogpusher.repository.IndexRepository;
import com.app.myblogpusher.repository.UserRepositoryRepository;
import com.app.myblogpusher.service.IndexEditService;
import com.app.myblogpusher.service.Github.GitHubPushService;

import jakarta.servlet.http.HttpSession;

@Controller
public class IndexEditController {

	@Autowired
	private UserRepositoryRepository userRepositoryRepository;

	@Autowired
	private CategoryRelationRepository categoryRelationRepository;

	@Autowired
	private IndexEditService hugoIndexService;

	@Autowired
	private IndexRepository indexRepository;

	@Autowired
	private GitHubPushService gitHubPushService;

	/**
	 * インデックス一覧画面を表示する。
	 */
	@GetMapping("/index/list")
	public String listIndexes(
			HttpSession session,
			Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login";
		}

		List<Index> indexes = indexRepository.findByUserId(loginUser.getUserId());

		model.addAttribute("indexes", indexes);
		model.addAttribute(
				"categoryPaths",
				hugoIndexService.findCategoryPaths(loginUser.getUserId()));

		return "index/index_list";
	}

	/**
	 * インデックス管理画面を表示する。
	 */
	@GetMapping("/index/edit")
	public String showIndexManager(
			HttpSession session,
			Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login";
		}

		model.addAttribute(
				"categoryPaths",
				hugoIndexService.findCategoryPaths(loginUser.getUserId()));

		return "index/index_edit";
	}

	/**
	 * 選択されたカテゴリー階層の_index.mdを読み込む。
	 */
	@PostMapping("/index/edit/load")
	public String loadIndex(
			@RequestParam Long groupId,
			HttpSession session,
			Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login";
		}

		Optional<UserRepositoryEntity> repoOpt = userRepositoryRepository.findByUserId(
				loginUser.getUserId());

		if (repoOpt.isEmpty()) {
			model.addAttribute("error", "リポジトリが設定されていません。");
			return showIndexManager(session, model);
		}

		try {
			// ログインユーザーが編集できるカテゴリー階層か検証する。
			boolean ownsCategoryPath = hugoIndexService
					.findCategoryPaths(loginUser.getUserId())
					.stream()
					.anyMatch(category -> category.getGroupId().equals(groupId));

			if (!ownsCategoryPath) {
				throw new IllegalArgumentException(
						"指定されたカテゴリー階層が見つからないか、編集権限がありません。");
			}

			// 同期後はindexテーブルから編集内容を取得する。
			com.app.myblogpusher.entity.Index index = indexRepository.findByUserIdAndGroupId(
					loginUser.getUserId(),
					groupId)
					.orElse(null);

			String content;

			if (index != null) {
				content = index.getContent();
			} else {
				// GitHubに_index.mdが存在しない場合は新規作成用の初期内容を表示する。
				CategoryRelation relation = categoryRelationRepository
						.findByGroupId(groupId)
						.stream()
						.findFirst()
						.orElseThrow(() -> new IllegalArgumentException(
								"カテゴリー階層が見つかりません。"));

				String categoryPath = relation.getCategoryPath();
				String[] pathParts = categoryPath.split("/");
				content = hugoIndexService.createDefaultContent(
						pathParts[pathParts.length - 1]);
			}

			model.addAttribute("selectedGroupId", groupId);
			model.addAttribute("content", content);

		} catch (IllegalArgumentException e) {
			model.addAttribute(
					"error",
					"インデックスファイルを読み込めませんでした: "
							+ e.getMessage());
		}

		model.addAttribute(
				"categoryPaths",
				hugoIndexService.findCategoryPaths(loginUser.getUserId()));

		return "index/index_edit";
	}

	/**
	 * 編集した_index.mdをGitHubへ保存する。
	 */
	@PostMapping("/index/edit/save")
	public String saveIndex(
			@RequestParam Long groupId,
			@RequestParam String content,
			HttpSession session,
			RedirectAttributes redirectAttributes) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login";
		}

		Optional<UserRepositoryEntity> repoOpt = userRepositoryRepository.findByUserId(
				loginUser.getUserId());

		if (repoOpt.isEmpty()) {
			redirectAttributes.addFlashAttribute(
					"error",
					"リポジトリが設定されていません。");
			return "redirect:/index/list";
		}

		try {
			// ログインユーザーが所有するカテゴリー階層か検証する。
			boolean ownsCategoryPath = hugoIndexService
					.findCategoryPaths(loginUser.getUserId())
					.stream()
					.anyMatch(category -> category.getGroupId().equals(groupId));

			if (!ownsCategoryPath) {
				throw new IllegalArgumentException(
						"指定されたカテゴリー階層を編集する権限がありません。");
			}

			// 選択されたカテゴリー階層の経路を取得する。
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

			// content配下の_index.mdだけを更新する。
			gitHubPushService.pushMarkdownFile(
					repoOpt.get(),
					loginUser.getCipherKey(),
					categoryPath + "/_index.md",
					content,
					"Update category index: " + categoryPath);

			// GitHubへの保存成功後、DBにも編集内容を反映する。
			Index index = indexRepository
					.findByUserIdAndGroupId(loginUser.getUserId(), groupId)
					.orElseGet(() -> {
						Index newIndex = new Index();
						newIndex.setUserId(loginUser.getUserId());
						newIndex.setGroupId(groupId);
						newIndex.setCreateUser(loginUser.getUserId());
						newIndex.setCreateDate(LocalDateTime.now());
						return newIndex;
					});

			index.setTitle(extractFrontMatterValue(content, "title"));
			index.setDescription(extractFrontMatterValue(content, "description"));
			index.setContent(content);
			index.setUpdateUser(loginUser.getUserId());
			index.setUpdateDate(LocalDateTime.now());

			indexRepository.save(index);

			redirectAttributes.addFlashAttribute(
					"message",
					"インデックスを保存しました。");
			redirectAttributes.addFlashAttribute(
					"selectedGroupId",
					groupId);

		} catch (IOException | GitAPIException | IllegalArgumentException e) {
			redirectAttributes.addFlashAttribute(
					"error",
					"インデックスの保存に失敗しました: " + e.getMessage());
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
}
