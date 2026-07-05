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

	private String toRomanized(String text) {
		StringBuilder result = new StringBuilder();
		List<EnglishDictionary> allEntries = englishDictionaryRepository.findAll();
		List<Token> tokens = tokenizer.tokenize(text);

		for (Token token : tokens) {
			String partOfSpeech = token.getPartOfSpeechLevel1();
			String reading = token.getReading();
			String surface = token.getSurface();

			// 記号はスキップ
			if ("記号".equals(partOfSpeech))
				continue;

			// 助詞・助動詞はマップで変換
			if ("助詞".equals(partOfSpeech) || "助動詞".equals(partOfSpeech)) {
				String mapped = PARTICLE_MAP.getOrDefault(reading, "");
				if (!mapped.isEmpty()) {
					result.append(mapped).append(" ");
				}
				continue;
			}

			// 表層形で辞書検索
			// 表層形で辞書検索（完全一致優先）
			String english = allEntries.stream()
					.filter(e -> e.getJapanese().equals(surface))
					.map(EnglishDictionary::getEnglish)
					.findFirst()
					.orElse(null);

			// 動詞は原形でも辞書検索
			if (english == null && "動詞".equals(partOfSpeech)) {
				String baseForm = token.getBaseForm();
				if (baseForm != null && !baseForm.equals("*")) {
					english = allEntries.stream()
							.filter(e -> e.getJapanese().equals(baseForm))
							.map(EnglishDictionary::getEnglish)
							.findFirst()
							.orElse(null);
				}
				// 活用形そのままでも検索
				if (english == null) {
					english = allEntries.stream()
							.filter(e -> e.getJapanese().equals(surface))
							.map(EnglishDictionary::getEnglish)
							.findFirst()
							.orElse(null);
				}
			}

			// 動詞は原形の読みで辞書検索
			String searchReading = reading;
			if ("動詞".equals(partOfSpeech)) {
				String baseForm = token.getBaseForm();
				if (baseForm != null && !baseForm.equals("*")) {
					List<Token> baseTokens = tokenizer.tokenize(baseForm);
					if (!baseTokens.isEmpty() && baseTokens.get(0).getReading() != null) {
						searchReading = baseTokens.get(0).getReading();
					}
					// 原形の表層形でも辞書検索
					english = allEntries.stream()
							.filter(e -> baseForm.equals(e.getJapanese()))
							.map(EnglishDictionary::getEnglish)
							.findFirst()
							.orElse(null);
					if (english != null) {
						result.append(english).append(" ");
						continue;
					}
				}
			}

			// 読みでローマ字変換
			if (searchReading != null && !searchReading.isEmpty() && !searchReading.equals("*")) {
				result.append(katakanaToRomaji(searchReading)).append(" ");
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
		List<Token> tokens = tokenizer.tokenize(title);

		for (Token token : tokens) {
			String partOfSpeech = token.getPartOfSpeechLevel1();
			String reading = token.getReading();
			String surface = token.getSurface();

			// 記号のみスキップ
			if ("記号".equals(partOfSpeech))
				continue;

			// 助詞・助動詞はPARTICLE_MAPで変換
			if ("助詞".equals(partOfSpeech) || "助動詞".equals(partOfSpeech)) {
				String mapped = PARTICLE_MAP.getOrDefault(reading, "");
				if (!mapped.isEmpty()) {
					result.add(new SlugAnalysisDto(surface, reading, partOfSpeech, mapped, true));
				}
				continue;
			}

			// 表層形で辞書検索
			String english = allEntries.stream()
					.filter(e -> surface.contains(e.getJapanese()) || e.getJapanese().contains(surface))
					.map(EnglishDictionary::getEnglish)
					.findFirst()
					.orElse(null);

			// 動詞は原形でも辞書検索
			if (english == null && "動詞".equals(partOfSpeech)) {
				String baseForm = token.getBaseForm();
				if (baseForm != null && !baseForm.equals("*")) {
					english = allEntries.stream()
							.filter(e -> baseForm.equals(e.getJapanese()))
							.map(EnglishDictionary::getEnglish)
							.findFirst()
							.orElse(null);
				}
			}

			boolean fromDictionary = english != null;
			String converted = fromDictionary ? english
					: katakanaToRomaji(reading != null ? reading : surface);

			// 登録済みも含めてリストに追加（除外しない）
			result.add(new SlugAnalysisDto(surface, reading, partOfSpeech, converted, fromDictionary));
		}
		return result;
	}

	public String generateSlugFromAnalysis(List<SlugAnalysisDto> analysis) {
		StringBuilder result = new StringBuilder();
		String prev = "";

		for (SlugAnalysisDto token : analysis) {
			String converted = token.getConverted();
			if (converted == null || converted.isEmpty())
				continue;

			// ed/pastは直前が動詞の場合のみくっつける
			// 直前が辞書登録済み（既に適切な英単語）の場合はスキップ
			if ((converted.equals("ed") || converted.equals("past"))) {
				if (token.isFromDictionary()) {
					// 助詞・助動詞が辞書登録されている場合はそのまま追加
					result.append(converted).append("-");
				}
				// fromDictionaryでない助動詞のed/pastは直前の単語にくっつける
				else if (result.length() > 0) {
					String current = result.toString();
					if (current.endsWith("-")) {
						// 直前のトークンが辞書登録済みならスキップ
						if (!prev.isEmpty() && isAlreadyPastForm(prev)) {
							continue;
						}
						result.setLength(current.length() - 1);
						result.append(converted).append("-");
					}
				}
				prev = converted;
				continue;
			}

			result.append(converted).append("-");
			prev = converted;
		}

		return result.toString()
				.toLowerCase()
				.replaceAll("-+", "-")
				.replaceAll("^-+|-+$", "")
				.trim();
	}

	// 既に過去形っぽい英単語かどうか判定（came、wentなど不規則変化）
	private boolean isAlreadyPastForm(String word) {
		// edで終わっている or 辞書登録済みの不規則過去形
		return word.endsWith("ed") || word.endsWith("came") || word.endsWith("went")
				|| word.endsWith("was") || word.endsWith("were") || word.endsWith("had");
	}
}