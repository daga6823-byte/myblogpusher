/**
 * 画像カテゴリー選択用DTO
 *
 * image_assetに登録されているフォルダ一覧を
 * 画像選択画面のカテゴリーコンボボックスへ表示する。
 */

package com.app.myblogpusher.dto;

public class ImageCategoryDto {

    private String folderName;

    public ImageCategoryDto(String folderName) {
        this.folderName = folderName;
    }

    public String getFolderName() {
        return folderName;
    }
}