package com.app.myblogpusher.service;

import java.util.ArrayList;
import java.util.List;

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
		headers.setBearerAuth(supabaseKey);
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

	public String getImageUrl(String fileName) {
		return supabaseUrl + "/storage/v1/object/public/" + bucketName + "/" + fileName;
	}
}