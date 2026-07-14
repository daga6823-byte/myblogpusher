package com.app.myblogpusher.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "menu_master")
public class MenuMaster {

    @Id
    @Column(name = "menu_url")
    private String menuUrl;

    @Column(name = "menu_name", nullable = false)
    private String menuName;

    @Column(name = "min_role", nullable = false)
    private Integer minRole;
    
    @Column(name = "create_date")
    private LocalDateTime createDate;

    @Column(name = "update_date")
    private LocalDateTime updateDate;

    @Column(name = "create_user")
    private Long createUser;

    @Column(name = "update_user")
    private Long updateUser;
}