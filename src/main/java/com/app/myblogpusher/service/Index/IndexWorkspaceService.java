
package com.app.myblogpusher.service.Index;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.Index.IndexWorkspace;
import com.app.myblogpusher.repository.Index.IndexWorkspaceRepository;

@Service
public class IndexWorkspaceService {

	@Autowired
	private IndexWorkspaceRepository repository;

	public void save(
			Long userId,
			Long groupId,
			String title,
			String content) {

		if (userId == null) {
			return;
		}

		IndexWorkspace workspace = repository.findById(userId)
				.orElse(new IndexWorkspace());

		workspace.setUserId(userId);
		workspace.setGroupId(groupId);
		workspace.setTitle(title);
		workspace.setContent(content);
		workspace.setUpdateDate(LocalDateTime.now());

		repository.save(workspace);
	}

	public Optional<IndexWorkspace> find(Long userId) {
		return repository.findById(userId);
	}

	public void delete(Long userId) {
		repository.deleteById(userId);
	}
}
