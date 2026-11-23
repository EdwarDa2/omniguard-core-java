package com.omniguard.adapter.in.rest.controller;

import com.omniguard.adapter.in.rest.dto.request.CreateUserRequestDto;
import com.omniguard.adapter.in.rest.dto.request.UpdateUserStatusRequestDto;
import com.omniguard.adapter.in.rest.dto.response.PlatformUserDto;
import com.omniguard.adapter.in.rest.dto.response.UserListResponseDto;
import com.omniguard.domain.model.User;
import com.omniguard.domain.port.in.UserUseCase;
import com.omniguard.domain.vo.Email;
import com.omniguard.domain.vo.Username;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.security.SecureRandom;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Gestión de Identidades (IAM)", description = "Administración de analistas y administradores del SOC")
public class UserController {

    private static final DateTimeFormatter DATE_FORMATTER =
            DateTimeFormatter.ofPattern("dd MMM, HH:mm").withZone(ZoneId.of("UTC"));

    private final UserUseCase userUseCase;

    public UserController(UserUseCase userUseCase) {
        this.userUseCase = userUseCase;
    }

    @GetMapping
    @Operation(summary = "Listar analistas y administradores", description = "Endpoint para el Tab IAM con métricas de usuarios activos, admins y revocados")
    public ResponseEntity<UserListResponseDto> listUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size
    ) {
        List<User> domainUsers = userUseCase.findAllUsers(page, size);
        String currentUsername = getCurrentUsername();

        List<PlatformUserDto> userDtos = domainUsers.stream().map(u -> {
            boolean isCurrent = u.username().getValue().equalsIgnoreCase(currentUsername);
            return toPlatformUserDto(u, isCurrent);
        }).toList();

        long activeCount = userDtos.stream().filter(u -> "Activo".equalsIgnoreCase(u.status())).count();
        long adminCount = userDtos.stream().filter(u -> "Admin".equalsIgnoreCase(u.role())).count();
        long revokedCount = userDtos.stream().filter(u -> "Revocado".equalsIgnoreCase(u.status())).count();

        UserListResponseDto.UserMetricsDto metrics =
                new UserListResponseDto.UserMetricsDto(activeCount, adminCount, revokedCount);

        return ResponseEntity.ok(new UserListResponseDto(metrics, userDtos));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Crear nuevo analista", description = "Crea un usuario y asigna rol en el SOC")
    public ResponseEntity<PlatformUserDto> createUser(@Valid @RequestBody CreateUserRequestDto request) {
        String roleName = "Admin".equalsIgnoreCase(request.role()) ? "ROLE_ADMIN" : "ROLE_ANALYST";
        String generatedPassword = generateSecurePassword();

        String rawUsername = request.email().split("@")[0].replaceAll("[^a-zA-Z0-9_.-]", "");
        if (rawUsername.length() < 3) rawUsername = "user_" + rawUsername;

        User created = userUseCase.createUser(
                Username.of(rawUsername),
                Email.of(request.email()),
                generatedPassword,
                roleName
        );

        return ResponseEntity.status(HttpStatus.CREATED).body(toPlatformUserDto(created, false));
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Cambiar estado de acceso", description = "Permite revocar o reactivar accesos al SOC")
    public ResponseEntity<PlatformUserDto> updateUserStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateUserStatusRequestDto request
    ) {
        boolean activate = !"Revocado".equalsIgnoreCase(request.status()) && !"disabled".equalsIgnoreCase(request.status());
        User user = userUseCase.getMe(id);
        User updated = userUseCase.updateUser(id, user.username(), user.email(), user.role().name(), activate);

        boolean isCurrent = updated.username().getValue().equalsIgnoreCase(getCurrentUsername());
        return ResponseEntity.ok(toPlatformUserDto(updated, isCurrent));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ROLE_ADMIN')")
    @Operation(summary = "Eliminar analista", description = "Elimina permanentemente una cuenta de usuario")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        userUseCase.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    private PlatformUserDto toPlatformUserDto(User user, boolean isCurrentUser) {
        String roleDisplay = user.role().name().contains("ADMIN") ? "Admin" : "Viewer";
        String statusDisplay = user.isActive() ? "Activo" : "Revocado";
        String lastLoginDisplay = user.lastLoginAt() != null
                ? DATE_FORMATTER.format(user.lastLoginAt())
                : "Nunca";

        return new PlatformUserDto(
                String.valueOf(user.id()),
                user.username().getValue(),
                user.email() != null ? user.email().getValue() : "",
                roleDisplay,
                statusDisplay,
                lastLoginDisplay,
                isCurrentUser
        );
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null ? auth.getName() : "";
    }

    private String generateSecurePassword() {
        byte[] bytes = new byte[16];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes) + "Aa1!";
    }
}
