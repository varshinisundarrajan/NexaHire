package com.nexahire.service;

import com.nexahire.dto.UserUpdateRequest;
import com.nexahire.entity.AuditLog;
import com.nexahire.entity.Role;
import com.nexahire.entity.User;
import com.nexahire.exception.BadRequestException;
import com.nexahire.exception.ResourceNotFoundException;
import com.nexahire.repository.AuditLogRepository;
import com.nexahire.repository.RoleRepository;
import com.nexahire.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       AuditLogRepository auditLogRepository,
                       AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.auditLogRepository = auditLogRepository;
        this.auditService = auditService;
    }

    public Page<User> getAllUsers(int page, int size, String sortBy, String direction) {
        Sort sort = "desc".equalsIgnoreCase(direction) ? Sort.by(sortBy).descending() : Sort.by(sortBy).ascending();
        Pageable pageable = PageRequest.of(page, size, sort);
        return userRepository.findAll(pageable);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));
    }

    @Transactional
    public User updateUser(Long id, UserUpdateRequest request) {
        User user = getUserById(id);

        if (request.getFullName() != null && !request.getFullName().isBlank()) {
            user.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            user.setPhone(request.getPhone());
        }
        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            user.setStatus(request.getStatus().toUpperCase());
        }
        if (request.getRoleName() != null && !request.getRoleName().isBlank()) {
            String roleName = request.getRoleName().startsWith("ROLE_") ? request.getRoleName() : "ROLE_" + request.getRoleName().toUpperCase();
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new BadRequestException("Role not found: " + request.getRoleName()));
            user.setRole(role);
        }

        User updated = userRepository.save(user);
        auditService.log("UPDATE_USER", "USER", id, "Admin updated user details for " + updated.getUsername());
        return updated;
    }

    @Transactional
    public User toggleUserStatus(Long id, String status) {
        User user = getUserById(id);
        user.setStatus(status.toUpperCase());
        User updated = userRepository.save(user);
        auditService.log("CHANGE_USER_STATUS", "USER", id, "User status changed to " + status);
        return updated;
    }

    public Page<AuditLog> getAuditLogs(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("timestamp").descending());
        return auditLogRepository.findAllByOrderByTimestampDesc(pageable);
    }

    public List<AuditLog> getRecentAuditLogs() {
        return auditLogRepository.findTop20ByOrderByTimestampDesc();
    }
}
