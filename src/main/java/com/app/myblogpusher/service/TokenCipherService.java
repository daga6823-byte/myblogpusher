package com.app.myblogpusher.service;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class TokenCipherService {

	private static final String ALGORITHM = "AES/GCM/NoPadding";
	private static final int GCM_TAG_LENGTH_BITS = 128;
	private static final int IV_LENGTH_BYTES = 12;

	private final SecretKeySpec secretKey;

	public TokenCipherService(@Value("${app.token-encryption-key}") String base64Key) {
		byte[] keyBytes = Base64.getDecoder().decode(base64Key);
		this.secretKey = new SecretKeySpec(keyBytes, "AES");
	}

	/**
	 * 平文トークンを暗号化し、暗号文とIVをそれぞれBase64文字列で返す。
	 */
	public EncryptedToken encrypt(String plainToken) {
		try {
			byte[] iv = new byte[IV_LENGTH_BYTES];
			new SecureRandom().nextBytes(iv);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
			byte[] encrypted = cipher.doFinal(plainToken.getBytes(StandardCharsets.UTF_8));

			return new EncryptedToken(
					Base64.getEncoder().encodeToString(encrypted),
					Base64.getEncoder().encodeToString(iv));
		} catch (Exception e) {
			throw new IllegalStateException("トークンの暗号化に失敗しました", e);
		}
	}

	/**
	 * 暗号文とIV（いずれもBase64文字列）から平文トークンを復元する。
	 */
	public String decrypt(String encryptedBase64, String ivBase64) {
		try {
			byte[] iv = Base64.getDecoder().decode(ivBase64);
			byte[] encrypted = Base64.getDecoder().decode(encryptedBase64);

			Cipher cipher = Cipher.getInstance(ALGORITHM);
			cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv));
			byte[] decrypted = cipher.doFinal(encrypted);

			return new String(decrypted, StandardCharsets.UTF_8);
		} catch (Exception e) {
			throw new IllegalStateException("トークンの復号に失敗しました", e);
		}
	}

	public record EncryptedToken(String cipherText, String iv) {
	}

	public String generateNewKey() {
		byte[] keyBytes = new byte[32]; // 256bit
		new SecureRandom().nextBytes(keyBytes);
		return Base64.getEncoder().encodeToString(keyBytes);
	}
}