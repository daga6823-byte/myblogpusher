package com.app.myblogpusher.dto;

import lombok.Getter;

@Getter
public class ProofreadResultView {
	private final int index;
	private final int fromPos;
	private final int toPos;
	private final String matchedText;
	private final String message;
	private final String suggestion;

	public ProofreadResultView(int index, int fromPos, int toPos, String matchedText, String message,
			String suggestion) {
		this.index = index;
		this.fromPos = fromPos;
		this.toPos = toPos;
		this.matchedText = matchedText;
		this.message = message;
		this.suggestion = suggestion;
	}
}