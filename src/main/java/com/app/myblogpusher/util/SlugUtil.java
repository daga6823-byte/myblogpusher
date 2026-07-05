/**
 * 記事タイトルからURLスラッグを生成するユーティリティ
 * kuromojiで形態素解析し、英単語辞典・助詞マップ・ローマ字変換を組み合わせてスラッグを生成する
 * カテゴリースラッグの生成も担当
 */

package com.app.myblogpusher.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.app.myblogpusher.dto.SlugAnalysisDto;
import com.app.myblogpusher.entity.EnglishDictionary;
import com.app.myblogpusher.repository.EnglishDictionaryRepository;
import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;

@Component
public class SlugUtil {

	private static final Tokenizer tokenizer = new Tokenizer();
	private final EnglishDictionaryRepository englishDictionaryRepository;

	public SlugUtil(EnglishDictionaryRepository englishDictionaryRepository) {
		this.englishDictionaryRepository = englishDictionaryRepository;
	}

	public String generateSlug(String title) {
		if (title == null || title.isBlank()) {
			return "no-title";
		}

		String romanized = toRomanized(title);

		return romanized
				.toLowerCase()
				.replaceAll("\\s+", "-") // 先にスペースを-に変換
				.replaceAll("[^a-z0-9-]", "") // 次に英数字と-以外を除去
				.replaceAll("-+", "-") // 連続する-を一つに
				.replaceAll("^-+|-+$", "")
				.trim();
	}

	private static final Map<String, String> PARTICLE_MAP = Map.ofEntries(
			Map.entry("ニ", "to"),
			Map.entry("ヲ", "of"),
			Map.entry("ガ", "of"),
			Map.entry("ハ", "is"),
			Map.entry("ワ", "is"),
			Map.entry("テ", "ed"),
			Map.entry("タ", "past"),
			Map.entry("デ", "at"),
			Map.entry("カラ", "from"),
			Map.entry("マデ", "until"),
			Map.entry("ノ", "of"),
			Map.entry("モ", "also"),
			Map.entry("ヤ", "and"),
			Map.entry("ト", "and"),
			Map.entry("ケド", "but"),
			Map.entry("ケレド", "but"),
			Map.entry("シカ", "only"),
			Map.entry("サエ", "even"),
			Map.entry("ナド", "etc"),
			Map.entry("ヨリ", "than"),
			Map.entry("ヘ", "to"),
			Map.entry("ネ", ""),
			Map.entry("ヨ", ""),
			Map.entry("ナ", ""),
			Map.entry("ゾ", ""));

	private String replaceWithDictionary(String title) {
		List<EnglishDictionary> allEntries = englishDictionaryRepository.findAll();
		String result = title;
		for (EnglishDictionary entry : allEntries) {
			result = result.replace(entry.getJapanese(), " " + entry.getEnglish() + " ");
		}
		return result;
	}

	private String toRomanized(String text) {
		text = replaceWithDictionary(text);

		StringBuilder result = new StringBuilder();

		List<Token> tokens = tokenizer.tokenize(text);
		for (Token token : tokens) {
			String partOfSpeech = token.getPartOfSpeechLevel1();
			String reading = token.getReading();

			// 助詞・助動詞はマップで変換、なければローマ字
			if ("助詞".equals(partOfSpeech) || "助動詞".equals(partOfSpeech)) {
				String mapped = PARTICLE_MAP.get(reading);
				if (mapped != null && !mapped.isEmpty()) {
					result.append(mapped).append(" ");
				} else if (reading != null && !reading.equals("*")) {
					result.append(katakanaToRomaji(reading)).append(" ");
				}
				continue;
			}

			// 記号はスキップ（句読点等）
			if ("記号".equals(partOfSpeech)) {
				continue;
			}

			// 動詞は原形の読みで辞書検索
			String searchReading = reading;
			if ("動詞".equals(partOfSpeech)) {
				String baseForm = token.getBaseForm();
				if (baseForm != null && !baseForm.equals("*")) {
					List<Token> baseTokens = tokenizer.tokenize(baseForm);
					if (!baseTokens.isEmpty()) {
						String baseReading = baseTokens.get(0).getReading();
						if (baseReading != null && !baseReading.equals("*")) {
							searchReading = baseReading;
						}
					}
				}
			}

			if (searchReading != null && !searchReading.isEmpty() && !searchReading.equals("*")) {
				String english = searchEnglishDictionary(searchReading);
				if (english != null) {
					result.append(english).append(" ");
				} else {
					result.append(katakanaToRomaji(searchReading)).append(" ");
				}
			} else {
				result.append(token.getSurface()).append(" ");
			}
		}

		return result.toString().trim();
	}

	private String searchEnglishDictionary(String japaneseWord) {
		if (englishDictionaryRepository == null) {
			return null;
		}
		return englishDictionaryRepository.findByJapanese(japaneseWord)
				.map(EnglishDictionary::getEnglish)
				.orElse(null);
	}

	private String searchEnglishDictionary(String reading, String surface) {
		// 読みで検索
		Optional<EnglishDictionary> byReading = englishDictionaryRepository.findByJapanese(reading);
		if (byReading.isPresent())
			return byReading.get().getEnglish();

		// 表層形で検索
		Optional<EnglishDictionary> bySurface = englishDictionaryRepository.findByJapanese(surface);
		if (bySurface.isPresent())
			return bySurface.get().getEnglish();

		return null;
	}

