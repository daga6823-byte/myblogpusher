package com.app.myblogpusher.util;

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
}