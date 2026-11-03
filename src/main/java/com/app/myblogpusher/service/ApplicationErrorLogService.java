/**
 * アプリケーションエラーログの記録を担当するサービス
 *
 * 例外情報とリクエスト情報をまとめ、
 * ApplicationErrorLogRepositoryを通じてDBへ保存する。
 */
package com.app.myblogpusher.service;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.app.myblogpusher.entity.ApplicationErrorLog;
import com.app.myblogpusher.repository.ApplicationErrorLogRepository;

import jakarta.servlet.http.HttpServletRequest;

@Service
public class ApplicationErrorLogService {

	private final ApplicationErrorLogRepository errorLogRepository;

	public ApplicationErrorLogService(
			ApplicationErrorLogRepository errorLogRepository) {
		this.errorLogRepository = errorLogRepository;
	}

	/**
	 * 発生した例外とリクエスト情報をDBへ記録する。
	 *
	 * 既存処理のトランザクション状態に影響されず、
	 * エラーログを独立したトランザクションで保存する。
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void record(
			Exception exception,
			HttpServletRequest request) {

		ApplicationErrorLog errorLog = new ApplicationErrorLog();

		Object loginUser = request.getSession(false) != null
				? request.getSession(false).getAttribute("loginUser")
				: null;

		if (loginUser instanceof com.app.myblogpusher.entity.UserMaster user) {
			errorLog.setUserId(user.getUserId());
		}

		errorLog.setRequestUri(request.getRequestURI());
		errorLog.setHttpMethod(request.getMethod());
		errorLog.setExceptionClass(exception.getClass().getName());
		errorLog.setErrorMessage(exception.getMessage());
		errorLog.setStackTrace(toStackTrace(exception));
		errorLog.setCreateDate(LocalDateTime.now());

		errorLogRepository.save(errorLog);
	}

	/**
	 * 例外のスタックトレースを文字列に変換する。
	 */
	private String toStackTrace(Exception exception) {
		StringWriter writer = new StringWriter();
		exception.printStackTrace(new PrintWriter(writer));
		return writer.toString();
	}
}