package com.app.myblogpusher.service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.dto.TypoDictionaryView;
import com.app.myblogpusher.entity.ArticleCategory;
import com.app.myblogpusher.entity.TypoCorrection;
import com.app.myblogpusher.repository.TypoCorrectionRepository;

@Service
public class TypoCorrectionService {

	@Autowired
	private TypoCorrectionRepository typoCorrectionRepository;

	public List<TypoMatch> findMatches(Long categoryId, String content) {

		categoryId = resolveDictionaryCategoryId(categoryId);

		List<TypoCorrection> rules = typoCorrectionRepository.findByCategoryIdOrCategoryIdIsNull(categoryId);

		// wrong_wordの文字数が長い順に並べ替える（範囲が広いルールを優先）
		rules.sort(
				Comparator.comparingInt(
						(TypoCorrection r) -> r.getWrongWord().length()).reversed());

		List<String> excludeWords = rules.stream()
				.filter(r -> r.getWrongWord().equals(r.getCorrectWord()))
				.map(TypoCorrection::getWrongWord)
				.toList();

		List<TypoMatch> matches = new ArrayList<>();
		int idx = 0;

		for (TypoCorrection rule : rules) {

			if (rule.getWrongWord().equals(rule.getCorrectWord())) {
				continue; // 除外指定の行はそのまま検出ルールにはしない
			}

			Pattern pattern = buildPattern(rule.getWrongWord());
			Matcher matcher = pattern.matcher(content);

			while (matcher.find()) {
				String matchedText = matcher.group();
				int start = matcher.start();

				int correctLength = rule.getCorrectWord().length();
				int endForCheck = Math.min(content.length(), start + correctLength);
				String surroundingText = content.substring(start, endForCheck);

				if (surroundingText.equals(rule.getCorrectWord())) {
					continue;
				}

				if (excludeWords.contains(matchedText)) {
					continue;
				}

				//				if (isAlreadyCorrect(content, start, matchedText.length(), rule.getCorrectWord())) {
				//				    continue;
				//				}

				String contextHtml = buildContextHtml(content, start, matchedText);
				matches.add(new TypoMatch(idx, matchedText, rule.getCorrectWord(), contextHtml));
				idx++;
			}
		}

		return matches;
	}

	private Pattern buildPattern(String wrongWordPattern) {
		StringBuilder regex = new StringBuilder();
		for (char c : wrongWordPattern.toCharArray()) {
			if (c == '〇') {
				regex.append("[・ー]{0,1}");
			} else {
				regex.append(Pattern.quote(String.valueOf(c)));
			}
		}
		return Pattern.compile(regex.toString());
	}

	private String buildContextHtml(String content, int index, String matchedText) {
		int range = 20;
		int start = Math.max(0, index - range);
		int end = Math.min(content.length(), index + matchedText.length() + range);

		String before = escapeHtml(content.substring(start, index));
		String matched = escapeHtml(matchedText);
		String after = escapeHtml(content.substring(index + matchedText.length(), end));

		return before + "<mark style=\"background-color: yellow;\">" + matched + "</mark>" + after;
	}

	private String escapeHtml(String text) {
		return text.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;");
	}

	public boolean insertTypo(Long categoryId, String wrongWord, String correctWord, Long userId) {

		boolean exists = (categoryId == null)
				? typoCorrectionRepository.existsByCategoryIdIsNullAndWrongWordAndCorrectWord(wrongWord, correctWord)
				: typoCorrectionRepository.existsByCategoryIdAndWrongWordAndCorrectWord(categoryId, wrongWord,
						correctWord);

		if (exists) {
			return false; // 既に同じ内容が登録済み
		}

		TypoCorrection typo = new TypoCorrection();
		typo.setCategoryId(categoryId);
		typo.setWrongWord(wrongWord);
		typo.setCorrectWord(correctWord);
		typo.setCreateDate(LocalDateTime.now());
		typo.setUpdateDate(LocalDateTime.now());
		typo.setCreateUser(userId);
		typo.setUpdateUser(userId);

		typoCorrectionRepository.save(typo);
		return true;
	}

	public Map<Long, Long> countByCategoryIds(List<Long> categoryIds) {

		if (categoryIds.isEmpty()) {
			return Map.of();
		}

		return typoCorrectionRepository.countByCategoryIds(categoryIds)
				.stream()
				.collect(Collectors.toMap(
						row -> (Long) row[0],
						row -> (Long) row[1]));
	}

	@Autowired
	private ArticleCategoryService articleCategoryService;

	public List<TypoDictionaryView> findDictionaryView(Long userId) {

		List<TypoCorrection> typos = typoCorrectionRepository.findByCreateUser(userId);

		return typos.stream()
				.map(t -> {
					String categoryName = (t.getCategoryId() == null)
							? "汎用"
							: articleCategoryService.findById(t.getCategoryId())
									.map(ArticleCategory::getCategoryName)
									.orElse("（不明）");

					return new TypoDictionaryView(t.getTypoId(), categoryName, t.getWrongWord(), t.getCorrectWord());
				})
				.toList();
	}

