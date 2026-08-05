package com.example.bff.service;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.representations.idm.FederatedIdentityRepresentation;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AdminIdentityService {

    private static final Logger log = LoggerFactory.getLogger(AdminIdentityService.class);

    private final Keycloak keycloak;
    private final FindByIndexNameSessionRepository<? extends Session> sessionRepository;

    @Value("${KEYCLOAK_REALM:event-ticketing}")
    private String realm;

    public AdminIdentityService(
            Keycloak keycloak,
            @Autowired(required = false) FindByIndexNameSessionRepository<? extends Session> sessionRepository) {
        this.keycloak = keycloak;
        this.sessionRepository = sessionRepository;
    }

    public void updateUserRoles(String userId, List<String> roleNames) {
        UserResource userResource = keycloak.realm(realm).users().get(userId);
        UserRepresentation user = userResource.toRepresentation();

        // Validate Enterprise/LDAP User constraint
        if (user.getFederationLink() != null && !user.getFederationLink().isEmpty()) {
            log.warn("Attempted to modify roles for enterprise LDAP user: {}", user.getUsername());
            throw new IllegalArgumentException("Cannot modify roles for Enterprise LDAP federated user. Roles are managed by the enterprise directory.");
        }

        List<RoleRepresentation> rolesToAdd = roleNames.stream()
                .map(roleName -> keycloak.realm(realm).roles().get(roleName).toRepresentation())
                .collect(Collectors.toList());

        userResource.roles().realmLevel().add(rolesToAdd);
        log.info("Successfully updated roles {} for user {}", roleNames, user.getUsername());

        // Invalidate all Keycloak SSO sessions and token state to prevent using old roles
        userResource.logout();

        // Invalidate all Spring Sessions in Redis for this user
        invalidateBffSessions(user.getUsername());
        log.info("Terminated all Keycloak and BFF sessions after role update for user: {}", user.getUsername());
    }

    public void resetUserPassword(String userId, String newPassword) {
        UserResource userResource = keycloak.realm(realm).users().get(userId);
        UserRepresentation user = userResource.toRepresentation();

        // 1. Kiểm tra tài khoản Enterprise LDAP
        if (user.getFederationLink() != null && !user.getFederationLink().isEmpty()) {
            log.warn("Attempted to reset password for enterprise LDAP user: {}", user.getUsername());
            throw new IllegalArgumentException("Cannot reset password for Enterprise LDAP federated user. Password management is handled in the corporate directory.");
        }

        // 2. Kiểm tra tài khoản Đăng nhập mạng xã hội (Google, GitHub, Facebook)
        List<FederatedIdentityRepresentation> federatedIdentities = userResource.getFederatedIdentity();
        boolean isFederated = federatedIdentities != null && !federatedIdentities.isEmpty();

        // Kiểm tra xem tài khoản này có mật khẩu nội bộ hay không (trường hợp tài khoản tự tạo rồi sau đó mới liên kết Google)
        List<CredentialRepresentation> credentials = userResource.credentials();
        boolean hasLocalPassword = credentials != null && credentials.stream()
                .anyMatch(cred -> CredentialRepresentation.PASSWORD.equals(cred.getType()));

        // Nếu là tài khoản Google thuần túy (chưa từng có mật khẩu nội bộ) -> Chặn không cho set password
        if (isFederated && !hasLocalPassword) {
            String idpList = federatedIdentities.stream()
                    .map(FederatedIdentityRepresentation::getIdentityProvider)
                    .collect(Collectors.joining(", "));
            log.warn("Attempted to reset password for pure external IdP user: {} (Provider: {})", user.getUsername(), idpList);
            throw new IllegalArgumentException("Cannot reset password for pure external Identity Provider account (" + idpList + "). This account authenticates exclusively through " + idpList + ".");
        }

        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(newPassword);
        credential.setTemporary(false);

        userResource.resetPassword(credential);
        
        // Invalidate all Keycloak sessions across all devices for this user
        userResource.logout();

        // Invalidate all Spring Sessions in Redis for this user
        invalidateBffSessions(user.getUsername());
        log.info("Successfully reset password and terminated all Keycloak + BFF sessions for user {}", user.getUsername());
    }

    public void logoutAllSessions(String userId) {
        UserResource userResource = keycloak.realm(realm).users().get(userId);
        UserRepresentation user = userResource.toRepresentation();

        // Invalidate Keycloak SSO sessions
        userResource.logout();

        // Invalidate Spring Sessions in Redis
        invalidateBffSessions(user.getUsername());
        log.info("Successfully terminated all active sessions across Keycloak and BFF for user: {}", user.getUsername());
    }

    private void invalidateBffSessions(String username) {
        if (sessionRepository != null && username != null) {
            Map<String, ? extends Session> userSessions = sessionRepository.findByPrincipalName(username);
            if (userSessions != null && !userSessions.isEmpty()) {
                userSessions.keySet().forEach(sessionRepository::deleteById);
                log.info("Deleted {} active Spring Session(s) from Redis for user: {}", userSessions.size(), username);
            }
        }
    }
}
