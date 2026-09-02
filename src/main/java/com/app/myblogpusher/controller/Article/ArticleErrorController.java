/**
 * 投稿エラー記事の確認画面を担当するController
 *
 * ArticleWork.status = 2 の記事を取得し、
 * エラー確認画面へ渡す。
 */

package com.app.myblogpusher.controller.Article;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.Article.ArticleWork;
import com.app.myblogpusher.service.Article.ArticleWorkService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ArticleErrorController {

	@Autowired
	private ArticleWorkService articleWorkService;

	/**
	 * 投稿エラー記事の確認画面を表示する
	 */
	@GetMapping("/article/error")
	public String showErrorPage(
			HttpSession session,
			Model model) {

		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");

		if (loginUser == null) {
			return "redirect:/login";
		}

		List<ArticleWork> errorWorks = articleWorkService.findError(
				loginUser.getUserId());

		model.addAttribute(
				"errorWorks",
				errorWorks);

		return "article/error";
	}
}