package com.app.myblogpusher.dto;

public class TypoScanResultView {
	private final String wrongWord;
	private final String suggestion;
	private final String message;

	public TypoScanResultView(String wrongWord, String suggestion, String message) {
		this.wrongWord = wrongWord;
		this.suggestion = suggestion;
		this.message = message;
	}

	public String getWrongWord() {
		return wrongWord;
	}

	public String getSuggestion() {
		return suggestion;
	}

	public String getMessage() {
		return message;
	}
}