/**
 * TOTP認証ライブラリのBean設定を担当するConfiguration
 *
 * totp-spring-boot-starterの自動設定が利用できない環境でも、
 * TOTP秘密鍵生成、QRコード生成、認証コード検証に必要なBeanを登録する。
 */
package com.app.myblogpusher.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import dev.samstevens.totp.code.CodeGenerator;
import dev.samstevens.totp.code.CodeVerifier;
import dev.samstevens.totp.code.DefaultCodeGenerator;
import dev.samstevens.totp.code.DefaultCodeVerifier;
import dev.samstevens.totp.code.HashingAlgorithm;
import dev.samstevens.totp.qr.QrDataFactory;
import dev.samstevens.totp.qr.QrGenerator;
import dev.samstevens.totp.qr.ZxingPngQrGenerator;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.secret.SecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import dev.samstevens.totp.time.TimeProvider;

@Configuration
public class TotpConfig {

	@Bean
	public SecretGenerator secretGenerator() {
		return new DefaultSecretGenerator();
	}

	@Bean
	public TimeProvider timeProvider() {
		return new SystemTimeProvider();
	}

	@Bean
	public HashingAlgorithm hashingAlgorithm() {
		return HashingAlgorithm.SHA1;
	}

	@Bean
	public QrDataFactory qrDataFactory(
			HashingAlgorithm hashingAlgorithm) {

		return new QrDataFactory(
				hashingAlgorithm,
				6,
				30);
	}

	@Bean
	public QrGenerator qrGenerator() {
		return new ZxingPngQrGenerator();
	}

	@Bean
	public CodeGenerator codeGenerator(
			HashingAlgorithm hashingAlgorithm) {

		return new DefaultCodeGenerator(
				hashingAlgorithm,
				6);
	}

	@Bean
	public CodeVerifier codeVerifier(
			CodeGenerator codeGenerator,
			TimeProvider timeProvider) {

		DefaultCodeVerifier verifier = new DefaultCodeVerifier(
				codeGenerator,
				timeProvider);

		verifier.setTimePeriod(30);
		verifier.setAllowedTimePeriodDiscrepancy(1);

		return verifier;
	}
}