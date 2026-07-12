/**
 * front matter（+++または---で区切られたメタデータ部分）から
 * title・categories・dateを抽出するユーティリティ
 */

package com.app.myblogpusher.util;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

@Component
public class FrontMatterUtil {

	public String extractTitle(String content) {
		// Front matter が `+++` または `---` で区切られている
		String delimiter = content.startsWith("+++") ? "\\+\\+\\+" : "---";

		String[] parts = content.split(delimiter, 3);
		if (parts.length >= 2) {
			String frontMatter = parts[1];
			for (String line : frontMatter.split("\n")) {
				if (line.contains("title")) {
					// TOML形式: title = 'xxx'
					// YAML形式: title: xxx
					String title = line.replaceAll("^[^=:]*[=:]\\s*['\"]?", "").replaceAll("['\"]\\s*$", "").trim();
					return title;
				}
			}
		}
		return "（タイトルなし）";
	}

	public List<String> extractCategories(String content) {
		String delimiter = content.startsWith("+++") ? "\\+\\+\\+" : "---";

		String[] parts = content.split(delimiter, 3);
		if (parts.length >= 2) {
			String frontMatter = parts[1];
			for (String line : frontMatter.split("\n")) {
				if (line.contains("categories")) {
					String categoryLine = line.replaceAll("^[^=]*=\\s*", "").trim();
					categoryLine = categoryLine.replaceAll("[\\[\\]]", "");
					String[] cats = categoryLine.split(",");
					List<String> result = new ArrayList<>();
					for (String cat : cats) {
						result.add(cat.replaceAll("['\"]", "").trim());
					}
					return result;
				}
			}
		}
		return new ArrayList<>();
	}

	/**
	 * front matterのdateフィールド（例: date = '2026-07-12T21:39:45+09:00'）を抽出しLocalDateTimeに変換する
	 * パースできない場合はnullを返す
	 */
	public LocalDateTime extractDate(String content) {
		String delimiter = content.startsWith("+++") ? "\\+\\+\\+" : "---";

		String[] parts = content.split(delimiter, 3);
		if (parts.length >= 2) {
			String frontMatter = parts[1];
			for (String line : frontMatter.split("\n")) {
				if (line.trim().startsWith("date")) {
					// TOML形式: date = 'xxx'
					// YAML形式: date: xxx
					String dateStr = line.replaceAll("^[^=:]*[=:]\\s*['\"]?", "").replaceAll("['\"]\\s*$", "").trim();
					try {
						return OffsetDateTime.parse(dateStr, DateTimeFormatter.ISO_OFFSET_DATE_TIME)
								.toLocalDateTime();
					} catch (Exception e) {
						return null;
					}
				}
			}
		}
		return null;
	}
}