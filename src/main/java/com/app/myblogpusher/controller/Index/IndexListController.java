/**
 * インデックスのワークテーブル一覧画面を担当するコントローラー
 *
 * 編集中のインデックスとカテゴリー階層を取得し、一覧画面へ渡す。
 */
package com.app.myblogpusher.controller.Index;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.service.Index.IndexEditService;
import com.app.myblogpusher.service.Index.IndexWorkService;

import jakarta.servlet.http.HttpSession;

@Controller
public class IndexListController {

	@Autowired
	private IndexWorkService indexWorkService;

	@Autowired
	private IndexEditService hugoIndexService;

	/**
	 * インデックス一覧画面を表示する。
	 */
	@GetMapping("/index/list")
	public String listIndexes(HttpSession session, Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login";
		}

		Long userId = loginUser.getUserId();

		model.addAttribute(
				"indexWorks",
				indexWorkService.findByUserId(userId));
		model.addAttribute(
				"categoryPaths",
				hugoIndexService.findCategoryPaths(userId));

		return "index/index_list";
	}
}
