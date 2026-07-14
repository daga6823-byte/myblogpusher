package com.app.myblogpusher.dto;

import lombok.Getter;

@Getter
public class TypoDictionaryView {

    private final Long typoId;
    private final String categoryName;
    private final String wrongWord;
    private final String correctWord;

    public TypoDictionaryView(Long typoId, String categoryName, String wrongWord, String correctWord) {
        this.typoId = typoId;
        this.categoryName = categoryName;
        this.wrongWord = wrongWord;
        this.correctWord = correctWord;
    }

}