	public String generateCategorySlug(String categoryName) {
		if (categoryName == null || categoryName.isBlank()) {
			return "no-category";
		}

		String romanized = toRomanized(categoryName);

		return romanized
				.toLowerCase()
				.replaceAll("[^a-z0-9]", "")
				.replaceAll("\\s+", "_")
				.replaceAll("^_+|_+$", "")
				.trim();
	}

	public static String katakanaToRomaji(String text) {
		// カタカナをひらがなに変換してからローマ字化
		StringBuilder hiragana = new StringBuilder();
		for (char c : text.toCharArray()) {
			if (c >= 'ァ' && c <= 'ン') {
				hiragana.append((char) (c - 'ァ' + 'ぁ'));
			} else {
				hiragana.append(c);
			}
		}
		return hiraganaToRomaji(hiragana.toString());
	}

	public static String hiraganaToRomaji(String hiragana) {
		return hiragana
				.replaceAll("あ", "a").replaceAll("い", "i").replaceAll("う", "u").replaceAll("え", "e")
				.replaceAll("お", "o")
				.replaceAll("か", "ka").replaceAll("き", "ki").replaceAll("く", "ku").replaceAll("け", "ke")
				.replaceAll("こ", "ko")
				.replaceAll("が", "ga").replaceAll("ぎ", "gi").replaceAll("ぐ", "gu").replaceAll("げ", "ge")
				.replaceAll("ご", "go")
				.replaceAll("さ", "sa").replaceAll("し", "shi").replaceAll("す", "su").replaceAll("せ", "se")
				.replaceAll("そ", "so")
				.replaceAll("ざ", "za").replaceAll("じ", "ji").replaceAll("ず", "zu").replaceAll("ぜ", "ze")
				.replaceAll("ぞ", "zo")
				.replaceAll("た", "ta").replaceAll("ち", "chi").replaceAll("つ", "tsu").replaceAll("て", "te")
				.replaceAll("と", "to")
				.replaceAll("だ", "da").replaceAll("ぢ", "di").replaceAll("づ", "du").replaceAll("で", "de")
				.replaceAll("ど", "do")
				.replaceAll("な", "na").replaceAll("に", "ni").replaceAll("ぬ", "nu").replaceAll("ね", "ne")
				.replaceAll("の", "no")
				.replaceAll("は", "ha").replaceAll("ひ", "hi").replaceAll("ふ", "fu").replaceAll("へ", "he")
				.replaceAll("ほ", "ho")
				.replaceAll("ば", "ba").replaceAll("び", "bi").replaceAll("ぶ", "bu").replaceAll("べ", "be")
				.replaceAll("ぼ", "bo")
				.replaceAll("ぱ", "pa").replaceAll("ぴ", "pi").replaceAll("ぷ", "pu").replaceAll("ぺ", "pe")
				.replaceAll("ぽ", "po")
				.replaceAll("ま", "ma").replaceAll("み", "mi").replaceAll("む", "mu").replaceAll("め", "me")
				.replaceAll("も", "mo")
				.replaceAll("や", "ya").replaceAll("ゆ", "yu").replaceAll("よ", "yo")
				.replaceAll("ら", "ra").replaceAll("り", "ri").replaceAll("る", "ru").replaceAll("れ", "re")
				.replaceAll("ろ", "ro")
				.replaceAll("わ", "wa").replaceAll("ゐ", "wi").replaceAll("ゑ", "we").replaceAll("を", "wo")
				.replaceAll("ん", "n");
	}

	public List<SlugAnalysisDto> analyzeSlug(String title) {
		List<SlugAnalysisDto> result = new ArrayList<>();
		List<Token> tokens = tokenizer.tokenize(title);

		for (Token token : tokens) {
			String partOfSpeech = token.getPartOfSpeechLevel1();
			String reading = token.getReading();
			String surface = token.getSurface();

			// 記号・助詞・助動詞は除外
			if ("記号".equals(partOfSpeech)
					|| "助詞".equals(partOfSpeech)
					|| "助動詞".equals(partOfSpeech)) {
				continue;
			}

			String searchReading = reading;
			if ("動詞".equals(partOfSpeech)) {
				String baseForm = token.getBaseForm();
				if (baseForm != null && !baseForm.equals("*")) {
					List<Token> baseTokens = tokenizer.tokenize(baseForm);
					if (!baseTokens.isEmpty() && baseTokens.get(0).getReading() != null) {
						searchReading = baseTokens.get(0).getReading();
					}
				}
			}

			String english = searchEnglishDictionary(searchReading, surface);
			boolean fromDictionary = english != null;
			String converted = fromDictionary ? english
					: katakanaToRomaji(searchReading != null ? searchReading : surface);

			// 登録済みは除外
			if (fromDictionary)
				continue;

			result.add(new SlugAnalysisDto(surface, reading, partOfSpeech, converted, false));
		}
		return result;
	}

	public String generateSlugFromAnalysis(List<SlugAnalysisDto> analysis) {
		StringBuilder result = new StringBuilder();
		for (SlugAnalysisDto token : analysis) {
			String converted = token.getConverted();
			if (converted != null && !converted.isEmpty()) {
				result.append(converted).append(" ");
			}
		}
		return result.toString().trim()
				.toLowerCase()
				.replaceAll("\\s+", "-")
				.replaceAll("[^a-z0-9-]", "")
				.replaceAll("-+", "-")
				.replaceAll("^-+|-+$", "")
				.trim();
	}
}