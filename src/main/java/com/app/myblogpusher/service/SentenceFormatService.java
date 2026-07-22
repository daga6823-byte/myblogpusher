/**
 * 通常文章の整形処理を担当するサービス
 *
 * 句点単位で文章を分割し、
 * 読みやすい段落形式へ変換する。
 *
 * Markdown記法やFront Matterは扱わない。
 */

package com.app.myblogpusher.service;

import org.springframework.stereotype.Service;

@Service
public class SentenceFormatService {

	private static final String PARAGRAPH_BREAK_TRIGGER = "さて";
	private static final String SENTENCE_END_CHARS = "。？！";
	private static final String CLOSING_BRACKET_CHARS = "」』";

	/**
	 * 通常文章を句点区切りで整形する
	 *
	 * 「さて」で始まる文は段落区切り、
	 * それ以外は通常改行で繋ぐ。
	 */
	public String formatParagraph(String body) {

		if (body == null || body.isBlank()) {
			return body;
		}

		// 既存インデントは保持する
		String normalized = body;

		StringBuilder result = new StringBuilder();
		StringBuilder currentSentence = new StringBuilder();

		for (int i = 0; i < normalized.length(); i++) {

			char c = normalized.charAt(i);

			currentSentence.append(c);

			// -------------------------------------------------
			// 文末記号検出
			// -------------------------------------------------
			if (SENTENCE_END_CHARS.indexOf(c) >= 0) {

				int j = i + 1;

				// 「！！」「！？」「。。」など連続記号を含める
				while (j < normalized.length()
						&& SENTENCE_END_CHARS.indexOf(normalized.charAt(j)) >= 0) {

					currentSentence.append(normalized.charAt(j));
					j++;
				}

				// 「。」の後ろの閉じ括弧を含める
				while (j < normalized.length()
						&& CLOSING_BRACKET_CHARS.indexOf(normalized.charAt(j)) >= 0) {

					currentSentence.append(normalized.charAt(j));
					j++;
				}

				appendSentence(result, currentSentence.toString());

				currentSentence.setLength(0);

				i = j - 1;
			}
		}

		// -------------------------------------------------
		// 文末記号がない最後の文章
		// -------------------------------------------------
		if (!currentSentence.isEmpty()) {

			appendSentence(result, currentSentence.toString());

		}

		return result.toString();
	}

	/**
	 * 整形済み文章へ1文追加する
	 */
	private void appendSentence(StringBuilder result, String sentence) {

		String trimmed = sentence;

		if (trimmed.trim().isEmpty()) {
			return;
		}

		if (result.length() > 0) {

			if (startsWithTrigger(trimmed)) {

				// 「さて」は段落を分ける
				result.append("\n\n");

			} else {

				// 通常改行
				result.append("\n");

			}
		}

		// 文頭インデント
		result.append(trimmed);
	}

	/**
	 * 段落開始判定
	 */
	private boolean startsWithTrigger(String sentence) {

		return sentence.startsWith(PARAGRAPH_BREAK_TRIGGER);
	}
}