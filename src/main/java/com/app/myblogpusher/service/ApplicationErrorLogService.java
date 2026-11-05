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

		saveErrorLog(errorLog);
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

		saveErrorLog(errorLog);
	}

	/**
	 * 非同期処理で発生した例外をDBへ記録する。
	 *
	 * HTTPリクエストを伴わない処理のため、
	 * 処理名をリクエストURI欄に記録する。
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public void recordAsync(
			Throwable exception,
			Long userId,
			String processName) {

		ApplicationErrorLog errorLog = new ApplicationErrorLog();

		errorLog.setUserId(userId);
		errorLog.setRequestUri(processName);
		errorLog.setHttpMethod("ASYNC");

		Throwable rootCause = getRootCause(exception);

		errorLog.setExceptionClass(rootCause.getClass().getName());
		errorLog.setErrorMessage(toErrorMessage(exception));
		errorLog.setStackTrace(toStackTrace(exception));
		errorLog.setCreateDate(LocalDateTime.now());

		saveErrorLog(errorLog);
	}

	/**
	 * 例外のスタックトレースを完全な文字列に変換する。
	 *
	 * 原因例外、すべてのスタックフレームを省略せずに記録する。
	 */
	private String toStackTrace(Throwable exception) {
	    StringBuilder trace = new StringBuilder();
	    Throwable cause = exception;

	    while (cause != null) {
	        if (trace.length() > 0) {
	            trace.append("\nCaused by: ");
	        }

	        trace.append(cause.getClass().getName());

	        if (cause.getMessage() != null) {
	            trace.append(": ").append(cause.getMessage());
	        }

	        for (StackTraceElement element : cause.getStackTrace()) {
	            trace.append("\n    at ").append(element);
	        }

	        cause = cause.getCause();
	    }

	    return trace.toString();
	}

	/**
	 * 例外のクラス名とメッセージを取得する。
	 *
	 * 例外メッセージは加工・省略せずに記録する。
	 */
	private String toErrorMessage(Throwable exception) {
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

			cause = cause.getCause();
		}

		return true;
	}
	
	/**
	 * エラーログをDBへ保存する。
	 *
	 * DB保存に失敗した場合は標準エラーログへ出力し、
	 * 保存失敗の例外を呼び出し元へ伝播させる。
	 */
	private void saveErrorLog(ApplicationErrorLog errorLog) {
	    try {
	        errorLogRepository.save(errorLog);
	    } catch (RuntimeException e) {
	        System.err.println("[ApplicationErrorLog] DBへの保存に失敗しました。");
	        e.printStackTrace();
	        throw e;
	    }
	}
}