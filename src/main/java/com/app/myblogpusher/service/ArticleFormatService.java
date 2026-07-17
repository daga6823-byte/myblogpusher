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

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import com.app.myblogpusher.util.MarkdownStructureUtil;

@Service
public class ArticleFormatService {

	private static final Pattern FRONT_MATTER_PATTERN = Pattern.compile("^\\+\\+\\+[\\s\\S]*?\\+\\+\\+\\n*");

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

		Matcher matcher = FRONT_MATTER_PATTERN.matcher(content);

		String frontMatter = "";

		String body = content;

		if (matcher.find()) {

			String rawFrontMatter = matcher.group();

			body = content.substring(matcher.end());

			// 本文先頭の余計な空行除去
			body = body.replaceFirst("^(?:[ \\t　]*\\n)+", "");

			// Front Matter末尾は空行1つで統一
			frontMatter = rawFrontMatter.replaceAll("\\n+$", "")
					+ "\n\n";
		}

		String formattedBody = formatBody(body);

		return frontMatter + formattedBody;
	}

	/**
	 * 本文を行単位で処理する
	 */
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