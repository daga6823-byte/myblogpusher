/**

* セキュリティ関連メールの送信を担当するService
*
* ユーザーのメールアドレスへログイン・2FAに関する
* セキュリティ通知を送信する。
*
* 送信先はUserMaster.emailを使用し、
* 送信元アドレスはapplication.propertiesから取得する。
  */
package com.app.myblogpusher.service;

import org.springframework.stereotype.Service;

@Service
public class SecurityMailService {
//
//	private final JavaMailSender mailSender;
//
//	private final String fromAddress;
//
//	public SecurityMailService(
//			JavaMailSender mailSender,
//			@Value("${app.mail.from}") String fromAddress) {
//
//		this.mailSender = mailSender;
//		this.fromAddress = fromAddress;
//	}
//
//	/**
//	 * 2FA認証に失敗したことを通知する。
//	 */
//	public void sendTwoFactorFailureNotice(
//			UserMaster user,
//			String ipAddress,
//			String region) {
//
//		if (user == null
//				|| user.getEmail() == null
//				|| user.getEmail().isBlank()) {
//			return;
//		}
//
//		SimpleMailMessage message = new SimpleMailMessage();
//
//		message.setFrom(fromAddress);
//		message.setTo(user.getEmail());
//		message.setSubject("【Myblogpusher】セキュリティ警告");
//		message.setText(
//				"Myblogpusherで2FA認証に失敗しました。\n\n"
//						+ "本人によるログインの場合は問題ありません。\n"
//						+ "身に覚えのない場合は、パスワードの変更などの対策を行ってください。\n\n"
//						+ "接続元IP: " + ipAddress + "\n"
//						+ "接続元地域: " + region);
//
//		mailSender.send(message);
	}
//}
