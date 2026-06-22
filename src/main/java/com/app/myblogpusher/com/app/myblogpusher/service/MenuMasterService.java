package com.app.myblogpusher.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.app.myblogpusher.entity.MenuMaster;
import com.app.myblogpusher.repository.MenuMasterRepository;

@Service
public class MenuMasterService {

    @Autowired
    private MenuMasterRepository menuMasterRepository;

    public List<MenuMaster> findVisibleMenus(Integer role) {
        return menuMasterRepository.findByMinRoleLessThanEqualOrderByMinRoleAsc(role);
    }
}