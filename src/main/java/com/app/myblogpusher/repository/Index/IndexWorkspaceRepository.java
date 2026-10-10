
/**
 * インデックス編集中の一時保存データへのアクセスを担当するリポジトリ
 */
package com.app.myblogpusher.repository.Index;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.Index.IndexWorkspace;

public interface IndexWorkspaceRepository
		extends JpaRepository<IndexWorkspace, Long> {
}
