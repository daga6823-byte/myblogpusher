package com.app.myblogpusher.dto;

import lombok.Getter;

@Getter
public class TypoScanResultView {
	private final String wrongWord;
	private final String suggestion;
	private final String message;

	public TypoScanResultView(String wrongWord, String suggestion, String message) {
		this.wrongWord = wrongWord;
		this.suggestion = suggestion;
		this.message = message;
	}
}