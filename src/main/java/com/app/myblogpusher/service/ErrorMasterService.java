package com.app.myblogpusher.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.ErrorMaster;
import com.app.myblogpusher.repository.ErrorMasterRepository;

@Service
public class ErrorMasterService {

	@Autowired
	private ErrorMasterRepository errorMasterRepository;

	/**
	 * エラーコードから有効なエラーマスターを取得する。
	 *
	 * @param errorCode エラーコード
	 * @return エラーマスター
	 * @throws IllegalArgumentException エラーコードが未登録の場合
	 */
	public ErrorMaster findByErrorCode(String errorCode) {

		return errorMasterRepository
				.findByErrorCodeAndEnabledTrue(errorCode)
				.orElseThrow(() -> new IllegalArgumentException(
						"エラーコードがエラーマスターに登録されていません: "
								+ errorCode));
	}
}