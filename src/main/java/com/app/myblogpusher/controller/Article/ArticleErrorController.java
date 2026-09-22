/**
 * 投稿エラー記事の確認画面を担当するController
 *
 * ArticleWork.status = 2 の記事を取得し、
 * エラー確認画面へ渡す。
 */

package com.app.myblogpusher.controller.Article;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.app.myblogpusher.dto.ArticleErrorView;
import com.app.myblogpusher.entity.ErrorMaster;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.Article.ArticleWork;
import com.app.myblogpusher.service.ErrorMasterService;
import com.app.myblogpusher.service.Article.ArticleWorkService;

import jakarta.servlet.http.HttpSession;

@Controller
public class ArticleErrorController {

	@Autowired
	private ArticleWorkService articleWorkService;

	@Autowired
	private ErrorMasterService errorMasterService;

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

		List<ArticleErrorView> errorViews = errorWorks.stream()
				.map(work -> {
					ErrorMaster errorMaster = errorMasterService.findByErrorCode(
							work.getErrorCode());

					return new ArticleErrorView(
							work.getWorkId(),
							work.getTitle(),
							errorMaster.getErrorMessage());
				})
				.collect(Collectors.toList());

		model.addAttribute(
				"errorWorks",
				errorViews);

		return "article/error";
	}
}