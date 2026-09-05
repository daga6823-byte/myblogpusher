package com.app.myblogpusher.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.app.myblogpusher.entity.CategoryRelation;

@Repository
public interface CategoryRelationRepository
        extends JpaRepository<CategoryRelation, Long> {

    List<CategoryRelation> findByCategoryId(Long categoryId);

    List<CategoryRelation> findByParentCategoryId(Long parentCategoryId);

    List<CategoryRelation> findByCategoryPath(String categoryPath);

    List<CategoryRelation> findByGroupId(Long groupId);
}
