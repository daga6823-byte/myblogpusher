package com.app.myblogpusher.dto;

public class EnglishDictionaryForm {

	private Long id;
	private String japaneseWord;
	private String english;

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getJapaneseWord() {
		return japaneseWord;
	}

	public void setJapaneseWord(String japaneseWord) {
		this.japaneseWord = japaneseWord;
	}

	public String getEnglish() {
		return english;
	}

	public void setEnglish(String english) {
		this.english = english;
	}
}