
/**
 * Hugoカテゴリーのインデックス編集画面を担当するコントローラー
 *
 * 編集画面を表示し、編集対象となるカテゴリー階層を渡す。
 */

package com.app.myblogpusher.controller.Index;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.myblogpusher.dto.Category.CategoryOptionView;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.Index.IndexEditService;
import com.app.myblogpusher.service.Index.IndexWorkService;
import com.app.myblogpusher.service.Index.IndexWorkspaceService;

import jakarta.servlet.http.HttpSession;

@Controller
public class IndexEditController {

	@Autowired
	private IndexEditService hugoIndexService;

	@Autowired
	private IndexWorkService indexWorkService;
	
	@Autowired
	private IndexWorkspaceService indexWorkspaceService;

	/**
	 * インデックス管理画面を表示する。
	 */
	@GetMapping("/index/edit")
	public String showIndexManager(
			@RequestParam(required = false) String hugoPath,
			HttpSession session,
			Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login";
		}

		Long userId = loginUser.getUserId();

		List<CategoryOptionView> categoryPaths =
		        hugoIndexService.findCategoryPaths(userId);

		model.addAttribute("categoryPaths", categoryPaths);
		model.addAttribute("categories", categoryPaths);
		model.addAttribute("selectedHugoPath", hugoPath);

		CategoryOptionView selectedCategory = categoryPaths.stream()
		        .filter(category -> hugoPath != null
		                && hugoPath.equals(category.getCategoryPath()))
		        .findFirst()
		        .orElse(null);

		model.addAttribute(
		        "categoryPath",
		        selectedCategory != null
		                ? selectedCategory.getCategoryPath()
		                : null);

		model.addAttribute(
		        "categoryGroupId",
		        selectedCategory != null
		                ? selectedCategory.getGroupId()
		                : null);

		if (hugoPath != null) {
			// 自動保存データを確認し、同じカテゴリーのデータがあれば復元する。
			var workspace = indexWorkspaceService.find(userId);

			if (workspace.isPresent()
			        && hugoPath.equals(workspace.get().getHugoPath())) {

				model.addAttribute("title", workspace.get().getTitle());
				model.addAttribute("content", workspace.get().getContent());

			} else {
				// 自動保存データがなければ、編集中データを表示する。
				indexWorkService.findByUserIdAndHugoPath(userId, hugoPath)
						.ifPresent(work -> {
							model.addAttribute("title", work.getTitle());
							model.addAttribute("content", work.getContent());
						});
			}
		}

		return "index/index_edit";
	}
}