	public void update(Long typoId, String wrongWord, String correctWord, Long userId) {
		TypoCorrection typo = typoCorrectionRepository.findById(typoId).orElseThrow();

		if (!typo.getCreateUser().equals(userId)) {
			throw new IllegalStateException("他のユーザーの誤字パターンは編集できません");
		}

		typo.setWrongWord(wrongWord);
		typo.setCorrectWord(correctWord);
		typo.setUpdateUser(userId);
		typo.setUpdateDate(LocalDateTime.now());

		typoCorrectionRepository.save(typo);
	}

	public void delete(Long typoId, Long userId) {
		TypoCorrection typo = typoCorrectionRepository.findById(typoId).orElseThrow();

		if (!typo.getCreateUser().equals(userId)) {
			throw new IllegalStateException("他のユーザーの誤字パターンは削除できません");
		}

		typoCorrectionRepository.delete(typo);
	}

	public List<LanguageToolService.LanguageToolMatch> excludeKnownTypos(
			Long categoryId, List<LanguageToolService.LanguageToolMatch> ltMatches) {

		Long dictionaryCategoryId = articleCategoryService.findDictionaryCategoryId(categoryId);

		List<TypoCorrection> rules = typoCorrectionRepository.findByCategoryIdOrCategoryIdIsNull(
				dictionaryCategoryId);

		Set<String> knownWrongWords = rules.stream()
				.map(TypoCorrection::getWrongWord)
				.collect(Collectors.toSet());

		return ltMatches.stream()
				.filter(m -> !knownWrongWords.contains(m.getMatchedText()))
				.toList();
	}

	public boolean updateCategory(List<Long> typoIds, Long categoryId, Long userId) {

		List<TypoCorrection> typos = typoCorrectionRepository.findAllById(typoIds);

		for (TypoCorrection typo : typos) {
			if (!typo.getCreateUser().equals(userId)) {
				throw new IllegalStateException("他のユーザーの誤字パターンは編集できません");
			}
		}

		for (TypoCorrection typo : typos) {
			typo.setCategoryId(categoryId);
			typo.setUpdateUser(userId);
			typo.setUpdateDate(LocalDateTime.now());
		}

		typoCorrectionRepository.saveAll(typos);
		return true;
	}

	public void deleteAll(List<Long> typoIds, Long userId) {

		List<TypoCorrection> typos = typoCorrectionRepository.findAllById(typoIds);

		for (TypoCorrection typo : typos) {
			if (!typo.getCreateUser().equals(userId)) {
				throw new IllegalStateException("他のユーザーの誤字パターンは削除できません");
			}
		}

		typoCorrectionRepository.deleteAll(typos);
	}

	/**
	 * 編集時に参照するカテゴリーIDを取得する
	 * ルートカテゴリーは除外し、2階層目のカテゴリーIDを返す
	 */
	private Long resolveDictionaryCategoryId(Long categoryId) {

		if (categoryId == null) {
			return null;
		}

		ArticleCategory category = articleCategoryService.findById(categoryId).orElse(null);

		if (category == null) {
			return null;
		}

		while (category.getParentCategoryId() != null) {

			ArticleCategory parent = articleCategoryService
					.findById(category.getParentCategoryId())
					.orElse(null);

			if (parent == null) {
				break;
			}

			if (parent.getParentCategoryId() == null) {
				return category.getCategoryId();
			}

			category = parent;
		}

		return category.getCategoryId();
	}

	public Page<TypoDictionaryView> findDictionaryView(
			Long userId,
			int page,
			String keyword,
			Long categoryId) {

		Pageable pageable = PageRequest.of(
				page,
				50,
				Sort.by(
						Sort.Direction.ASC,
						"wrongWord"));

		Page<TypoCorrection> typoPage = typoCorrectionRepository.searchDictionary(
				userId,
				keyword == null ? "" : keyword,
				categoryId,
				pageable);

		return typoPage.map(t -> {

			String categoryName = (t.getCategoryId() == null)
					? "汎用"
					: articleCategoryService.findById(t.getCategoryId())
							.map(ArticleCategory::getCategoryName)
							.orElse("（不明）");

			return new TypoDictionaryView(
					t.getTypoId(),
					categoryName,
					t.getWrongWord(),
					t.getCorrectWord());
		});
	}

	public static class TypoMatch {
		private final int index;
		private final String wrongWord;
		private final String correctWord;
		private final String contextHtml;

		public TypoMatch(int index, String wrongWord, String correctWord, String contextHtml) {
			this.index = index;
			this.wrongWord = wrongWord;
			this.correctWord = correctWord;
			this.contextHtml = contextHtml;
		}

		public int getIndex() {
			return index;
		}

		public String getWrongWord() {
			return wrongWord;
		}

		public String getCorrectWord() {
			return correctWord;
		}

		public String getContextHtml() {
			return contextHtml;
		}
	}
}