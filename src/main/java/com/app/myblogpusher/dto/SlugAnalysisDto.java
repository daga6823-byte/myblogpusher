/**
 * スラッグ生成時の形態素分析結果を保持するDTO
 * 投稿確認画面での単語変換結果表示・辞書登録に使用
 */

package com.app.myblogpusher.dto;

public class SlugAnalysisDto {
	private String surface; // 表層形（元の単語）
	private String reading; // 読み
	private String partOfSpeech; // 品詞
	private String converted; // 変換結果（英単語orローマ字）
	private boolean fromDictionary; // 辞書から変換したか

	public SlugAnalysisDto(String surface, String reading, String partOfSpeech, String converted,
			boolean fromDictionary) {
		this.surface = surface;
		this.reading = reading;
		this.partOfSpeech = partOfSpeech;
		this.converted = converted;
		this.fromDictionary = fromDictionary;
	}

	public String getSurface() {
		return surface;
	}

	public String getReading() {
		return reading;
	}

	public String getPartOfSpeech() {
		return partOfSpeech;
	}

	public String getConverted() {
		return converted;
	}

	public boolean isFromDictionary() {
		return fromDictionary;
	}
}