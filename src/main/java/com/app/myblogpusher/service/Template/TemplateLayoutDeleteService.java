
/**
 * テンプレートのレイアウト全体削除を担当するサービス
 *
 * セル・行・列のレイアウト情報を削除する。
 */
package com.app.myblogpusher.service.Template;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.repository.Template.TemplateCellRepository;
import com.app.myblogpusher.repository.Template.TemplateColumnRepository;
import com.app.myblogpusher.repository.Template.TemplateRowRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateLayoutDeleteService {

    private final TemplateCellRepository templateCellRepository;
    private final TemplateRowRepository templateRowRepository;
    private final TemplateColumnRepository templateColumnRepository;

    /**
     * テンプレートのレイアウト情報をすべて削除する。
     */
    public void deleteLayout(Long templateId) {

        templateCellRepository.deleteByTemplateId(templateId);
        templateRowRepository.deleteByTemplateId(templateId);
        templateColumnRepository.deleteByTemplateId(templateId);
    }
}
