/**
 * 記事本文の整形処理を担当するサービス
 *
 * Front Matterを保持したまま、
 * 本文のみをMarkdown構造を壊さず整形する。
 *
 * 通常文章の整形はSentenceFormatService、
 * Markdown判定はMarkdownStructureUtilへ委譲する。
 */

package com.app.myblogpusher.service;

import org.springframework.stereotype.Service;

import com.app.myblogpusher.util.MarkdownStructureUtil;

@Service
public class ArticleFormatService {

	private final MarkdownStructureUtil markdownStructureUtil;

	private final SentenceFormatService sentenceFormatService;

	public ArticleFormatService(
			MarkdownStructureUtil markdownStructureUtil,
			SentenceFormatService sentenceFormatService) {

		this.markdownStructureUtil = markdownStructureUtil;
		this.sentenceFormatService = sentenceFormatService;
	}

	/**
	 * Front Matterを保持したまま本文を整形する
	 */
	public String formatContent(String content) {

		if (content == null || content.isBlank()) {
			return content;
		}

		if (!content.startsWith("+++")) {
			return content;
		}

		int end = content.indexOf("\n+++\n");

		if (end == -1) {
			return content;
		}

		String frontMatter = content.substring(0, end + 5)
				.replaceAll("\\n+$", "");

		String body = content.substring(end + 5);

		// Front Matterの後ろは空行1つだけ
		body = body.replaceFirst("^\\n*", "");

		return frontMatter + "\n\n" + body;
	}

	/**
	 * 本文を行単位で処理する
	 */
	private String formatBody(String body) {

		String[] lines = body.replace("\r\n", "\n")
				.split("\n", -1);

		StringBuilder result = new StringBuilder();

		boolean inCodeBlock = false;

		for (String line : lines) {

			String trimmed = line.trim();

			// -------------------------------------------------
			// コードブロック開始・終了
			// -------------------------------------------------
			if (markdownStructureUtil.isCodeBlockStart(line)) {

				inCodeBlock = !inCodeBlock;

				result.append(line)
						.append("\n");

				continue;
			}

			// -------------------------------------------------
			// コードブロック内はそのまま
			// -------------------------------------------------
			if (inCodeBlock) {

				result.append(line)
						.append("\n");

				continue;
			}

			// -------------------------------------------------
			// 空行保持
			// -------------------------------------------------
			if (trimmed.isEmpty()) {

				result.append("\n");

				continue;
			}

			// -------------------------------------------------
			// Markdown構造行は変更しない
			// -------------------------------------------------
			if (!markdownStructureUtil.shouldIndent(line)) {

				result.append(line)
						.append("\n");

				continue;
			}

			// -------------------------------------------------
			// 通常文章のみ整形（先頭・末尾の空白は保持）
			// -------------------------------------------------
			result.append(
					sentenceFormatService.formatParagraph(line))
					.append("\n");

		}

		// 末尾の不要な改行削除
		while (result.length() > 0
				&& result.charAt(result.length() - 1) == '\n') {

			result.deleteCharAt(result.length() - 1);
		}

		return result.toString();
	}
}