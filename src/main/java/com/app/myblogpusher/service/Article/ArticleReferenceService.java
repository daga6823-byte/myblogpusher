/**
 * 記事参考文献情報の管理を担当するService
 *
 * 参考文献の登録・更新・削除・取得を行う。
 * 参考文献はCategoryRelationのgroupId単位で管理する。
 */

package com.app.myblogpusher.service.Article;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.Article.ArticleReference;
import com.app.myblogpusher.repository.Article.ArticleReferenceRepository;

@Service
public class ArticleReferenceService {

	private final ArticleReferenceRepository articleReferenceRepository;

	public ArticleReferenceService(
			ArticleReferenceRepository articleReferenceRepository) {

		this.articleReferenceRepository = articleReferenceRepository;
	}

	/**
	 * カテゴリー経路に紐付く参考文献一覧を取得する
	 */
	public List<ArticleReference> findByGroup(
			Long userId,
			Long groupId) {

		return articleReferenceRepository
				.findByUserIdAndGroupIdOrderByReferenceNameAsc(
						userId,
						groupId);
	}

	/**
	 * 参考文献を登録する
	 */
	public ArticleReference save(
			Long userId,
			Long groupId,
			String referenceName,
			String url) {

		ArticleReference reference = new ArticleReference();

		reference.setUserId(userId);
		reference.setGroupId(groupId);
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
}