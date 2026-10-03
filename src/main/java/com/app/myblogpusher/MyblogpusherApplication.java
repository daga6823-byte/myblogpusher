/**
 * Myblogpusherアプリケーションの起動クラス
 *
 * Spring Bootアプリケーションの起動と、
 * アプリケーション全体での非同期処理を有効化する。
 */

package com.app.myblogpusher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MyblogpusherApplication {
	public static void main(String[] args) {
		SpringApplication.run(MyblogpusherApplication.class, args);
	}
}