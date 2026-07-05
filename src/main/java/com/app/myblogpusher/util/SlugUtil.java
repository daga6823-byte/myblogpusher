/**
 * 記事タイトルからURLスラッグを生成するユーティリティ
 * kuromojiで形態素解析し、英単語辞典・助詞マップ・ローマ字変換を組み合わせてスラッグを生成する
 * カテゴリースラッグの生成も担当
 */

package com.app.myblogpusher.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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
			Map.entry("タ", "ed"),
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

	private String toRomanized(String text) {
		List<EnglishDictionary> allEntries = englishDictionaryRepository.findAll();

		// 長い単語から優先して置換（スペースで区切る）
		allEntries.sort((a, b) -> b.getJapanese().length() - a.getJapanese().length());
		for (EnglishDictionary entry : allEntries) {
			text = text.replace(entry.getJapanese(), " " + entry.getEnglish() + " ");
		}

		// 残った助詞をPARTICLE_MAPで置換
		List<Token> tokens = tokenizer.tokenize(text);
		StringBuilder result = new StringBuilder();
		for (Token token : tokens) {
			String surface = token.getSurface();
			String partOfSpeech = token.getPartOfSpeechLevel1();
			String reading = token.getReading();

			// 既に英単語に置換済みの部分はそのまま
			if (surface.matches("[a-zA-Z]+")) {
				result.append(surface).append(" ");
				continue;
			}

			// 記号スキップ
			if ("記号".equals(partOfSpeech))
				continue;

			// 助詞・助動詞はPARTICLE_MAPで変換
			if ("助詞".equals(partOfSpeech) || "助動詞".equals(partOfSpeech)) {
				String mapped = PARTICLE_MAP.getOrDefault(reading, "");
				if (!mapped.isEmpty()) {
					result.append(mapped).append(" ");
				}
				continue;
			}

			// 残りはローマ字
			if (reading != null && !reading.equals("*")) {
				result.append(katakanaToRomaji(reading)).append(" ");
			} else {
				result.append(surface).append(" ");
			}
		}

		return result.toString().trim();
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
		List<EnglishDictionary> allEntries = englishDictionaryRepository.findAll();

		// 長い単語から優先して置換
		allEntries.sort((a, b) -> b.getJapanese().length() - a.getJapanese().length());
		String replaced = title;
		for (EnglishDictionary entry : allEntries) {
			replaced = replaced.replace(entry.getJapanese(), " " + entry.getEnglish() + " ");
		}

		List<Token> tokens = tokenizer.tokenize(replaced);

		for (Token token : tokens) {
			String partOfSpeech = token.getPartOfSpeechLevel1();
			String reading = token.getReading();
			String surface = token.getSurface();

			if ("記号".equals(partOfSpeech))
				continue;

			// 置換済み英単語はそのまま
			if (surface.matches("[a-zA-Z]+")) {
				result.add(new SlugAnalysisDto(surface, reading, partOfSpeech, surface, true));
				continue;
			}

			// 助詞・助動詞はPARTICLE_MAPで変換
			if ("助詞".equals(partOfSpeech) || "助動詞".equals(partOfSpeech)) {
				String mapped = PARTICLE_MAP.getOrDefault(reading, "");
				if (!mapped.isEmpty()) {
					result.add(new SlugAnalysisDto(surface, reading, partOfSpeech, mapped, false));
				}
				continue;
			}

			// 残りはローマ字
			String converted = (reading != null && !reading.equals("*"))
					? katakanaToRomaji(reading)
					: surface;
			result.add(new SlugAnalysisDto(surface, reading, partOfSpeech, converted, false));
		}
		return result;
	}

	public String generateSlugFromAnalysis(List<SlugAnalysisDto> analysis) {
		StringBuilder result = new StringBuilder();

		for (SlugAnalysisDto token : analysis) {
			String converted = token.getConverted();
			if (converted == null || converted.isEmpty())
				continue;

			if (converted.equals("ed")) {
			    if (result.length() > 0 && result.charAt(result.length() - 1) == '-') {
			        result.setLength(result.length() - 1);
			    }
			    String current = result.toString();
			    String lastWord = current.contains("-")
			        ? current.substring(current.lastIndexOf("-") + 1)
			        : current;
			    // 既にedで終わっていればスキップ
			    if (lastWord.endsWith("ed")) {
			        result.append("-");
			    } else {
			        result.append("ed-");
			    }
			} else {
			    result.append(converted).append("-");
			}
		}

		return result.toString()
				.toLowerCase()
				.replaceAll("-+", "-")
				.replaceAll("^-+|-+$", "")
				.trim();
	}
}