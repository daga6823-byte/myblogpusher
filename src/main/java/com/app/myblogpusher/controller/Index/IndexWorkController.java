
/**
 * インデックス編集中データの保存を担当するコントローラー
 *
 * 下書き保存を受け付け、保存成功後にワークスペースを削除する。
 */
package com.app.myblogpusher.controller.Index;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.Index.IndexWorkService;
import com.app.myblogpusher.service.Index.IndexWorkspaceService;

import jakarta.servlet.http.HttpSession;

@Controller
public class IndexWorkController {

    @Autowired
    private IndexWorkService indexWorkService;

    @Autowired
    private IndexWorkspaceService indexWorkspaceService;

    /**
     * 編集中のインデックスを下書き保存する。
     */
    @PostMapping("/index/edit/save")
    public String saveDraft(
            @RequestParam Long groupId,
            @RequestParam String title,
            @RequestParam String content,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        UserMaster loginUser =
                (UserMaster) session.getAttribute("loginUser");

        if (loginUser == null) {
            return "redirect:/login";
        }

        Long userId = loginUser.getUserId();

        try {
            // index_workに下書きを保存する。
            indexWorkService.saveDraft(
                    userId,
                    groupId,
                    title,
                    content);

            // 保存成功後に自動保存データを削除する。
            indexWorkspaceService.delete(userId);

            redirectAttributes.addFlashAttribute(
                    "saved",
                    "下書きを保存しました。");

        } catch (IllegalArgumentException | IllegalStateException e) {
            redirectAttributes.addFlashAttribute(
                    "error",
                    "下書きの保存に失敗しました。");
        }

        return "redirect:/index/edit";
    }
}
