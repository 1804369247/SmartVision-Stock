package com.example.smartvisionstock.service.impl;

import com.example.smartvisionstock.entity.Role;
import com.example.smartvisionstock.repository.RoleRepository;
import com.example.smartvisionstock.repository.UserRepository;
import com.example.smartvisionstock.service.RoleService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoleServiceImpl implements RoleService {

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional
    public Role createRole(Role role) {
        if (roleRepository.existsByName(role.getName())) {
            throw new IllegalArgumentException("角色名称已存在");
        }
        return roleRepository.save(role);
    }

    @Override
    @Transactional
    public Role updateRole(Long id, Role role) {
        Role existing = roleRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("角色不存在"));
        if (!existing.getName().equals(role.getName()) && roleRepository.existsByName(role.getName())) {
            throw new IllegalArgumentException("角色名称已存在");
        }
        existing.setDescription(role.getDescription());
        existing.setMenuIds(role.getMenuIds());
        existing.setDataScope(role.getDataScope());
        return roleRepository.save(existing);
    }

    @Override
    @Transactional
    public void deleteRole(Long id) {
        Role role = roleRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("角色不存在"));
        
        // 检查是否有用户正在使用此角色
        long userCount = userRepository.countByRole(role.getName());
        if (userCount > 0) {
            throw new IllegalArgumentException("该角色下仍有 " + userCount + " 名用户，请先为用户更换角色后再删除");
        }
        
        roleRepository.deleteById(id);
    }

    @Override
    public Role getRoleById(Long id) {
        return roleRepository.findById(id).orElse(null);
    }

    @Override
    public Role getRoleByName(String name) {
        return roleRepository.findByName(name).orElse(null);
    }

    @Override
    public Page<Role> getRoleList(int page, int size, String name) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        if (name != null && !name.isEmpty()) {
            return roleRepository.findByNameContaining(name, pageable);
        }
        return roleRepository.findAll(pageable);
    }

    @Override
    public List<Role> getAllRoles() {
        return roleRepository.findAll(Sort.by(Sort.Direction.DESC, "createTime"));
    }

    @Override
    @Transactional
    public void updateStatus(Long roleId, Integer status) {
        Role role = roleRepository.findById(roleId).orElseThrow(() -> new IllegalArgumentException("角色不存在"));
        role.setStatus(status);
        roleRepository.save(role);
    }
}