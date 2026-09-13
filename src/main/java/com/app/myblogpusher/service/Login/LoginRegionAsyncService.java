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

			String region = ipGeolocationService.findCountryCode(ipAddress);

			if (region != null && !region.isBlank()) {
				loginHistoryService.updateRegion(historyId, region);
			}
		});
	}
}