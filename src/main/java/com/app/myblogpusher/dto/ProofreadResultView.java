package com.app.myblogpusher.dto;

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

	public int getIndex() {
		return index;
	}

	public int getFromPos() {
		return fromPos;
	}

	public int getToPos() {
		return toPos;
	}

	public String getMatchedText() {
		return matchedText;
	}

	public String getMessage() {
		return message;
	}

	public String getSuggestion() {
		return suggestion;
	}
}