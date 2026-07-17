package com.example.smartvisionstock.repository;

import com.example.smartvisionstock.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    /** 按用户名模糊分页查询（修复搜索失效问题）*/
    Page<User> findByUsernameContainingIgnoreCase(String username, Pageable pageable);

    /** 按角色名称统计用户数（删除角色前检查引用）*/
    long countByRole(String role);
}