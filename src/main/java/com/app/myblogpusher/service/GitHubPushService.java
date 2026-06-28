package com.app.myblogpusher.service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.ArticleCategory;
import com.app.myblogpusher.entity.EnglishDictionary;
import com.app.myblogpusher.entity.UserRepositoryEntity;
import com.app.myblogpusher.repository.ArticleCategoryRepository;
import com.app.myblogpusher.repository.EnglishDictionaryRepository;
import com.app.myblogpusher.util.SlugUtil;
import com.atilika.kuromoji.ipadic.Token;
import com.atilika.kuromoji.ipadic.Tokenizer;

@Service
public class GitHubPushService {

	private final TokenCipherService tokenCipherService;
	private final ArticleCategoryRepository articleCategoryRepository;
	private final ArticleWorkService articleWorkService;
	private final EnglishDictionaryRepository englishDictionaryRepository;

	public GitHubPushService(
			TokenCipherService tokenCipherService,
			ArticleCategoryRepository articleCategoryRepository,
			ArticleWorkService articleWorkService,
			EnglishDictionaryRepository englishDictionaryRepository) {

		this.tokenCipherService = tokenCipherService;
		this.articleCategoryRepository = articleCategoryRepository;
		this.articleWorkService = articleWorkService;
		this.englishDictionaryRepository = englishDictionaryRepository;
	}

	/**
	 * 記事をMarkdownファイルとしてGitHubにプッシュ
	 */
	public void pushArticle(
			UserRepositoryEntity repoEntity,
			String cipherKey,
			Long categoryId,
			String articleTitle,
			String articleContent,
			String slug)
			throws IOException, GitAPIException {

		System.out.println("pushArticle start");

		//		long start = System.currentTimeMillis();

		String accessToken = tokenCipherService.decrypt(
				repoEntity.getAccessToken(),
				repoEntity.getTokenIv(),
				cipherKey);

		System.out.println("Decrypted token length: " + accessToken.length());
		System.out.println("Token starts with: " + accessToken.substring(0, Math.min(10, accessToken.length())));

		String repoPath = System.getProperty("java.io.tmpdir")
				+ "/myblogpusher_"
				+ repoEntity.getRepoId();

		File repoDir = new File(repoPath);

		if (!repoDir.exists()) {
			repoDir.mkdirs();
		}

		ArticleCategory category = articleCategoryRepository.findById(categoryId)
				.orElseThrow(() -> new IllegalArgumentException("カテゴリーが見つかりません"));

		String categorySlug = SlugUtil.generateCategorySlug(category.getCategoryName());

		Git git = initializeRepository(repoDir, repoEntity, accessToken);

		//		System.out.println("initialize: " + (System.currentTimeMillis() - start) + "ms");

		try {

			createCategoryIndexIfNotExists(
					git,
					repoPath,
					categorySlug,
					category.getCategoryName());

			Path contentPath = Paths.get(
					repoPath,
					"content",
					"posts",
					slug + ".md");

			contentPath.getParent().toFile().mkdirs();

			Files.write(
					contentPath,
					articleContent.getBytes(StandardCharsets.UTF_8));

			//			System.out.println("write: " + (System.currentTimeMillis() - start) + "ms");

			git.add()
					.addFilepattern("content")
					.call();

			//			System.out.println("add: " + (System.currentTimeMillis() - start) + "ms");

			git.commit()
					.setMessage("Add article: " + slug)
					.setAuthor(
							"Myblogpusher",
							"noreply@myblogpusher.local")
					.call();

			//			System.out.println("commit: " + (System.currentTimeMillis() - start) + "ms");

			// Git push
			git.push()
					.setCredentialsProvider(new UsernamePasswordCredentialsProvider("git", accessToken))
					.setRefSpecs(new org.eclipse.jgit.transport.RefSpec("HEAD:refs/heads/main"))
					.call();

			//			System.out.println("push: " + (System.currentTimeMillis() - start) + "ms");

		} finally {
			git.close();
		}
	}

	private void createCategoryIndexIfNotExists(Git git, String repoPath, String categorySlug, String categoryName)
			throws IOException, GitAPIException {

		Path indexPath = Paths.get(repoPath, "content", "categories", categorySlug, "_index.md");

		// 既に存在すればスキップ
		if (Files.exists(indexPath)) {
			return;
		}

		indexPath.getParent().toFile().mkdirs();

		String indexContent = "---\n"
				+ "title: \"" + categoryName + "\"\n"
				+ "description: \"\"\n"
				+ "---\n";

		Files.write(indexPath, indexContent.getBytes(StandardCharsets.UTF_8));

		// Git add
		git.add().addFilepattern("content/categories/" + categorySlug + "/_index.md").call();
	}

	private Git initializeRepository(File repoDir, UserRepositoryEntity repoEntity, String accessToken)
			throws IOException, GitAPIException {

		long start = System.currentTimeMillis();

		File gitDir = new File(repoDir, ".git");

		if (gitDir.exists()) {

			Repository repository = new FileRepositoryBuilder()
					.setGitDir(gitDir)
					.build();

			System.out.println("build: " + (System.currentTimeMillis() - start) + "ms");

			Git git = new Git(repository);

			System.out.println("new Git: " + (System.currentTimeMillis() - start) + "ms");

			git.pull()
					.setCredentialsProvider(
							new UsernamePasswordCredentialsProvider(
									"git",
									accessToken))
					.call();

			System.out.println("pull: " + (System.currentTimeMillis() - start) + "ms");

			return git;

		} else {
			String remoteUrl = String.format("https://github.com/%s/%s.git",
					repoEntity.getRepoOwner(),
					repoEntity.getRepoName());

			return Git.cloneRepository()
					.setURI(remoteUrl)
					.setDirectory(repoDir)
					.setCredentialsProvider(new UsernamePasswordCredentialsProvider("git", accessToken))
					.call();
		}
	}

