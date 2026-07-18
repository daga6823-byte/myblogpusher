/**
 * 記事参考文献情報の管理を担当するService
 *
 * 参考文献の登録・更新・削除・取得を行う。
 * また、記事カテゴリーから参考文献を紐付ける
 * ルート直下カテゴリーを判定する。
 */

package com.app.myblogpusher.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.ArticleCategory;
import com.app.myblogpusher.entity.ArticleReference;
import com.app.myblogpusher.repository.ArticleReferenceRepository;

@Service
public class ArticleReferenceService {

	private final ArticleReferenceRepository articleReferenceRepository;

	private final ArticleCategoryService articleCategoryService;

	public ArticleReferenceService(
			ArticleReferenceRepository articleReferenceRepository,
			ArticleCategoryService articleCategoryService) {

		this.articleReferenceRepository = articleReferenceRepository;
		this.articleCategoryService = articleCategoryService;
	}

	/**
	 * カテゴリーに紐付く参考文献一覧を取得する
	 */
	public List<ArticleReference> findByCategory(
			Long userId,
			Long categoryId) {

		return articleReferenceRepository
				.findByUserIdAndCategoryIdOrderByReferenceNameAsc(
						userId,
						categoryId);
	}

	/**
	 * 参考文献を登録する
	 */
	public ArticleReference save(
			Long userId,
			Long categoryId,
			String referenceName,
			String url) {

		ArticleReference reference = new ArticleReference();

		reference.setUserId(userId);
		reference.setCategoryId(categoryId);
		reference.setReferenceName(referenceName);
		reference.setUrl(url);
		reference.setCreateDate(LocalDateTime.now());
		reference.setUpdateDate(LocalDateTime.now());

		return articleReferenceRepository.save(reference);
	}

	/**
	 * 参考文献を削除する
	 */
	public void delete(Long referenceId) {

		articleReferenceRepository.deleteById(referenceId);
	}

	/**
	 * 記事カテゴリーから参考文献登録対象カテゴリーを取得する
	 *
	 * 例:
	 * movie/batman/gadget
	 *
	 * の場合
	 *
	 * batman
	 *
	 * を返す
	 */
	public Long resolveReferenceCategoryId(
			Long categoryId) {

		ArticleCategory current = articleCategoryService.findById(categoryId)
				.orElseThrow();

		ArticleCategory child = current;

		while (child.getParentCategoryId() != null) {

			ArticleCategory parent = articleCategoryService.findById(
					child.getParentCategoryId())
					.orElseThrow();

			if (parent.getParentCategoryId() == null) {
				return child.getCategoryId();
			}

			child = parent;
		}

		return current.getCategoryId();
	}

	/**
	 * 参考文献登録対象カテゴリー名を取得する
	 */
	public String getReferenceCategoryName(Long categoryId) {

		Long referenceCategoryId = resolveReferenceCategoryId(categoryId);

		return articleCategoryService.findById(referenceCategoryId)
				.orElseThrow()
				.getCategoryName();
	}
}