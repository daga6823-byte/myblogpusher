package com.app.myblogpusher.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.languagetool.JLanguageTool;
import org.languagetool.language.Japanese;
import org.languagetool.rules.RuleMatch;
import org.languagetool.rules.spelling.SpellingCheckRule;
import org.springframework.stereotype.Service;

@Service
public class LanguageToolService {

	public List<LanguageToolMatch> checkText(String content) {

		JLanguageTool langTool = new JLanguageTool(new Japanese());

		List<RuleMatch> matches;
		try {
			matches = langTool.check(content);
		} catch (IOException e) {
			throw new RuntimeException("LanguageToolによる解析に失敗しました", e);
		}

		List<LanguageToolMatch> result = new ArrayList<>();

		for (RuleMatch match : matches) {
			boolean isSpelling = match.getRule() instanceof SpellingCheckRule;
			String matchedText = content.substring(match.getFromPos(), match.getToPos());

			System.out.println("[LT検出] isSpelling=" + isSpelling
					+ " ruleClass=" + match.getRule().getClass().getName()
					+ " text=" + matchedText
					+ " rule=" + match.getRule().getId());

			String suggestion = match.getSuggestedReplacements().isEmpty()
					? ""
					: match.getSuggestedReplacements().get(0);

			result.add(new LanguageToolMatch(
					match.getFromPos(),
					match.getToPos(),
					matchedText,
					suggestion,
					match.getMessage(),
					isSpelling ? "Misspelling" : "Other"));
		}

		return result;
	}

	public List<LanguageToolMatch> filterTypos(List<LanguageToolMatch> matches) {
		return matches.stream()
				.filter(m -> "Misspelling".equals(m.getCategory()))
				.toList();
	}

	public List<LanguageToolMatch> filterProofreading(List<LanguageToolMatch> matches) {
		return matches.stream()
				.filter(m -> !"Misspelling".equals(m.getCategory()))
				.toList();
	}

	public static class LanguageToolMatch {
		private final int fromPos;
		private final int toPos;
		private final String matchedText;
		private final String suggestion;
		private final String message;
		private final String category;

		public LanguageToolMatch(int fromPos, int toPos, String matchedText,
				String suggestion, String message, String category) {
			this.fromPos = fromPos;
			this.toPos = toPos;
			this.matchedText = matchedText;
			this.suggestion = suggestion;
			this.message = message;
			this.category = category;
		}

		public int getFromPos() {
			return fromPos;
		}

		public int getToPos() {
			return toPos;
		}

		public String getMatchedText() {
			return matchedText;
		}

		public String getSuggestion() {
			return suggestion;
		}

		public String getMessage() {
			return message;
		}

		public String getCategory() {
			return category;
		}
	}
}