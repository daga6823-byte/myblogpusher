
/**
 * CSRF攻撃対策とリクエスト処理の診断ログを担当する共通フィルター
 *
 * セッション単位でCSRFトークンを管理し、更新系リクエストの
 * トークンを検証する。
 * リクエストの到達状況、検証結果、レスポンスの状態を記録する。
 *
 * フィルターの登録はWebConfigで明示的に行う。
 */
package com.app.myblogpusher.filter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

public class CsrfProtectionFilter extends OncePerRequestFilter {

    public static final String CSRF_TOKEN_SESSION_ATTRIBUTE = "csrfToken";
    public static final String CSRF_TOKEN_REQUEST_ATTRIBUTE = "_csrf";

    private static final String CSRF_TOKEN_COOKIE = "XSRF-TOKEN";
    private static final String CSRF_TOKEN_PARAMETER = "_csrf";
    private static final String CSRF_TOKEN_HEADER = "X-CSRF-TOKEN";

    private static final String[] SAFE_METHODS = {
            "GET", "HEAD", "OPTIONS"
    };

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {

        String method = request.getMethod();
        String uri = request.getRequestURI();

        // フィルターに到達したすべてのリクエストを記録する。
        logRequest("REQUEST", method, uri, response);

        if ("TRACE".equalsIgnoreCase(method)) {
            response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            logRequest("REJECTED: TRACE method", method, uri, response);
            return;
        }

        boolean safeMethod = isSafeMethod(method);
        HttpSession session = request.getSession(safeMethod);
        String expectedToken = null;

        if (session != null) {
            expectedToken = (String) session.getAttribute(
                    CSRF_TOKEN_SESSION_ATTRIBUTE);

            if (expectedToken == null) {
                expectedToken = generateToken();
                session.setAttribute(
                        CSRF_TOKEN_SESSION_ATTRIBUTE, expectedToken);
            }

            request.setAttribute(
                    CSRF_TOKEN_REQUEST_ATTRIBUTE, expectedToken);

            Cookie csrfCookie = new Cookie(
                    CSRF_TOKEN_COOKIE, expectedToken);
            csrfCookie.setHttpOnly(false);
            csrfCookie.setSecure(request.isSecure());
            csrfCookie.setPath("/");
            csrfCookie.setAttribute("SameSite", "Lax");
            response.addCookie(csrfCookie);
        }

        if (!safeMethod) {
            if (session == null) {
                response.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "CSRF token is missing");
                logRequest("REJECTED: no session", method, uri, response);
                return;
            }

            String submittedToken = request.getHeader(CSRF_TOKEN_HEADER);
            String tokenSource = "header";

            if (submittedToken == null || submittedToken.isBlank()) {
                submittedToken = request.getParameter(
                        CSRF_TOKEN_PARAMETER);
                tokenSource = "parameter";
            }
            
            Cookie[] cookies = request.getCookies();
            String cookieToken = null;

            if (cookies != null) {
                for (Cookie cookie : cookies) {
                    if (CSRF_TOKEN_COOKIE.equals(cookie.getName())) {
                        cookieToken = cookie.getValue();
                        break;
                    }
                }
            }

            System.out.println(
                    "[CsrfProtectionFilter] TOKEN CHECK"
                    + " sessionExists=" + (session != null)
                    + " sessionTokenExists=" + (expectedToken != null)
                    + " submittedTokenExists="
                            + (submittedToken != null && !submittedToken.isBlank())
                    + " cookieMatchesSession="
                            + isValidToken(expectedToken, cookieToken)
                    + " submittedMatchesCookie="
                            + isValidToken(cookieToken, submittedToken));

            if (!isValidToken(expectedToken, submittedToken)) {
                response.sendError(
                        HttpServletResponse.SC_FORBIDDEN,
                        "Invalid CSRF token");

                // トークン値は記録せず、検証結果だけを記録する。
                logRequest(
                        "REJECTED: invalid CSRF token; source="
                                + tokenSource,
                        method, uri, response);
                return;
            }

            logRequest(
                    "CSRF validation passed; source=" + tokenSource,
                    method, uri, response);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // コントローラーやインターセプターの処理後の状態を記録する。
            logRequest("RESPONSE", method, uri, response);
        }
    }

    /**
     * リクエストの状態を記録する。セッションIDやCSRFトークンは記録しない。
     */
    private void logRequest(
            String event,
            String method,
            String uri,
            HttpServletResponse response) {

        System.out.println(
                "[CsrfProtectionFilter] "
                + event
                + " method=" + method
                + " uri=" + uri
                + " status=" + response.getStatus()
                + " location=" + response.getHeader("Location"));
    }

    /**
     * 更新処理を伴わないHTTPメソッドか判定する。
     */
    private boolean isSafeMethod(String method) {
        for (String safeMethod : SAFE_METHODS) {
            if (safeMethod.equalsIgnoreCase(method)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 暗号学的に安全な乱数からCSRFトークンを生成する。
     */
    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    /**
     * セッションのトークンと送信されたトークンを安全に比較する。
     */
    private boolean isValidToken(
            String expectedToken,
            String submittedToken) {

        if (expectedToken == null
                || submittedToken == null
                || submittedToken.isBlank()) {
            return false;
        }

        return MessageDigest.isEqual(
                expectedToken.getBytes(StandardCharsets.UTF_8),
                submittedToken.getBytes(StandardCharsets.UTF_8));
    }
}
