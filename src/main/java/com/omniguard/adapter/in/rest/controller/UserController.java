package com.omniguard.adapter.in.rest.controller;

import com.omniguard.adapter.in.rest.dto.user.*;
import com.omniguard.adapter.in.rest.mapper.UserRestMapper;
import com.omniguard.adapter.in.rest.mapper.RoleRestMapper;
import com.omniguard.domain.model.User;
import com.omniguard.domain.model.Role;
import com.omniguard.domain.port.in.UserUseCase;
import com.omniguard.domain.vo.Email;
import com.omniguard.domain.vo.Username;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserUseCase userUseCase;
    private final UserRestMapper userRestMapper;
    private final RoleRestMapper roleRestMapper;

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getMe() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();
        User user = userUseCase.getMe(UUID.fromString(userId));
        return ResponseEntity.ok(userRestMapper.toResponse(user));
    }

    @PatchMapping("/me")
    public ResponseEntity<UserResponse> updateProfile(
            @Valid @RequestBody UserProfileUpdateRequest request
    ) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();
        User user = userUseCase.updateProfile(
                UUID.fromString(userId),
                Username.of(request.getUsername()),
                Email.of(request.getEmail())
        );
        return ResponseEntity.ok(userRestMapper.toResponse(user));
    }

    @PostMapping("/me/change-password")
    public ResponseEntity<Void> changePassword(
            @Valid @RequestBody com.omniguard.adapter.in.rest.dto.auth.ChangePasswordRequest request
    ) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();
        userUseCase.changePassword(
                UUID.fromString(userId),
                request.getCurrentPassword(),
                request.getNewPassword()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/me/totp")
    public ResponseEntity<com.omniguard.adapter.in.rest.dto.auth.TotpSetupResponse> toggleTotp(
            @Valid @RequestBody com.omniguard.adapter.in.rest.dto.auth.ToggleTotpRequest request
    ) {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        String userId = auth.getName();
        User user = userUseCase.toggleTotp(UUID.fromString(userId), request.getEnable());

        if (request.getEnable() && user.totpSecret() != null) {
            String qrCodeUrl = String.format(
                    "otpauth://totp/OmniGuard:%s?secret=%s&issuer=OmniGuard",
                    user.email(), user.totpSecret()
            );
            return ResponseEntity.ok(new com.omniguard.adapter.in.rest.dto.auth.TotpSetupResponse(user.totpSecret(), qrCodeUrl, null));
        }
        return ResponseEntity.ok(new com.omniguard.adapter.in.rest.dto.auth.TotpSetupResponse(null, null, null));
    }

    // Admin endpoints
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.omniguard.adapter.in.rest.dto.common.PageResponse<UserResponse>> findAll(
            @RequestParam(defaultValue = "0") @jakarta.validation.constraints.Min(0) int page,
            @RequestParam(defaultValue = "20") @jakarta.validation.constraints.Min(1) @jakarta.validation.constraints.Max(100) int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "DESC") String sortDir
    ) {
        Pageable pageable = org.springframework.data.domain.PageRequest.of(page, size, org.springframework.data.domain.Sort.Direction.fromString(sortDir), sortBy);
        Page<User> users = userUseCase.findAllUsers(page, size);
        return ResponseEntity.ok(com.omniguard.adapter.in.rest.dto.common.PageResponse.of(users.map(userRestMapper::toResponse)));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest request) {
        User user = userUseCase.createUser(
                Username.of(request.getUsername()),
                Email.of(request.getEmail()),
                request.getPassword(),
                request.getRole()
        );
        return ResponseEntity.ok(userRestMapper.toResponse(user));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> findById(@PathVariable UUID id) {
        User user = userUseCase.findById(id)
                .orElseThrow(() -> new com.omniguard.domain.exception.EntityNotFoundException("User", id));
        return ResponseEntity.ok(userRestMapper.toResponse(user));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody UserUpdateRequest request
    ) {
        User user = userUseCase.updateUser(
                id,
                request.getUsername() != null ? Username.of(request.getUsername()) : null,
                request.getEmail() != null ? Email.of(request.getEmail()) : null,
                request.getRole(),
                request.getIsActive() != null ? request.getIsActive() : true
        );
        return ResponseEntity.ok(userRestMapper.toResponse(user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        userUseCase.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<com.omniguard.adapter.in.rest.dto.role.RoleResponse>> findAllRoles() {
        List<Role> roles = userUseCase.findAllRoles();
        return ResponseEntity.ok(roleRestMapper.toResponseList(roles));
    }

    @PostMapping("/roles")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<com.omniguard.adapter.in.rest.dto.role.RoleResponse> createRole(
            @RequestParam String name,
            @RequestParam(required = false) String description
    ) {
        Role role = userUseCase.createRole(name, description);
        return ResponseEntity.ok(roleRestMapper.toResponse(role));
    }

    @DeleteMapping("/roles/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRole(@PathVariable UUID id) {
        userUseCase.deleteRole(id);
        return ResponseEntity.noContent().build();
    }
}