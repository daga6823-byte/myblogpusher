/**
 * Supabase Storage上のフォルダ単位の移動を担当するサービス
 *
 * フォルダ配下の画像をまとめて別フォルダへ移動し、
 * 途中で失敗した場合や後続のDB更新が失敗した場合は、移動済みのファイルを元のパスへ戻す。
 */
package com.app.myblogpusher.service.Supabase;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class SupabaseStorageFolderService {

	private final SupabaseStorageService storageService;
	private final SupabaseStorageMoveService moveService;

	/**
	 * フォルダ移動に必要な依存関係を設定する。
	 */
	public SupabaseStorageFolderService(
			SupabaseStorageService storageService,
			SupabaseStorageMoveService moveService) {
		this.storageService = storageService;
		this.moveService = moveService;
	}

	/**
	 * フォルダ配下の画像を別フォルダへ移動する。
	 *
	 * 変更先に同じ相対パスのファイルがある場合は、
	 * 上書きせずに処理を中止する。
	 *
	 * @return 移動前・移動後のStorageパスの対応表
	 */
	public Map<String, String> renameFolder(
			String oldFolderName,
			String newFolderName) {

		String oldPrefix = oldFolderName + "/";
		String newPrefix = newFolderName + "/";

		List<String> allPaths = storageService.listAllFilePaths();

		List<String> sourcePaths = allPaths.stream()
				.filter(path -> path.startsWith(oldPrefix))
				.toList();

		if (sourcePaths.isEmpty()) {
			throw new IllegalArgumentException(
					"変更対象のフォルダにファイルがありません");
		}

		// 同名ファイルが1つでもあれば、移動を開始する前に中止する。
		for (String oldPath : sourcePaths) {
			String relativePath = oldPath.substring(oldPrefix.length());
			String newPath = newPrefix + relativePath;

			if (allPaths.contains(newPath)) {
				throw new IllegalArgumentException(
						"変更先に同名ファイルが存在するため、統合できません: "
								+ relativePath);
			}
		}

		Map<String, String> movedPaths = new HashMap<>();

		try {
			for (String oldPath : sourcePaths) {
				String relativePath = oldPath.substring(oldPrefix.length());
				String newPath = newPrefix + relativePath;

				moveService.moveStoragePath(oldPath, newPath);
				movedPaths.put(oldPath, newPath);
			}

			return movedPaths;

		} catch (Exception e) {
			try {
				restoreMovedPaths(movedPaths);
			} catch (Exception rollbackException) {
				e.addSuppressed(rollbackException);
			}

			throw new RuntimeException(
					"フォルダの統合に失敗しました", e);
		}
	}

	/**
	 * フォルダ移動後にDB更新が失敗した場合、
	 * 移動済みのファイルだけを元のパスへ戻す。
	 */
	public void restoreMovedPaths(Map<String, String> movedPaths) {
		List<Map.Entry<String, String>> entries = new ArrayList<>(movedPaths.entrySet());

		for (int i = entries.size() - 1; i >= 0; i--) {
			Map.Entry<String, String> entry = entries.get(i);

			moveService.moveStoragePath(entry.getValue(), entry.getKey());
		}
	}
}