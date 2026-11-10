/**
 * Supabase Storage APIとのHTTP通信を担当するクライアント
 *
 * 接続設定、URLの組み立て、apikeyヘッダーの付与、各APIの呼び出しを一箇所にまとめる。
 * どのファイルをどう扱うかの判断は、呼び出し元のサービスが担当する。
 */
package com.app.myblogpusher.service.Supabase;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriUtils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class SupabaseStorageClient {

	private final String supabaseUrl;
	private final String supabaseKey;
	private final String bucketName;

	private final RestTemplate restTemplate = new RestTemplate();
	private final ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Supabaseの接続設定を受け取る。
	 */
	public SupabaseStorageClient(
			@Value("${supabase.url}") String supabaseUrl,
			@Value("${supabase.key}") String supabaseKey,
			@Value("${supabase.storage.bucket}") String bucketName) {
		this.supabaseUrl = supabaseUrl;
		this.supabaseKey = supabaseKey;
		this.bucketName = bucketName;
	}

	/**
	 * バケット直下の一覧をGETで取得する。
	 */
	public List<JsonNode> listRoot() {
		String url = supabaseUrl + "/storage/v1/object/list/" + bucketName;

		HttpHeaders headers = createHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);

		HttpEntity<Void> entity = new HttpEntity<>(headers);

		try {
			String response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class).getBody();
			return toNodes(response);
		} catch (Exception e) {
			throw new RuntimeException("Failed to list images", e);
		}
	}

	/**
	 * 指定プレフィックス直下のエントリ(ファイル・フォルダ)をPOSTで取得する。
	 */
	public List<JsonNode> list(String prefix) {
		String url = supabaseUrl + "/storage/v1/object/list/" + bucketName;

		HttpHeaders headers = createHeaders();
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
			return toNodes(response);
		} catch (Exception e) {
			throw new RuntimeException("Failed to list images at prefix: " + prefix, e);
		}
	}

	/**
	 * 指定パスへファイルを新規アップロードする。
	 * 5xxエラーの場合は60秒後に1回だけ再試行する。
	 * 409の場合は再試行せず、例外を呼び出し元へ送出する。
	 */
	public void uploadObject(String storagePath, byte[] data, MediaType contentType) {
	    HttpHeaders headers = createHeaders();
	    headers.setContentType(contentType);

	    HttpEntity<byte[]> entity = new HttpEntity<>(data, headers);

	    try {
	        restTemplate.exchange(
	                objectUrl(storagePath),
	                HttpMethod.POST,
	                entity,
	                String.class);

	    } catch (HttpServerErrorException e) {
	        try {
	            Thread.sleep(60_000);
	        } catch (InterruptedException interruptedException) {
	            Thread.currentThread().interrupt();
	            throw new RuntimeException(
	                    "Supabase Storage upload retry was interrupted",
	                    interruptedException);
	        }

	        restTemplate.exchange(
	                objectUrl(storagePath),
	                HttpMethod.POST,
	                entity,
	                String.class);
	    }
	}

	/**
	 * 指定パスのファイルを同じパスのまま上書きする。
	 */
	public void upsert(String storagePath, byte[] data, MediaType contentType) {
		HttpHeaders headers = createHeaders();
		headers.set("x-upsert", "true");
		headers.setContentType(contentType);

		HttpEntity<byte[]> entity = new HttpEntity<>(data, headers);

		restTemplate.exchange(
				objectUrl(storagePath),
				HttpMethod.PUT,
				entity,
				String.class);
	}

	/**
	 * 指定パスのファイルをダウンロードする。
	 */
	public byte[] download(String storagePath) {
		HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

		return restTemplate.exchange(
				objectUrl(storagePath),
				HttpMethod.GET,
				entity,
				byte[].class)
				.getBody();
	}

	/**
	 * 指定パスのファイルを削除する。
	 */
	public void delete(String storagePath) {
		HttpEntity<Void> entity = new HttpEntity<>(createHeaders());

		restTemplate.exchange(
				objectUrl(storagePath),
				HttpMethod.DELETE,
				entity,
				String.class);
	}

	/**
	 * 公開URLを組み立てる。パスの各セグメントはURLエンコードする。
	 */
	public String publicUrl(String storagePath) {
		String encodedPath = Arrays.stream(storagePath.split("/", -1))
				.map(segment -> UriUtils.encode(segment, StandardCharsets.UTF_8))
				.collect(Collectors.joining("/"));

		return supabaseUrl
				+ "/storage/v1/object/public/"
				+ bucketName
				+ "/"
				+ encodedPath;
	}

	/**
	 * オブジェクト操作用のAPI URLを組み立てる。
	 */
	private String objectUrl(String storagePath) {
		return supabaseUrl
				+ "/storage/v1/object/"
				+ bucketName
				+ "/"
				+ storagePath;
	}

	/**
	 * apikeyを設定した共通ヘッダーを生成する。
	 */
	private HttpHeaders createHeaders() {
		HttpHeaders headers = new HttpHeaders();
		headers.set("apikey", supabaseKey);
		return headers;
	}

	/**
	 * 一覧APIのレスポンス(JSON配列)をノードのリストへ変換する。
	 */
	private List<JsonNode> toNodes(String response) throws Exception {
		JsonNode root = objectMapper.readTree(response);
		List<JsonNode> nodes = new ArrayList<>();

		if (root.isArray()) {
			root.forEach(nodes::add);
		}

		return nodes;
	}
}