/**
 * 英単語辞典の画面操作を担当するコントローラー
 *
 * 辞典の一覧表示、新規登録・編集フォームの表示、
 * 登録・更新・削除、および登録元画面への遷移を管理する。
 * 辞典データの検索・保存は EnglishDictionaryRepository に委譲する。
 */
package com.app.myblogpusher.controller;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.app.myblogpusher.dto.EnglishDictionaryForm;
import com.app.myblogpusher.entity.EnglishDictionary;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.repository.EnglishDictionaryRepository;

import jakarta.servlet.http.HttpSession;

@Controller
public class EnglishDictionaryController {

	private final EnglishDictionaryRepository englishDictionaryRepository;

	public EnglishDictionaryController(EnglishDictionaryRepository englishDictionaryRepository) {
		this.englishDictionaryRepository = englishDictionaryRepository;
	}

	/**
	 * 共通辞典の一覧を表示する。
	 *
	 * @param model 画面に渡すデータ
	 * @return 英単語辞典一覧画面
	 */
	@GetMapping("/english-dictionary/list")
	public String list(Model model) {
		List<EnglishDictionary> dictionaries = englishDictionaryRepository.findByUserIdIsNull();
		model.addAttribute("dictionaries", dictionaries);
		return "english_dictionary_list";
	}

	/**
	 * 新規登録フォームまたは編集フォームを表示する。
	 *
	 * @param id 編集対象の辞典ID
	 * @param returnTo 登録後の戻り先
	 * @param workId 投稿確認対象の記事作業ID
	 * @param session セッション情報
	 * @param model 画面に渡すデータ
	 * @return 英単語辞典登録・編集画面、または一覧画面へのリダイレクト
	 */
	@GetMapping("/english-dictionary/form")
	public String form(
			@RequestParam(required = false) Long id,
			@RequestParam(required = false) String returnTo,
			@RequestParam(required = false) Long workId,
			HttpSession session,
			Model model) {

		// ログインユーザーの管理者権限を確認する。
		UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
		if (loginUser == null || !Integer.valueOf(2).equals(loginUser.getRole())) {
			return "redirect:/english-dictionary/list";
		}

		// 投稿確認画面から遷移した場合、登録後の戻り先を保存する。
		if ("publish_preview".equals(returnTo)) {
			session.setAttribute("dictionaryReturnTo", returnTo);

			if (workId != null) {
				session.setAttribute("previewWorkId", workId);
			}
		}

		// 新規登録または編集フォームを準備する。
		EnglishDictionaryForm form = new EnglishDictionaryForm();

		if (id != null) {
			// 編集対象を共通辞典に限定する。
			EnglishDictionary dict = englishDictionaryRepository
					.findByIdAndUserIdIsNull(id)
					.orElseThrow();

			form.setId(dict.getId());
			form.setJapanese(dict.getJapanese());
			form.setEnglish(dict.getEnglish());
		}

		model.addAttribute("form", form);
		return "english_dictionary_form";
	}

	/**
	 * 共通辞典を新規登録または更新する。
	 *
	 * @param form 登録・更新する辞典情報
	 * @param returnTo 登録後の戻り先
	 * @param session セッション情報
	 * @return 登録元画面または辞典一覧画面へのリダイレクト
	 */
	@PostMapping("/english-dictionary/save")
	public String save(
	        @ModelAttribute("form") EnglishDictionaryForm form,
	        @RequestParam(required = false) String returnTo,
	        HttpSession session) {

	    // ログインユーザーの管理者権限を確認する。
	    UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
	    if (loginUser == null || !Integer.valueOf(2).equals(loginUser.getRole())) {
	        return "redirect:/english-dictionary/list";
	    }

	    Long userId = loginUser.getUserId();
	    LocalDateTime now = LocalDateTime.now();

	    // 共通辞典の更新、または新規登録を行う。
	    EnglishDictionary dictionary;

	    if (form.getId() != null) {
	        // 更新対象が共通辞典であることを確認する。
	        dictionary = englishDictionaryRepository
	                .findByIdAndUserIdIsNull(form.getId())
	                .orElseThrow();

	        dictionary.setEnglish(form.getEnglish());
	        dictionary.setUpdateDate(now);
	        dictionary.setUpdateUser(userId);
	    } else {
	        // 新規データを共通辞典として登録する。
	        dictionary = new EnglishDictionary();
	        dictionary.setUserId(null);
	        dictionary.setJapanese(form.getJapanese());
	        dictionary.setEnglish(form.getEnglish());
	        dictionary.setCreateDate(now);
	        dictionary.setCreateUser(userId);
	        dictionary.setUpdateDate(now);
	        dictionary.setUpdateUser(userId);
	    }

	    englishDictionaryRepository.save(dictionary);

	    // 登録元画面の情報を確認する。
	    String savedReturnTo = (String) session.getAttribute("dictionaryReturnTo");

	    if ("publish_preview".equals(returnTo)
	            || "publish_preview".equals(savedReturnTo)) {

	        session.removeAttribute("dictionaryReturnTo");
	        return "redirect:/publish/preview/back";
	    }

	    return "redirect:/english-dictionary/list?saved";
	}

	/**
	 * 共通辞典を削除する。
	 *
	 * @param id 削除対象の辞典ID
	 * @param session セッション情報
	 * @return 英単語辞典一覧画面へのリダイレクト
	 */
	@PostMapping("/english-dictionary/delete")
	public String delete(
	        @RequestParam Long id,
	        HttpSession session) {

	    // ログインユーザーの管理者権限を確認する。
	    UserMaster loginUser = (UserMaster) session.getAttribute("loginUser");
	    if (loginUser == null || !Integer.valueOf(2).equals(loginUser.getRole())) {
	        return "redirect:/english-dictionary/list";
	    }

	    // 共通辞典であることを確認してから削除する。
	    EnglishDictionary dictionary = englishDictionaryRepository
	            .findByIdAndUserIdIsNull(id)
	            .orElseThrow();

	    englishDictionaryRepository.delete(dictionary);
	    return "redirect:/english-dictionary/list";
	}
}