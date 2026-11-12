/**
 * 英単語辞典の登録・更新処理を担当するサービス
 *
 * 英単語辞典の新規登録および既存データの更新を担当する。
 * DBへの保存処理は非同期で実行する。
 */
package com.app.myblogpusher.service;

import java.time.LocalDateTime;

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
	 * 英語欄は英字のみを許可し、不正な文字が含まれる場合は登録しない。
	 *
	 * @param form 登録・更新する辞典情報
	 * @param userId 操作ユーザーID
	 */
	@Transactional
	public void save(EnglishDictionaryForm form, Long userId) {

		// 英語欄の入力値を検証する。
		String english = form.getEnglish();

		if (english == null
				|| !english.trim().matches("[A-Za-z]+(?:[ '-][A-Za-z]+)*")) {
			throw new IllegalArgumentException(
					"英語欄には英字のみ入力してください");
		}

		english = english.trim();

		LocalDateTime now = LocalDateTime.now();
		EnglishDictionary dictionary;

		if (form.getId() != null) {
			dictionary = englishDictionaryRepository
					.findByIdAndUserIdIsNull(form.getId())
					.orElseThrow();

			dictionary.setJapanese(form.getJapanese());
			dictionary.setEnglish(english);
			dictionary.setUpdateDate(now);
			dictionary.setUpdateUser(userId);
		} else {
			dictionary = new EnglishDictionary();
			dictionary.setUserId(null);
			dictionary.setJapanese(form.getJapanese());
			dictionary.setEnglish(english);
			dictionary.setCreateDate(now);
			dictionary.setCreateUser(userId);
			dictionary.setUpdateDate(now);
			dictionary.setUpdateUser(userId);
		}

		englishDictionaryRepository.save(dictionary);
	}
}