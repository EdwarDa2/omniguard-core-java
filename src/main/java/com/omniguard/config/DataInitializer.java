package com.omniguard.config;

import com.omniguard.domain.model.Role;
import com.omniguard.domain.model.User;
import com.omniguard.domain.port.out.RoleRepository;
import com.omniguard.domain.port.out.UserRepository;
import com.omniguard.domain.vo.Email;
import com.omniguard.domain.vo.Username;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DataInitializer implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${admin.initial.username:admin}")
    private String adminUsername;

    @Value("${admin.initial.email:admin@omniguard.local}")
    private String adminEmail;

    @Value("${admin.initial.password:OmniGuardAdmin2026!}")
    private String adminPassword;

    public DataInitializer(
            RoleRepository roleRepository,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        initializeRoles();
        initializeAdminUser();
    }

    private void initializeRoles() {
        createRoleIfNotExists("ROLE_ADMIN", "Administrador total del SOC");
        createRoleIfNotExists("ROLE_ANALYST", "Analista de Ciberseguridad / Blue Team");
        createRoleIfNotExists("ROLE_READONLY", "Auditor con acceso de sólo lectura");
    }

    private Role createRoleIfNotExists(String name, String description) {
        return roleRepository.findByName(name).orElseGet(() -> {
            log.info("Sembrando rol del sistema: {}", name);
            return roleRepository.save(Role.create(name, description));
        });
    }

    private void initializeAdminUser() {
        Username username = Username.of(adminUsername);
        if (userRepository.existsByUsername(username)) {
            log.debug("El usuario administrador '{}' ya existe. Omitiendo inicialización.", adminUsername);
            return;
        }

        Role adminRole = createRoleIfNotExists("ROLE_ADMIN", "Administrador total del SOC");
        Email email = Email.of(adminEmail);
        String passwordHash = passwordEncoder.encode(adminPassword);

        User adminUser = User.create(adminRole, username, email, passwordHash);
        userRepository.save(adminUser);

        log.info("Usuario administrador inicial sembrado con éxito: {} ({})", adminUsername, adminEmail);
    }
}
