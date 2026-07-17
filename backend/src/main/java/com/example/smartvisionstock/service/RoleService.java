package com.example.smartvisionstock.service;

import com.example.smartvisionstock.entity.Role;
import org.springframework.data.domain.Page;

import java.util.List;

public interface RoleService {
    Role createRole(Role role);
    Role updateRole(Long id, Role role);
    void deleteRole(Long id);
    Role getRoleById(Long id);
    Role getRoleByName(String name);
    Page<Role> getRoleList(int page, int size, String name);
    List<Role> getAllRoles();
    void updateStatus(Long roleId, Integer status);
}