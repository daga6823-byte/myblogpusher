package com.app.myblogpusher.util;

import java.util.List;

import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;

public class SlugUtil {

	private static final Tokenizer tokenizer = new Tokenizer();

	private SlugUtil() {
	}

	public static String generateSlug(String title) {

		if (title == null || title.isBlank()) {
			return "no-title";
		}

		// 日本語をカタカナに変換してからローマ字化
		String romanized = toRomanized(title);

		return romanized
				.toLowerCase()
				.replaceAll("[^a-z0-9]", "")
				.replaceAll("\\s+", "-")
				.replaceAll("^-+|-+$", "")
				.trim();
	}

	private static String toRomanized(String text) {
		StringBuilder result = new StringBuilder();

		List<Token> tokens = tokenizer.tokenize(text);
		for (Token token : tokens) {
			String reading = token.getReading();
			if (reading != null && !reading.isEmpty() && !reading.equals("*")) {
				result.append(katakanaToRomaji(reading)).append(" ");
			} else {
				result.append(token.getSurface());
			}
		}

		return result.toString().trim();
	}

	private static String katakanaToRomaji(String katakana) {
		return katakana
				.replaceAll("ア", "a").replaceAll("イ", "i").replaceAll("ウ", "u").replaceAll("エ", "e")
				.replaceAll("オ", "o")
				.replaceAll("カ", "ka").replaceAll("キ", "ki").replaceAll("ク", "ku").replaceAll("ケ", "ke")
				.replaceAll("コ", "ko")
				.replaceAll("サ", "sa").replaceAll("シ", "si").replaceAll("ス", "su").replaceAll("セ", "se")
				.replaceAll("ソ", "so")
				.replaceAll("タ", "ta").replaceAll("チ", "ti").replaceAll("ツ", "tu").replaceAll("テ", "te")
				.replaceAll("ト", "to")
				.replaceAll("ナ", "na").replaceAll("ニ", "ni").replaceAll("ヌ", "nu").replaceAll("ネ", "ne")
				.replaceAll("ノ", "no")
				.replaceAll("ハ", "ha").replaceAll("ヒ", "hi").replaceAll("フ", "hu").replaceAll("ヘ", "he")
				.replaceAll("ホ", "ho")
				.replaceAll("マ", "ma").replaceAll("ミ", "mi").replaceAll("ム", "mu").replaceAll("メ", "me")
				.replaceAll("モ", "mo")
				.replaceAll("ヤ", "ya").replaceAll("ユ", "yu").replaceAll("ヨ", "yo")
				.replaceAll("ラ", "ra").replaceAll("リ", "ri").replaceAll("ル", "ru").replaceAll("レ", "re")
				.replaceAll("ロ", "ro")
				.replaceAll("ワ", "wa").replaceAll("ヲ", "wo").replaceAll("ン", "n")
				.replaceAll("ガ", "ga").replaceAll("ギ", "gi").replaceAll("グ", "gu").replaceAll("ゲ", "ge")
				.replaceAll("ゴ", "go")
				.replaceAll("ザ", "za").replaceAll("ジ", "zi").replaceAll("ズ", "zu").replaceAll("ゼ", "ze")
				.replaceAll("ゾ", "zo")
				.replaceAll("ダ", "da").replaceAll("ヂ", "di").replaceAll("ヅ", "du").replaceAll("デ", "de")
				.replaceAll("ド", "do")
				.replaceAll("バ", "ba").replaceAll("ビ", "bi").replaceAll("ブ", "bu").replaceAll("ベ", "be")
				.replaceAll("ボ", "bo")
				.replaceAll("パ", "pa").replaceAll("ピ", "pi").replaceAll("プ", "pu").replaceAll("ペ", "pe")
				.replaceAll("ポ", "po");
	}
	
	public static String generateCategorySlug(String categoryName) {
		if (categoryName == null || categoryName.isBlank()) {
			return "no-category";
		}

		// 日本語をローマ字化
		String romanized = toRomanized(categoryName);

		return romanized
				.toLowerCase()
				.replaceAll("[^a-z0-9]", "")
				.replaceAll("\\s+", "_")
				.replaceAll("^_+|_+$", "")
				.trim();
	}
}