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

		String normalized = content.replace("\r\n", "\n");

		int end = normalized.indexOf("\n+++\n");

		if (end == -1) {
			return content;
		}

		// Front Matter末尾まで取得
		String frontMatter = normalized.substring(0, end + 5);

		// 本文は一切加工しない
		String body = normalized.substring(end + 5);

		return frontMatter + body;
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

			if (markdownStructureUtil.isCodeBlockStart(line)) {

				inCodeBlock = !inCodeBlock;

				result.append(line)
						.append("\n");

				continue;
			}

			if (inCodeBlock) {

				result.append(line)
						.append("\n");

				continue;
			}

			if (trimmed.isEmpty()) {

				result.append("\n");

				continue;
			}

			if (!markdownStructureUtil.shouldIndent(line)) {

				result.append(line)
						.append("\n");

				continue;
			}

			result.append(
					sentenceFormatService.formatParagraph(line))
					.append("\n");
		}

		while (result.length() > 0
				&& result.charAt(result.length() - 1) == '\n') {

			result.deleteCharAt(result.length() - 1);
		}

		return result.toString();
	}
}