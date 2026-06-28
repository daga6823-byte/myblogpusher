package com.app.myblogpusher.controller.Article;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import com.app.myblogpusher.service.SupabaseStorageService;

/**
 * 画像機能を担当するコントローラー
 * Supabase Storage から画像一覧を取得
 */
@Controller
public class ArticleImageController {

	@Autowired
	private SupabaseStorageService supabaseStorageService;

	/**
	 * Supabase Storage から画像一覧をJSON で返す
	 */
	@GetMapping("/article/images")
	@ResponseBody
	public List<String> getImages() {
		return supabaseStorageService.listImages();
	}
}