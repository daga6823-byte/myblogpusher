package com.app.myblogpusher.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class CategoryDictionaryView {

    private final Long categoryId;
    private final String categoryName;
    private final Long parentCategoryId;
    private final String parentCategoryName;
    private final String displayName;
    private final long typoCount;

    public boolean isDeletable() {
        return typoCount == 0;
    }
}
