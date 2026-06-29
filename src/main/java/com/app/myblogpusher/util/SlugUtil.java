package com.app.myblogpusher.util;

import java.util.List;

import org.springframework.stereotype.Component;

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

	public String generateSlug(String title) { // staticを削除

		if (title == null || title.isBlank()) {
			return "no-title";
		}

		// 日本語をローマ字に変換
		String romanized = toRomanized(title);

		return romanized
				.toLowerCase()
				.replaceAll("[^a-z0-9]", "")
				.replaceAll("\\s+", "-")
				.replaceAll("^-+|-+$", "")
				.trim();
	}

	private String toRomanized(String text) { // staticを削除
		StringBuilder result = new StringBuilder();

		List<Token> tokens = tokenizer.tokenize(text);
		for (Token token : tokens) {
			String reading = token.getReading();
			if (reading != null && !reading.isEmpty() && !reading.equals("*")) {
				// 辞書検索
				String english = searchEnglishDictionary(reading);
				if (english != null) {
					result.append(english).append(" ");
				} else {
					result.append(katakanaToRomaji(reading)).append(" ");
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

	public static String katakanaToRomaji(String hiragana) {
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
}