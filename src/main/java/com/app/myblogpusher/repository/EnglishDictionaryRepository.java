/**
 * 英単語辞典のデータ検索を担当するリポジトリ
 *
 * 日本語から英単語への変換辞書について、
 * 日本語表記・大文字小文字を区別しない検索、
 * 個人辞典と共通辞典の検索を提供する。
 */
package com.app.myblogpusher.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.EnglishDictionary;

public interface EnglishDictionaryRepository extends JpaRepository<EnglishDictionary, Long> {

	/**
	 * 日本語表記が完全一致する辞典データを取得する。
	 *
	 * @param japanese 検索する日本語
	 * @return 一致する辞典データ
	 */
	Optional<EnglishDictionary> findByJapanese(String japanese);

	/**
	 * 大文字小文字を区別せず、日本語表記が一致する辞典データを取得する。
	 *
	 * @param japanese 検索する日本語
	 * @return 一致する辞典データ
	 */
	Optional<EnglishDictionary> findByJapaneseIgnoreCase(String japanese);

	/**
	 * 指定ユーザーが所有する辞典から、日本語表記が完全一致するデータを取得する。
	 *
	 * @param userId 辞典の所有者ID
	 * @param japanese 検索する日本語
	 * @return 一致する辞典データ
	 */
	Optional<EnglishDictionary> findByUserIdAndJapanese(Long userId, String japanese);

	/**
	 * 指定ユーザーが所有する辞典から、大文字小文字を区別せず検索する。
	 *
	 * @param userId 辞典の所有者ID
	 * @param japanese 検索する日本語
	 * @return 一致する辞典データ
	 */
	Optional<EnglishDictionary> findByUserIdAndJapaneseIgnoreCase(Long userId, String japanese);

	/**
	 * 共通辞典から、日本語表記が完全一致するデータを取得する。
	 *
	 * @param japanese 検索する日本語
	 * @return 一致する共通辞典データ
	 */
	Optional<EnglishDictionary> findByUserIdIsNullAndJapanese(String japanese);

	/**
	 * 共通辞典から、大文字小文字を区別せず検索する。
	 *
	 * @param japanese 検索する日本語
	 * @return 一致する共通辞典データ
	 */
	Optional<EnglishDictionary> findByUserIdIsNullAndJapaneseIgnoreCase(String japanese);

	/**
	 * 共通辞典の一覧を取得する。
	 *
	 * @return 共通辞典の一覧
	 */
	List<EnglishDictionary> findByUserIdIsNull();

	/**
	 * IDを指定して共通辞典を取得する。
	 *
	 * @param id 辞典ID
	 * @return 該当する共通辞典
	 */
	Optional<EnglishDictionary> findByIdAndUserIdIsNull(Long id);
}