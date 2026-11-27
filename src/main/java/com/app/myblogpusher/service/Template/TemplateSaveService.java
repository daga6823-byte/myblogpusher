
/**
 * テンプレートの新規作成・更新を担当するサービス
 *
 * テンプレート本体の保存と、新規作成時の初期レイアウト登録を統括する。
 * DB操作自体は既存のTemplateServiceとTemplateLayoutFacadeServiceに委譲する。
 */
package com.app.myblogpusher.service.Template;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.Template.TemplateCellEntity;
import com.app.myblogpusher.entity.Template.TemplateColumnEntity;
import com.app.myblogpusher.entity.Template.TemplateEntity;
import com.app.myblogpusher.entity.Template.TemplateRowEntity;
import com.app.myblogpusher.service.Facade.TemplateLayoutFacadeService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateSaveService {

    private final TemplateService templateService;
    private final TemplateLayoutFacadeService templateLayoutFacadeService;

    /**
     * テンプレートを新規作成または更新する。
     *
     * 新規作成時はテンプレート本体の登録後に
     * 初期行・列・セルを登録する。
     *
     * @param userId ログインユーザーID
     * @param template 保存するテンプレート情報
     * @return 保存したテンプレートID
     */
    public Long saveTemplate(Long userId, TemplateEntity template) {

        if (template.getTemplateId() != null) {

            TemplateEntity updated = templateService.updateTemplate(
                    userId,
                    template);

            return updated.getTemplateId();
        }

        template.setUserId(userId);

        // テンプレート本体を新規登録する。
        TemplateEntity saved = templateService.createTemplate(template);

        Long templateId = saved.getTemplateId();

        // 初期行を登録する。
        TemplateRowEntity row = new TemplateRowEntity();
        row.setTemplateId(templateId);
        row.setRowNumber(1);
        row.setHeightUnit("auto");
        templateLayoutFacadeService.saveRow(row);

        // 初期列を登録する。
        TemplateColumnEntity column = new TemplateColumnEntity();
        column.setTemplateId(templateId);
        column.setColumnNumber(1);
        column.setWidthUnit("fr");
        templateLayoutFacadeService.saveColumn(column);

        // 初期セルを登録する。
        TemplateCellEntity cell = new TemplateCellEntity();
        cell.setTemplateId(templateId);
        cell.setRowNumber(1);
        cell.setColumnNumber(1);
        cell.setRowSpan(1);
        cell.setColumnSpan(1);
        cell.setCellType("EMPTY");
        templateLayoutFacadeService.saveCell(cell);

        return templateId;
    }
}
