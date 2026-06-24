package com.app.myblogpusher.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

@Service
public class ArticleFormatService {

	private static final Pattern FRONT_MATTER_PATTERN = Pattern.compile("^\\+\\+\\+[\\s\\S]*?\\+\\+\\+\\n*");
	private static final String PARAGRAPH_BREAK_TRIGGER = "さて";
	private static final String SENTENCE_END_CHARS = "。？！";

	/**
	 * フロントマターを保持したまま、本文部分のみを句点区切りで整形する。
	 * 「さて」で始まる文の前は空行（段落区切り）、それ以外は単一改行とする。
	 */
	public String formatContent(String content) {
		if (content == null || content.isBlank()) {
			return content;
		}

		Matcher matcher = FRONT_MATTER_PATTERN.matcher(content);
		String frontMatter = "";
		String body = content;

		if (matcher.find()) {
			frontMatter = matcher.group();
			body = content.substring(matcher.end());
		}

		String formattedBody = formatBody(body);

		return frontMatter + formattedBody;
	}

	private String formatBody(String body) {
		String normalized = body.replace("\r\n", "\n").replace("\n", "");

		StringBuilder result = new StringBuilder();
		StringBuilder currentSentence = new StringBuilder();

		for (int i = 0; i < normalized.length(); i++) {
			char c = normalized.charAt(i);
			currentSentence.append(c);

			if (SENTENCE_END_CHARS.indexOf(c) >= 0) {
				appendSentence(result, currentSentence.toString());
				currentSentence.setLength(0);
			}
		}

		if (!currentSentence.isEmpty()) {
			String remaining = currentSentence.toString().trim();
			if (!remaining.isEmpty()) {
				if (result.length() > 0) {
					result.append(startsWithTrigger(remaining) ? "\n\n\n" : "\n\n");
				}
				result.append(remaining);
			}
		}

		return result.toString();
	}

	private void appendSentence(StringBuilder result, String sentence) {
		String trimmed = sentence.trim();
		if (trimmed.isEmpty()) {
			return;
		}

		if (result.length() > 0) {
			result.append(startsWithTrigger(trimmed) ? "\n\n\n" : "\n\n");
		}

		result.append(trimmed);
	}

	private boolean startsWithTrigger(String sentence) {
		return sentence.startsWith(PARAGRAPH_BREAK_TRIGGER);
	}
}