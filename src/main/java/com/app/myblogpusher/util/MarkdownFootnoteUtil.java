package com.app.myblogpusher.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markdown脚注整理
 *
 * 本文出現順に脚注番号を振り直し、
 * 参考文献定義も並び替える。
 */
public class MarkdownFootnoteUtil {

	private static final Pattern REF_PATTERN = Pattern.compile("\\[\\^(\\d+)]");

	private static final Pattern DEF_PATTERN = Pattern.compile(
			"(?ms)^\\[\\^(\\d+)]:.*?(?=^\\[\\^\\d+]:|\\z)");

	private MarkdownFootnoteUtil() {
	}

	public static String normalize(String markdown) {

		Map<String, Integer> newNumbers = new LinkedHashMap<>();

		Matcher refMatcher = REF_PATTERN.matcher(markdown);

		int next = 1;

		while (refMatcher.find()) {

			String oldNo = refMatcher.group(1);

			if (!newNumbers.containsKey(oldNo)) {
				newNumbers.put(oldNo, next++);
			}
		}

		Matcher defMatcher = DEF_PATTERN.matcher(markdown);

		Map<String, String> definitions = new LinkedHashMap<>();

		while (defMatcher.find()) {

			String block = defMatcher.group();

			String no = defMatcher.group(1);

			definitions.put(no, block);
		}

		String body = DEF_PATTERN.matcher(markdown)
				.replaceAll("")
				.trim();

		for (Map.Entry<String, Integer> e : newNumbers.entrySet()) {

			body = body.replace(
					"[^" + e.getKey() + "]",
					"[^TMP" + e.getValue() + "]");
		}

		for (Map.Entry<String, Integer> e : newNumbers.entrySet()) {

			body = body.replace(
					"[^TMP" + e.getValue() + "]",
					"[^" + e.getValue() + "]");
		}

		List<String> reordered = new ArrayList<>();

		for (Map.Entry<String, Integer> e : newNumbers.entrySet()) {

			String def = definitions.get(e.getKey());

			if (def == null) {
				continue;
			}

			def = def.replaceFirst(
					"\\[\\^\\d+]:",
					"[^" + e.getValue() + "]:");

			reordered.add(def.trim());

		}

		StringBuilder result = new StringBuilder(body);

		if (!reordered.isEmpty()) {

			result.append("\n\n【参考文献】\n");

			for (String def : reordered) {

				result.append(def)
						.append("\n\n");
			}
		}

		return result.toString().trim() + "\n";
	}

}