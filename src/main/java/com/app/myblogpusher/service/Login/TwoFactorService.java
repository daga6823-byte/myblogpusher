/**
 * Google AuthenticatorによるTOTP認証を担当するService
 *
 * TOTP秘密鍵の生成、Google Authenticator登録用QRコードの生成、
 * 入力された6桁コードの検証を担当する。
 */

package com.app.myblogpusher.service.Login;

import java.util.Base64;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.qr.QrData;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.secret.SecretGenerator;

@Service
public class TwoFactorService {

	@Autowired
	private SecretGenerator secretGenerator;

	@Autowired
	private QrDataFactory qrDataFactory;

	@Autowired
	private QrGenerator qrGenerator;

	@Autowired
	private CodeVerifier codeVerifier;

	/**
	 * Google Authenticator用のTOTP秘密鍵を生成する。
	 *
	 * @return Base32形式の秘密鍵
	 */
	public String generateSecret() {
		return secretGenerator.generate();
	}

	/**
	 * Google Authenticator登録用QRコードを生成する。
	 *
	 * @param secret TOTP秘密鍵
	 * @param accountName アカウント名
	 * @return data URI形式のPNG画像
	 */
	public String generateQrCode(
			String secret,
			String accountName) {

		QrData data = qrDataFactory.newBuilder()
				.label(accountName)
				.secret(secret)
				.issuer("Myblogpusher")
				.build();

		try {
			byte[] image = qrGenerator.generate(data);

			String encoded = Base64.getEncoder().encodeToString(image);

			return "data:"
					+ qrGenerator.getImageMimeType()
					+ ";base64,"
					+ encoded;

		} catch (Exception e) {
			throw new IllegalStateException(
					"Google Authenticator用QRコードの生成に失敗しました",
					e);
		}
	}

	/**
	 * Google Authenticatorから入力された6桁コードを検証する。
	 *
	 * @param secret TOTP秘密鍵
	 * @param code 入力された6桁コード
	 * @return 正しい場合true
	 */
	public boolean verifyCode(
			String secret,
			String code) {

		if (secret == null || secret.isBlank()
				|| code == null || code.isBlank()) {
			return false;
		}

		return codeVerifier.isValidCode(secret, code);
	}
}