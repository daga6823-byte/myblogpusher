/**
 * カテゴリー同士の親子関係を管理するサービス
 *
 * ArticleCategoryはカテゴリーそのものを管理し、
 * CategoryRelationはカテゴリーがどの親カテゴリー配下に存在するか、
 * また、その関係がどのカテゴリー経路に属するかを管理する。
 */

package com.app.myblogpusher.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.CategoryRelation;
import com.app.myblogpusher.repository.CategoryRelationRepository;

@Service
public class CategoryRelationService {

    @Autowired
    private CategoryRelationRepository categoryRelationRepository;

    /**
     * カテゴリーと親カテゴリーの関係を登録する。
     *
     * groupIdが指定されている場合は既存のgroupIdを使用し、
     * 未指定の場合はPostgreSQLのserialによる自動採番に任せる。
     */
    public void addRelation(
            Long categoryId,
            Long parentCategoryId,
            Long groupId,
            String categoryPath,
            Long userId) {

        if (categoryId == null
                || parentCategoryId == null
                || categoryPath == null
                || categoryPath.isBlank()) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();

        CategoryRelation relation = new CategoryRelation();

        relation.setGroupId(groupId);
        relation.setCategoryId(categoryId);
        relation.setParentCategoryId(parentCategoryId);
        relation.setCategoryPath(categoryPath);

        relation.setCreateDate(now);
        relation.setUpdateDate(now);
        relation.setCreateUser(userId);
        relation.setUpdateUser(userId);

        categoryRelationRepository.save(relation);
    }

    /**
     * 指定カテゴリーの親子関係をすべて削除する。
     */
    public void deleteRelationsByCategoryId(Long categoryId) {

        List<CategoryRelation> relations = categoryRelationRepository
                .findByCategoryId(categoryId);

        categoryRelationRepository.deleteAll(relations);
    }

    /**
     * 指定カテゴリーを親としている関係をすべて削除する。
     */
    public void deleteRelationsByParentCategoryId(Long parentCategoryId) {

        List<CategoryRelation> relations = categoryRelationRepository
                .findByParentCategoryId(parentCategoryId);

        categoryRelationRepository.deleteAll(relations);
    }
}
