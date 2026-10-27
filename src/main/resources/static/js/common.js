
/**
 * 全画面で共通利用するJavaScript
 *
 * 戻るボタン、CSRFトークンの取得と送信など、
 * 画面共通の処理を担当する。
 */

/**
 * 戻るボタン：ホーム画面へ遷移する。
 */
const backButton = document.getElementById('backButton');

if (backButton) {
	backButton.addEventListener('click', () => {
		window.location.href = '/home';
	});
}

/**
 * Cookieから指定した名前の値を取得する。
 */
function getCookieValue(name) {
	const prefix = `${name}=`;
	const cookie = document.cookie
		.split('; ')
		.find(item => item.startsWith(prefix));

	if (!cookie) {
		return null;
	}

	return decodeURIComponent(cookie.substring(prefix.length));
}

/**
 * CSRFトークンを取得する。
 *
 * サーバー側フィルターが発行するCSRF用Cookieを参照する。
 */
function getCsrfToken() {
	return getCookieValue('XSRF-TOKEN');
}

/**
 * GET・HEAD・OPTIONS以外の同一オリジンリクエストに
 * CSRFトークンをHTTPヘッダーで付与する。
 */
function addCsrfHeader(url, headers) {
	const targetUrl = new URL(url, window.location.origin);
	const safeMethods = ['GET', 'HEAD', 'OPTIONS'];

	if (targetUrl.origin !== window.location.origin) {
		return headers;
	}

	const token = getCsrfToken();

	if (token) {
		headers.set('X-CSRF-TOKEN', token);
	}

	return headers;
}

/**
 * fetch() にCSRFトークン送信処理を追加する。
 */
const originalFetch = window.fetch.bind(window);

window.fetch = function(resource, options = {}) {
	const url = resource instanceof Request
		? resource.url
		: resource;

	const method = (
		options.method
		|| (resource instanceof Request ? resource.method : 'GET')
	).toUpperCase();

	const safeMethods = ['GET', 'HEAD', 'OPTIONS'];

	if (safeMethods.includes(method)) {
		return originalFetch(resource, options);
	}

	const headers = new Headers(
		options.headers
		|| (resource instanceof Request ? resource.headers : undefined)
	);

	addCsrfHeader(url, headers);

	return originalFetch(resource, {
		...options,
		headers
	});
};

/**
 * XMLHttpRequest にCSRFトークン送信処理を追加する。
 */
const originalXhrOpen = XMLHttpRequest.prototype.open;
const originalXhrSend = XMLHttpRequest.prototype.send;
const originalXhrSetRequestHeader =
	XMLHttpRequest.prototype.setRequestHeader;

XMLHttpRequest.prototype.open = function(method, url, ...args) {
	this._csrfMethod = method.toUpperCase();
	this._csrfUrl = url;

	return originalXhrOpen.call(this, method, url, ...args);
};

XMLHttpRequest.prototype.send = function(...args) {
	const safeMethods = ['GET', 'HEAD', 'OPTIONS'];

	if (!safeMethods.includes(this._csrfMethod)) {
		const token = getCsrfToken();

		if (token) {
			originalXhrSetRequestHeader.call(
				this, 'X-CSRF-TOKEN', token
			);
		}
	}

	return originalXhrSend.apply(this, args);
};

/**
 * HTMLフォーム送信時にCSRFトークンをhidden項目として追加する。
 */
document.addEventListener('submit', event => {
	const form = event.target;

	if (!(form instanceof HTMLFormElement)) {
		return;
	}

	const safeMethods = ['GET', 'DIALOG'];
	const method = (form.method || 'GET').toUpperCase();

	if (safeMethods.includes(method)) {
		return;
	}

	const token = getCsrfToken();

	if (!token) {
		event.preventDefault();
		console.error(
			'CSRFトークンを取得できません。フォーム送信を中止しました。'
		);
		return;
	}

	let input = form.querySelector('input[name="_csrf"]');

	if (!input) {
		input = document.createElement('input');
		input.type = 'hidden';
		input.name = '_csrf';
		form.appendChild(input);
	}

	input.value = token;
});
