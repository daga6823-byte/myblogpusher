/**
 * Supabase Storageへの画像アップロード・一覧取得・URL生成を担当するサービス
 * listAllFilePathsはフォルダを再帰的に辿り、バケット内の全ファイルパスを取得する
 * （image_asset未登録の既存画像をインポートする際に使用）
 */

package com.app.myblogpusher.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

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
	public String uploadImage(MultipartFile file, String folderName) throws IOException {
		String fileName = file.getOriginalFilename();
		String path = folderName + "/" + fileName;
		String url = supabaseUrl + "/storage/v1/object/" + bucketName + "/" + path;

		HttpHeaders headers = new HttpHeaders();
		headers.set("apikey", supabaseKey);
		MediaType contentType = file.getContentType() != null
				? MediaType.parseMediaType(file.getContentType())
				: MediaType.APPLICATION_OCTET_STREAM;
		headers.setContentType(contentType);

		HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);

		restTemplate.exchange(url, HttpMethod.POST, entity, String.class);

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

		String body = "{\"prefix\":\"" + prefix + "\",\"limit\":1000,\"offset\":0,"
				+ "\"sortBy\":{\"column\":\"name\",\"order\":\"asc\"}}";

		HttpEntity<String> entity = new HttpEntity<>(body, headers);

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
}