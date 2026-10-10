/**
 * Hugoカテゴリーのインデックス管理を担当するサービス
 *
 * ユーザーが管理できるカテゴリー階層の一覧取得と、
 * 新規_index.mdの初期内容生成を担当する。
 * GitHubへの保存・プッシュ処理はGitHubPushServiceへ委譲する。
 */
package com.app.myblogpusher.service.Index;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.Category.CategoryOptionView;
import com.app.myblogpusher.entity.CategoryRelation;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.entity.Article.ArticleCategory;
import com.app.myblogpusher.repository.CategoryRelationRepository;
import com.app.myblogpusher.service.Article.ArticleCategoryService;
import com.app.myblogpusher.service.Facade.GitHubFacadeService;

@Service
public class IndexEditService {

	@Autowired
	private ArticleCategoryService articleCategoryService;

	@Autowired
	private CategoryRelationRepository categoryRelationRepository;
	
	@Autowired
	private GitHubFacadeService gitHubFacadeService;

	/**
	 * ユーザーが管理できるカテゴリー階層を一覧取得する。
	 *
	 * 中間階層も含め、CategoryRelation.groupIdを選択値として返す。
	 *
	 * @param userId ログインユーザーID
	 * @return カテゴリー経路の一覧
	 */
	public List<CategoryOptionView> findCategoryPaths(Long userId) {
		List<ArticleCategory> categories = articleCategoryService.findByUserId(userId);

		if (categories.isEmpty()) {
			return List.of();
		}

		List<Long> categoryIds = categories.stream()
				.map(ArticleCategory::getCategoryId)
				.toList();

		List<CategoryRelation> relations = categoryRelationRepository.findAll();

		List<CategoryOptionView> result = new ArrayList<>();

		relations.stream()
				.filter(relation -> relation.getCategoryPath() != null
						&& !relation.getCategoryPath().isBlank())
				.filter(relation -> categoryIds.contains(
						relation.getCategoryId()))
				.sorted(Comparator.comparing(
						CategoryRelation::getCategoryPath,
						String.CASE_INSENSITIVE_ORDER))
				.forEach(relation -> result.add(
						new CategoryOptionView(
								relation.getGroupId(),
								relation.getCategoryId(),
								relation.getCategoryPath())));

		return result;
	}

	/**
	 * 新規インデックスファイルの初期Markdownを生成する。
	 *
	 * @param title カテゴリーの表示名
	 * @return 初期状態の_index.md本文
	 */
	public String createDefaultContent(String title) {

		return "---\n"
				+ "title: \"" + title + "\"\n"
				+ "description: \"\"\n"
				+ "---\n\n";
	}

	/**
	 * 指定されたカテゴリー階層の_index.mdを読み込む。
	 *
	 * ファイルが存在しない場合は、カテゴリー経路の末尾を
	 * タイトルにした新規作成用の初期内容を返す。
	 *
	 * @param groupId カテゴリー経路のグループID
	 * @param repo GitHubリポジトリ情報
	 * @param cipherKey アクセストークンの復号キー
	 * @return インデックスファイルの内容
	 */
	public String loadIndexContent(
			Long groupId,
			UserRepositoryEntity repo,
			String cipherKey) throws IOException, GitAPIException {

		if (groupId == null) {
			throw new IllegalArgumentException(
					"カテゴリー階層が選択されていません。");
		}

		CategoryRelation relation = categoryRelationRepository
				.findByGroupId(groupId)
				.stream()
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException(
						"指定されたカテゴリー階層が見つかりません。"));

		String categoryPath = relation.getCategoryPath();

		if (categoryPath == null || categoryPath.isBlank()) {
			throw new IllegalArgumentException(
					"カテゴリー経路が設定されていません。");
		}

		String relativePath = categoryPath + "/_index.md";

		String content = gitHubFacadeService.readMarkdownFile(
				repo,
				cipherKey,
				relativePath);

		if (content != null) {
			return content;
		}

		// 新規ファイルではカテゴリー経路の末尾を初期タイトルにする。
		String[] pathParts = categoryPath.split("/");
		String title = pathParts[pathParts.length - 1];

		return createDefaultContent(title);
	}
}