	@Async
	public void pushArticleAsync(UserRepositoryEntity repoEntity, String cipherKey,
			Long categoryId, String articleTitle, String articleContent,
			String slug, Long workId, Long userId) {
		try {
			pushArticle(repoEntity, cipherKey, categoryId, articleTitle, articleContent, slug);

			// GitHub プッシュ成功後、article_workに保存
			if (workId != null) {
				this.articleWorkService.updateArticleWork(workId, categoryId, articleTitle, articleContent, userId,
						slug);
			} else {
				this.articleWorkService.insertArticleWork(userId, categoryId, articleTitle, articleContent, slug);
			}
		} catch (Exception e) {
			System.err.println("投稿処理に失敗しました: " + e.getMessage());
			e.printStackTrace();
		}
	}

	public String generateSlugWithDictionary(String title) {
		if (title == null || title.isBlank()) {
			return "no-title";
		}

		System.out.println("Title: " + title);

		String result = title;

		// 辞書に登録された全ての日本語を検索して置き換え
		List<EnglishDictionary> dictionaries = englishDictionaryRepository.findAll();
		for (EnglishDictionary dict : dictionaries) {
			if (result.contains(dict.getJapanese())) {
				System.out.println("Found: " + dict.getJapanese() + " -> " + dict.getEnglish());
				result = result.replace(dict.getJapanese(), " " + dict.getEnglish() + " ");
			}
		}

		System.out.println("After dictionary replace: " + result);

		// 残りの文字を kuromoji でローマ字化
		List<Token> tokens = new Tokenizer().tokenize(result);
		StringBuilder finalResult = new StringBuilder();

		for (Token token : tokens) {
			String reading = token.getReading();
			if (reading != null && !reading.isEmpty() && !reading.equals("*")) {
				String hiragana = katakanaToHiragana(reading);
				finalResult.append(SlugUtil.katakanaToRomaji(hiragana)).append("-");
			}
		}

		String finalSlug = finalResult.toString()
				.toLowerCase()
				.replaceAll("[^a-z0-9-]", "")
				.replaceAll("-+", "-")
				.replaceAll("^-+|-+$", "")
				.trim();

		System.out.println("Final slug: " + finalSlug);

		return finalSlug;
	}

	private String katakanaToHiragana(String katakana) {
		return katakana
				.replaceAll("ア", "あ").replaceAll("イ", "い").replaceAll("ウ", "う").replaceAll("エ", "え")
				.replaceAll("オ", "お")
				.replaceAll("カ", "か").replaceAll("キ", "き").replaceAll("ク", "く").replaceAll("ケ", "け")
				.replaceAll("コ", "こ")
				.replaceAll("ガ", "が").replaceAll("ギ", "ぎ").replaceAll("グ", "ぐ").replaceAll("ゲ", "げ")
				.replaceAll("ゴ", "ご")
				.replaceAll("サ", "さ").replaceAll("シ", "し").replaceAll("ス", "す").replaceAll("セ", "せ")
				.replaceAll("ソ", "そ")
				.replaceAll("ザ", "ざ").replaceAll("ジ", "じ").replaceAll("ズ", "ず").replaceAll("ゼ", "ぜ")
				.replaceAll("ゾ", "ぞ")
				.replaceAll("タ", "た").replaceAll("チ", "ち").replaceAll("ツ", "つ").replaceAll("テ", "て")
				.replaceAll("ト", "と")
				.replaceAll("ダ", "だ").replaceAll("ヂ", "ぢ").replaceAll("ヅ", "づ").replaceAll("デ", "で")
				.replaceAll("ド", "ど")
				.replaceAll("ナ", "な").replaceAll("ニ", "に").replaceAll("ヌ", "ぬ").replaceAll("ネ", "ね")
				.replaceAll("ノ", "の")
				.replaceAll("ハ", "は").replaceAll("ヒ", "ひ").replaceAll("フ", "ふ").replaceAll("ヘ", "へ")
				.replaceAll("ホ", "ほ")
				.replaceAll("バ", "ば").replaceAll("ビ", "び").replaceAll("ブ", "ぶ").replaceAll("ベ", "べ")
				.replaceAll("ボ", "ぼ")
				.replaceAll("パ", "ぱ").replaceAll("ピ", "ぴ").replaceAll("プ", "ぷ").replaceAll("ペ", "ぺ")
				.replaceAll("ポ", "ぽ")
				.replaceAll("マ", "ま").replaceAll("ミ", "み").replaceAll("ム", "む").replaceAll("メ", "め")
				.replaceAll("モ", "も")
				.replaceAll("ヤ", "や").replaceAll("ユ", "ゆ").replaceAll("ヨ", "よ")
				.replaceAll("ラ", "ら").replaceAll("リ", "り").replaceAll("ル", "る").replaceAll("レ", "れ")
				.replaceAll("ロ", "ろ")
				.replaceAll("ワ", "わ").replaceAll("ヲ", "を").replaceAll("ン", "ん");
	}
}
