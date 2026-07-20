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

		int firstEnd = normalized.indexOf("\n+++", 3);

		if (firstEnd == -1) {
			return content;
		}

		int bodyStart = firstEnd + 4;

		String frontMatter = normalized.substring(0, bodyStart)
				.replaceAll("\n+$", "");

		String body = normalized.substring(bodyStart)
				.replaceFirst("^\n*", "");

		body = formatBody(body);

		return frontMatter
				+ "\n\n\n"
				+ body;
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

			line = convertBracketHeading(line);

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
	
	/**
	 * 【〇〇】形式をMarkdown見出しへ変換する
	 */
	private String convertBracketHeading(String line) {

		String trimmed = line.trim();

		if (trimmed.matches("^【.+】$")) {
			return "### " + trimmed;
		}

		return line;
	}
}