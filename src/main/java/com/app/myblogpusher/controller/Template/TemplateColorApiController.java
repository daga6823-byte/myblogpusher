
/**
 * テンプレート色履歴のJSON APIを担当するコントローラー
 *
 * ログインユーザーが過去に使用した色の一覧取得を担当する。
 * 色履歴の取得処理はTemplateColorServiceへ委譲する。
 */
package com.app.myblogpusher.controller.Template;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.entity.Template.TemplateColor;
import com.app.myblogpusher.service.Template.TemplateColorService;
import com.app.myblogpusher.util.LoginUserUtil;

import jakarta.servlet.http.HttpSession;

@RestController
public class TemplateColorApiController {

	private final TemplateColorService templateColorService;

	/**
	 * テンプレート色履歴APIに必要な依存関係を設定する。
	 */
	public TemplateColorApiController(
			TemplateColorService templateColorService) {
		this.templateColorService = templateColorService;
	}

	/**
	 * ログインユーザーの最近使用した色を取得する。
	 */
	@GetMapping("/template/color/history")
	public ResponseEntity<List<TemplateColor>> getColorHistory(
			HttpSession session) {

		UserMaster loginUser = LoginUserUtil.getLoginUser(session);

		if (loginUser == null) {
			return ResponseEntity.status(401).build();
		}

		List<TemplateColor> colors = templateColorService.getColors(
				loginUser.getUserId());

		return ResponseEntity.ok(colors);
	}

	/**
	 * 指定された色をユーザーの色履歴に登録する。
	 *
	 * 登録済みの色は既存IDを再利用し、
	 * 未登録の色は新規登録する。
	 */
	@PostMapping("/template/color/save")
	public ResponseEntity<Map<String, Long>> saveColor(
			@RequestParam String colorValue,
			HttpSession session) {

		UserMaster loginUser = LoginUserUtil.getLoginUser(session);

		if (loginUser == null) {
			return ResponseEntity.status(401).build();
		}

		if (colorValue == null || colorValue.isBlank()) {
			return ResponseEntity.badRequest().build();
		}

		templateColorService.addColor(
				loginUser.getUserId(),
				colorValue);

		return ResponseEntity.ok(Map.of("saved", 1L));
	}
}
