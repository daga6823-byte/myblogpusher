/**
 * 英単語辞典の登録・更新処理を担当するサービス
 *
 * 英単語辞典の新規登録および既存データの更新を担当する。
 * DBへの保存処理は非同期で実行する。
 */
package com.app.myblogpusher.service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.dto.EnglishDictionaryForm;
import com.app.myblogpusher.entity.EnglishDictionary;
import com.app.myblogpusher.repository.EnglishDictionaryRepository;

@Service
public class EnglishDictionaryService {

	private final EnglishDictionaryRepository englishDictionaryRepository;

	public EnglishDictionaryService(
			EnglishDictionaryRepository englishDictionaryRepository) {
		this.englishDictionaryRepository = englishDictionaryRepository;
	}

	/**
	 * 英単語辞典を新規登録または更新する。
	 *
	 * @param form 登録・更新する辞典情報
	 * @param userId 操作ユーザーID
	 * @return 保存処理の完了を通知するFuture
	 */
	@Async
	@Transactional
	public CompletableFuture<Void> saveAsync(
			EnglishDictionaryForm form, Long userId) {

		LocalDateTime now = LocalDateTime.now();
		EnglishDictionary dictionary;

		if (form.getId() != null) {
			dictionary = englishDictionaryRepository
					.findByIdAndUserIdIsNull(form.getId())
					.orElseThrow();

			dictionary.setJapanese(form.getJapanese());
			dictionary.setEnglish(form.getEnglish());
			dictionary.setUpdateDate(now);
			dictionary.setUpdateUser(userId);
		} else {
			dictionary = new EnglishDictionary();
			dictionary.setUserId(null);
			dictionary.setJapanese(form.getJapanese());
			dictionary.setEnglish(form.getEnglish());
			dictionary.setCreateDate(now);
			dictionary.setCreateUser(userId);
			dictionary.setUpdateDate(now);
			dictionary.setUpdateUser(userId);
		}

		englishDictionaryRepository.save(dictionary);

		return CompletableFuture.completedFuture(null);
	}
}