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
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.app.myblogpusher.entity.ApplicationErrorLog;
import com.app.myblogpusher.entity.UserMaster;
import com.app.myblogpusher.repository.ApplicationErrorLogRepository;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

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

		if (!shouldLog(exception, request)) {
			return;
		}

		ApplicationErrorLog errorLog = new ApplicationErrorLog();

		Object loginUser = request.getSession(false) != null
				? request.getSession(false).getAttribute("loginUser")
				: null;

		if (loginUser instanceof UserMaster user) {
			errorLog.setUserId(user.getUserId());
		}

		errorLog.setRequestUri(request.getRequestURI());
		errorLog.setHttpMethod(request.getMethod());
		Throwable rootCause = getRootCause(exception);

		errorLog.setExceptionClass(rootCause.getClass().getName());
		errorLog.setErrorMessage(toErrorMessage(exception));
		errorLog.setStackTrace(toStackTrace(exception));
		errorLog.setCreateDate(LocalDateTime.now());

		errorLogRepository.save(errorLog);
	}

	/**
	 * CSRF検証などのセキュリティ拒否情報をDBへ記録する。
	 *
	 * 例外が発生しない拒否処理も調査できるよう、
	 * リクエスト情報と拒否理由を独立したトランザクションで保存する。
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordSecurityRejection(
			String rejectionReason,
			HttpServletRequest request) {

		ApplicationErrorLog errorLog = new ApplicationErrorLog();

		HttpSession session = request.getSession(false);
		Object loginUser = session != null
				? session.getAttribute("loginUser")
				: null;

		if (loginUser instanceof UserMaster user) {
			errorLog.setUserId(user.getUserId());
		}

		errorLog.setRequestUri(request.getRequestURI());
		errorLog.setHttpMethod(request.getMethod());
		errorLog.setExceptionClass("SecurityRejection");
		errorLog.setErrorMessage(rejectionReason);
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
				String className = element.getClassName();

				if (!className.startsWith("com.app.myblogpusher.")
						|| className.contains(".filter.")
						|| className.contains(".handler.")) {
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
	 * 例外の内容を調査しやすい概要に変換する。
	 *
	 * Thymeleafのテンプレート解析エラーは、
	 * テンプレート名と行・列番号を抽出して記録する。
	 */
	private String toErrorMessage(Exception exception) {
		Throwable cause = exception;

		while (cause != null) {
			if (cause.getClass().getSimpleName().equals("TemplateInputException")) {
				String message = cause.getMessage();

				if (message != null) {
					int templateStart = message.indexOf("templates/");
					int templateEnd = message.indexOf("\"]", templateStart);
					int lineStart = message.indexOf("line ", templateEnd);
					int columnStart = message.indexOf(", col ", lineStart);

					if (templateStart >= 0 && templateEnd > templateStart
							&& lineStart >= 0 && columnStart > lineStart) {

						String template = message.substring(
								templateStart, templateEnd);

						String line = message.substring(
								lineStart + 5, columnStart);

						String column = message.substring(columnStart + 6)
								.replaceAll("[^0-9].*$", "");

						return "テンプレート解析エラー: "
								+ template + " (" + line + "行" + column + "列)";
					}
				}
			}

			cause = cause.getCause();
		}

		Throwable rootCause = getRootCause(exception);

		return rootCause.getClass().getSimpleName()
				+ (rootCause.getMessage() != null
						? ": " + rootCause.getMessage()
						: "");
	}

	/**
	 * 原因例外を取得する。
	 */
	private Throwable getRootCause(Throwable exception) {
		Throwable cause = exception;

		while (cause.getCause() != null && cause.getCause() != cause) {
			cause = cause.getCause();
		}

		return cause;
	}

	/**
	 * DBへの記録が不要な例外か判定する。
	 *
	 * ブラウザによるアイコン探索やリクエストパラメータ不足は
	 * DBのエラーログには記録しない。
	 */
	private boolean shouldLog(
			Exception exception,
			HttpServletRequest request) {

		Throwable cause = exception;

		while (cause != null) {

			if (cause instanceof NoResourceFoundException) {
				String uri = request.getRequestURI();

				if (uri.endsWith("/apple-touch-icon.png")
						|| uri.endsWith("/apple-touch-icon-precomposed.png")) {
					return false;
				}
			}

			if (cause instanceof MissingServletRequestParameterException) {
				return false;
			}

			cause = cause.getCause();
		}

		return true;
	}
}