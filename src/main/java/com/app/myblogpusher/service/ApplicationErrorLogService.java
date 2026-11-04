/**
 * アプリケーションエラーログの記録を担当するサービス
 *
 * 例外情報とリクエスト情報をまとめ、
 * ApplicationErrorLogRepositoryを通じてDBへ保存する。
 */
package com.app.myblogpusher.service;

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
		errorLog.setErrorMessage(toErrorMessage(exception));
		errorLog.setStackTrace(toStackTrace(exception));
		errorLog.setCreateDate(LocalDateTime.now());

		errorLogRepository.save(errorLog);
	}

	/**
	 * 例外の発生箇所と原因を簡潔な文字列に変換する。
	 *
	 * アプリケーション内部のスタックフレームを最大5件記録し、
	 * Springなどのフレームワーク内部の情報は省略する。
	 * 原因例外も最大5階層まで記録する。
	 */
	private String toStackTrace(Exception exception) {
		StringBuilder trace = new StringBuilder();
		Throwable cause = exception;
		int causeCount = 0;

		while (cause != null && causeCount < 5) {
			if (causeCount == 0) {
				trace.append(cause.getClass().getName());
			} else {
				trace.append("\nCaused by: ")
						.append(cause.getClass().getName());
			}

			if (cause.getMessage() != null) {
				trace.append(": ").append(cause.getMessage());
			}

			int recordedFrames = 0;

			for (StackTraceElement element : cause.getStackTrace()) {
				if (!element.getClassName().startsWith("com.app.myblogpusher.")) {
					continue;
				}

				trace.append("\n    at ").append(element);

				recordedFrames++;
				if (recordedFrames >= 5) {
					break;
				}
			}

			if (recordedFrames == 0) {
				trace.append("\n    （アプリケーション内部の発生箇所なし）");
			}

			cause = cause.getCause();
			causeCount++;
		}

		return trace.toString();
	}

	/**
	 * 例外と原因例外のメッセージをまとめる。
	 *
	 * 原因例外は最大5階層まで取得し、全体を最大2,000文字に制限する。
	 */
	private String toErrorMessage(Exception exception) {
		StringBuilder message = new StringBuilder();
		Throwable cause = exception;
		int causeCount = 0;

		while (cause != null && causeCount < 5) {
			if (message.length() > 0) {
				message.append(" / ");
			}

			message.append(cause.getClass().getSimpleName());

			if (cause.getMessage() != null) {
				message.append(": ").append(cause.getMessage());
			}

			cause = cause.getCause();
			causeCount++;
		}

		return message.toString();
	}
}