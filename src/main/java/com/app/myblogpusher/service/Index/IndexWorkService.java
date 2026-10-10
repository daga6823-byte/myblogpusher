/**
 * インデックス編集中データ(index_work)を管理するサービス
 *
 * ・ユーザーの編集中インデックス取得
 * ・新規編集データの保存
 * ・編集データの更新
 * ・GitHub反映状態の管理
 * ・反映完了後の編集データ削除
 * を担当する。
 *
 * GitHub反映済みのindexテーブルへの登録・更新はIndexEditService側で行う。
 */
package com.app.myblogpusher.service.Index;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.Index.IndexWork;
import com.app.myblogpusher.repository.Index.IndexWorkRepository;

@Service
public class IndexWorkService {

	@Autowired
	private IndexWorkRepository indexWorkRepository;

	/**
	 * ユーザーの編集中インデックス一覧を取得する。
	 */
	public List<IndexWork> findByUserId(Long userId) {
		return indexWorkRepository.findByUserId(userId);
	}

	/**
	 * ユーザーとカテゴリー経路から編集データを取得する。
	 */
	public Optional<IndexWork> findByUserIdAndGroupId(
			Long userId,
			Long groupId) {
		return indexWorkRepository.findByUserIdAndGroupId(userId, groupId);
	}

	/**
	 * 編集内容を新規保存、または既存データへ上書き保存する。
	 */
	public IndexWork saveDraft(
			Long userId,
			Long groupId,
			String title,
			String content) {

		LocalDateTime now = LocalDateTime.now();

		IndexWork work = indexWorkRepository
				.findByUserIdAndGroupId(userId, groupId)
				.orElseGet(() -> {
					IndexWork newWork = new IndexWork();
					newWork.setUserId(userId);
					newWork.setGroupId(groupId);
					newWork.setCreateUser(userId);
					newWork.setCreateDate(now);
					newWork.setStatus(0);
					return newWork;
				});

		work.setTitle(title);
		work.setContent(content);
		work.setUpdateUser(userId);
		work.setUpdateDate(now);
		work.setStatus(0);

		return indexWorkRepository.save(work);
	}

	/**
	 * GitHub反映状態を更新する。
	 *
	 * 0 = 編集中
	 * 1 = GitHub反映中
	 * 2 = エラー
	 */
	public void updateStatus(Long workId, Integer status) {
		IndexWork work = indexWorkRepository.findById(workId)
				.orElseThrow();

		work.setStatus(status);
		work.setUpdateDate(LocalDateTime.now());

		indexWorkRepository.save(work);
	}

	/**
	 * GitHub反映状態を更新する。
	 */
	public void updateStatus(
			Long workId,
			Integer status,
			String errorCode) {

		IndexWork work = indexWorkRepository.findById(workId)
				.orElseThrow();

		work.setStatus(status);
		work.setUpdateDate(LocalDateTime.now());

		indexWorkRepository.save(work);
	}

	/**
	 * GitHub反映成功後に編集データを削除する。
	 */
	public void delete(Long workId, Long userId) {
		IndexWork work = indexWorkRepository.findById(workId)
				.orElseThrow();

		if (!work.getUserId().equals(userId)) {
			throw new IllegalStateException(
					"他のユーザーのインデックスは削除できません");
		}

		indexWorkRepository.delete(work);
	}
}