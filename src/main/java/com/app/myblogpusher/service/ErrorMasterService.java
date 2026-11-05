package com.app.myblogpusher.service;

import java.time.LocalDateTime;

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
	 * 未登録の場合は仮のエラーマスターを自動登録する。
	 *
	 * @param errorCode エラーコード
	 * @return エラーマスター
	 */
	public ErrorMaster findByErrorCode(String errorCode) {

	    return errorMasterRepository
	            .findByErrorCodeAndEnabledTrue(errorCode)
	            .orElseGet(() -> {
	                ErrorMaster errorMaster = new ErrorMaster();

	                errorMaster.setErrorCode(errorCode);
	                errorMaster.setErrorName("未登録エラー: " + errorCode);
	                errorMaster.setErrorMessage(
	                        "予期しないエラーが発生しました。時間をおいて再度お試しください。");
	                errorMaster.setDescription("自動登録されたエラーです。内容を確認して正式なエラー定義を登録してください。");
	                errorMaster.setEnabled(true);
	                errorMaster.setCreateDate(LocalDateTime.now());

	                return errorMasterRepository.save(errorMaster);
	            });
	}
}