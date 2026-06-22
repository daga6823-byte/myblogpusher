package com.app.myblogpusher.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.app.myblogpusher.entity.MenuMaster;

public interface MenuMasterRepository extends JpaRepository<MenuMaster, String> {
    List<MenuMaster> findByMinRoleLessThanEqualOrderByMinRoleAsc(Integer role);
}