/**
 * Supabase Storageへの画像アップロード・一覧取得・URL生成を担当するサービス
 * listAllFilePathsはフォルダを再帰的に辿り、バケット内の全ファイルパスを取得する
 * （image_asset未登録の既存画像をインポートする際に使用）
 */

package com.app.myblogpusher.service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class SupabaseStorageService {

	@Value("${supabase.url}")
	private String supabaseUrl;

	@Value("${supabase.key}")
	private String supabaseKey;

	@Value("${supabase.storage.bucket}")
	private String bucketName;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	public List<String> listImages() {
		String url = supabaseUrl + "/storage/v1/object/list/" + bucketName;

		HttpHeaders headers = new HttpHeaders();
		headers.set("apikey", supabaseKey);
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<Void> entity = new HttpEntity<>(headers);

		try {
			String response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
			JsonNode root = objectMapper.readTree(response);
			List<String> images = new ArrayList<>();
			if (root.isArray()) {
				for (JsonNode node : root) {
					String fileName = node.get("name").asText();
					String imageUrl = getImageUrl(fileName);
					images.add(imageUrl);
				}
			}
			return images;
		} catch (Exception e) {
			throw new RuntimeException("Failed to list images", e);
		}
	}

	/**
	 * 画像をSupabase Storageの指定フォルダにアップロードする
	 * 戻り値はバケット内の相対パス（folderName/fileName）
	 */
	/**
	 * 変換後画像（File）をSupabase Storageへアップロードする
	 *
	 * HEIC→WebP変換後などMultipartFileではない画像を登録する場合に使用
	 */
	public String uploadImage(
			File file,
			String folderName) throws IOException {

		String fileName = file.getName();

		String path = folderName + "/" + fileName;

		String url = supabaseUrl
				+ "/storage/v1/object/"
				+ bucketName
				+ "/"
				+ path;

		HttpHeaders headers = new HttpHeaders();
		headers.set("apikey", supabaseKey);
		headers.setContentType(
				MediaType.parseMediaType(
						Files.probeContentType(file.toPath())));

		HttpEntity<byte[]> entity = new HttpEntity<>(
				Files.readAllBytes(file.toPath()),
				headers);

		restTemplate.exchange(
				url,
				HttpMethod.POST,
				entity,
				String.class);

		return path;
	}

	public String getImageUrl(String fileName) {
		return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + fileName;
	}

	/**
	 * バケット内の全ファイルパス（フォルダ/ファイル名）を再帰的に取得する
	 * image_asset未登録の既存画像をインポートする際に使用する
	 */
	public List<String> listAllFilePaths() {
		List<String> result = new ArrayList<>();
		collectFilePaths("", result);
		return result;
	}

	private void collectFilePaths(String prefix, List<String> result) {
		for (JsonNode entry : listRaw(prefix)) {
			String name = entry.get("name").asText();
			boolean isFolder = entry.get("id") == null || entry.get("id").isNull();
			String path = prefix.isEmpty() ? name : prefix + "/" + name;

			if (isFolder) {
				collectFilePaths(path, result);
			} else {
				result.add(path);
			}
		}
	}

	private List<JsonNode> listRaw(String prefix) {
		String url = supabaseUrl + "/storage/v1/object/list/" + bucketName;

		HttpHeaders headers = new HttpHeaders();
		headers.set("apikey", supabaseKey);
		headers.setContentType(MediaType.APPLICATION_JSON);

		Map<String, Object> body = new HashMap<>();
		body.put("prefix", prefix);
		body.put("limit", 1000);
		body.put("offset", 0);
		body.put("sortBy", Map.of(
				"column", "name",
				"order", "asc"));

		HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

		try {
			String response = restTemplate.exchange(url, HttpMethod.POST, entity, String.class).getBody();
			System.out.println(response);
			JsonNode root = objectMapper.readTree(response);
			List<JsonNode> nodes = new ArrayList<>();
			if (root.isArray()) {
				root.forEach(nodes::add);
			}
			return nodes;
		} catch (Exception e) {
			throw new RuntimeException("Failed to list images at prefix: " + prefix, e);
		}
	}

	/**
	 * Storageから画像をダウンロードする
	 */
	public byte[] downloadImage(String storagePath) {

		String url = supabaseUrl +
				"/storage/v1/object/" +
				bucketName +
				"/" +
				storagePath;

		HttpHeaders headers = new HttpHeaders();
		headers.set("apikey", supabaseKey);

		HttpEntity<Void> entity = new HttpEntity<>(headers);

		return restTemplate.exchange(
				url,
				HttpMethod.GET,
				entity,
				byte[].class)
				.getBody();

	}

	/**
	 * Storageから画像を削除する
	 */
	public void deleteImage(String storagePath) {

		String url = supabaseUrl +
				"/storage/v1/object/" +
				bucketName +
				"/" +
				storagePath;

		HttpHeaders headers = new HttpHeaders();
		headers.set("apikey", supabaseKey);

		HttpEntity<Void> entity = new HttpEntity<>(headers);

		restTemplate.exchange(
				url,
				HttpMethod.DELETE,
				entity,
				String.class);

	}

	/**
	 * 画像を別フォルダへ移動する
	 *
	 * ダウンロード
	 * → 新フォルダへアップロード
	 * → 元画像削除
	 *
	 * 戻り値：新しいstoragePath
	 */
	public String moveImage(
			String oldPath,
			String newFolderName) {

		byte[] image = downloadImage(oldPath);

		String fileName = oldPath.substring(
				oldPath.lastIndexOf('/') + 1);

		String newPath = newFolderName + "/" + fileName;

		String url = supabaseUrl +
				"/storage/v1/object/" +
				bucketName +
				"/" +
				newPath;

		HttpHeaders headers = new HttpHeaders();
		headers.set("apikey", supabaseKey);
		headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);

		HttpEntity<byte[]> entity = new HttpEntity<>(image, headers);

		restTemplate.exchange(
				url,
				HttpMethod.POST,
				entity,
				String.class);

		deleteImage(oldPath);

		return newPath;

	}
}