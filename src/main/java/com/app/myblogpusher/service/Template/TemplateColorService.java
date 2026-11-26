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

import com.app.myblogpusher.entity.Template.TemplateColorEntity;
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
	public List<TemplateColorEntity> getColors(Long userId) {
		return templateColorRepository.findByUserIdOrderByCreateDateDesc(userId);
	}

	/**
	 * 使用したカラーコードを履歴に登録する。
	 *
	 * 色コードは大文字に統一し、同一ユーザーの重複登録を防ぐ。
	 */
	public void addColor(Long userId, String colorCode) {

		if (colorCode == null || colorCode.isBlank()) {
			return;
		}

		String normalizedColorCode = colorCode.trim().toUpperCase();

		if (templateColorRepository.existsByUserIdAndColorCodeIgnoreCase(
		        userId, normalizedColorCode)) {
		    return;
		}

		TemplateColorEntity color = new TemplateColorEntity();

		color.setUserId(userId);
		color.setColorCode(normalizedColorCode);
		color.setCreateDate(LocalDateTime.now());

		templateColorRepository.save(color);
	}
}