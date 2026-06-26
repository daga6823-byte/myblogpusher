package com.app.myblogpusher.util;

public class SlugUtil {

	private SlugUtil() {
	}

	public static String generateSlug(String title) {

		if (title == null || title.isBlank()) {
			return "no-title";
		}

		return title
				.toLowerCase()
				.replaceAll("[^a-zA-Z0-9]", "")
				.trim();
	}
}