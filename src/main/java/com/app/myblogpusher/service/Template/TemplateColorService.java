/**
 * テンプレートの色履歴管理を担当するサービス
 *
 * ユーザーが使用したカラーコードを登録・取得する。
 * 同じ色の重複登録を防止する。
 */
package com.app.myblogpusher.service.Template;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.Template.TemplateColor;
import com.app.myblogpusher.repository.Template.TemplateColorRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateColorService {

	private final TemplateColorRepository templateColorRepository;

	/**
	 * ユーザーの色履歴を新しい順に取得する。
	 */
	@Transactional(readOnly = true)
	public List<TemplateColor> getColors(Long userId) {
		return templateColorRepository.findByUserIdOrderByCreateDateDesc(userId);
	}

	/**
	 * 使用したカラーコードを履歴に登録する。
	 */
	public void addColor(Long userId, String colorCode) {

		if (templateColorRepository.existsByUserIdAndColorCode(userId, colorCode)) {
			return;
		}

		TemplateColor color = new TemplateColor();
		color.setUserId(userId);
		color.setColorCode(colorCode);
		color.setCreateDate(LocalDateTime.now());

		templateColorRepository.save(color);
	}
}