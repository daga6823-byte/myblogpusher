/**
 * ログイン元IPのregionを非同期で取得するService
 *
 * ログイン処理では外部APIの応答を待たず、
 * バックグラウンドでIPinfoから国コードを取得して
 * 対象のログイン履歴へ反映する。
 */
package com.app.myblogpusher.service.Login;

import java.util.concurrent.CompletableFuture;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LoginRegionAsyncService {

	@Autowired
	private IpGeolocationService ipGeolocationService;

	@Autowired
	private LoginHistoryService loginHistoryService;

	/**
	 * ログイン元IPのregion取得を非同期で実行する。
	 */
	public void updateRegionAsync(Long historyId, String ipAddress) {

		CompletableFuture.runAsync(() -> {

			long start = System.nanoTime();

			try {
				System.out.println(
						"[LoginRegionTiming] 国コード取得開始: historyId="
								+ historyId);

				String region = ipGeolocationService.findCountryCode(ipAddress);

				long elapsedMillis = (System.nanoTime() - start) / 1_000_000;

				System.out.println(
						"[LoginRegionTiming] 国コード取得結果: region="
								+ region + ", elapsed=" + elapsedMillis + " ms");

				if (region != null && !region.isBlank()) {
					loginHistoryService.updateRegion(historyId, region);

					System.out.println(
							"[LoginRegionTiming] DB更新呼び出し完了: historyId="
									+ historyId + ", region=" + region);
				} else {
					System.err.println(
							"[LoginRegionTiming] 国コードを取得できませんでした: historyId="
									+ historyId);
				}

			} catch (Exception e) {
				System.err.println(
						"[LoginRegionTiming] 地域情報の更新に失敗しました: historyId="
								+ historyId + ", error=" + e.getMessage());
				e.printStackTrace();
			}
		});
	}
}