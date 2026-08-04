package com.example.bff.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.session.FindByIndexNameSessionRepository;
import org.springframework.session.Session;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminIdentityServiceTest {

    @Mock
    private Keycloak keycloak;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private UserResource userResource;

    @Mock
    private RolesResource rolesResource;

    @Mock
    private RoleResource roleResource;

    @Mock
    private RoleMappingResource roleMappingResource;

    @Mock
    private RoleScopeResource roleScopeResource;

    @Mock
    private FindByIndexNameSessionRepository<Session> sessionRepository;

    @Mock
    private Session session1;

    @Mock
    private Session session2;

    private AdminIdentityService adminIdentityService;

    @BeforeEach
    void setUp() {
        adminIdentityService = new AdminIdentityService(keycloak, sessionRepository);
        ReflectionTestUtils.setField(adminIdentityService, "realm", "event-ticketing");

        lenient().when(keycloak.realm("event-ticketing")).thenReturn(realmResource);
        lenient().when(realmResource.users()).thenReturn(usersResource);
        lenient().when(usersResource.get(anyString())).thenReturn(userResource);
        lenient().when(realmResource.roles()).thenReturn(rolesResource);
        lenient().when(rolesResource.get(anyString())).thenReturn(roleResource);
        lenient().when(userResource.roles()).thenReturn(roleMappingResource);
        lenient().when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
    }

    @Test
    void updateUserRoles_success_forSelfRegisteredUser() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("customer1");
        user.setFederationLink(null); // Self-registered user

        when(userResource.toRepresentation()).thenReturn(user);
        when(roleResource.toRepresentation()).thenReturn(new RoleRepresentation("EVENT_ORGANIZER", "", false));
        when(sessionRepository.findByPrincipalName("customer1")).thenReturn(Map.of("sess-1", session1, "sess-2", session2));

        adminIdentityService.updateUserRoles("user-123", List.of("EVENT_ORGANIZER"));

        verify(roleScopeResource).add(anyList());
        verify(userResource).logout(); // Keycloak SSO invalidated
        verify(sessionRepository).deleteById("sess-1"); // Redis session invalidated
        verify(sessionRepository).deleteById("sess-2");
    }

    @Test
    void updateUserRoles_throwsException_forLdapEnterpriseUser() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("ldap_john");
        user.setFederationLink("ldap-enterprise-provider"); // Enterprise LDAP user

        when(userResource.toRepresentation()).thenReturn(user);

        assertThatThrownBy(() -> adminIdentityService.updateUserRoles("ldap-user-id", List.of("ADMIN")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot modify roles for Enterprise LDAP federated user");

        verify(roleScopeResource, never()).add(any());
        verify(userResource, never()).logout();
    }

    @Test
    void resetUserPassword_success_forSelfRegisteredUser() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("customer1");
        user.setFederationLink(null);

        when(userResource.toRepresentation()).thenReturn(user);
        when(sessionRepository.findByPrincipalName("customer1")).thenReturn(Map.of("sess-1", session1));

        adminIdentityService.resetUserPassword("user-123", "NewSecretPass123!");

        verify(userResource).resetPassword(any());
        verify(userResource).logout();
        verify(sessionRepository).deleteById("sess-1");
    }

    @Test
    void resetUserPassword_throwsException_forLdapEnterpriseUser() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("ldap_mary");
        user.setFederationLink("ldap-enterprise-provider");

        when(userResource.toRepresentation()).thenReturn(user);

        assertThatThrownBy(() -> adminIdentityService.resetUserPassword("ldap-user-id", "NewPassword!"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Cannot reset password for Enterprise LDAP federated user");

        verify(userResource, never()).resetPassword(any());
        verify(userResource, never()).logout();
    }

    @Test
    void logoutAllSessions_success() {
        UserRepresentation user = new UserRepresentation();
        user.setUsername("customer1");

        when(userResource.toRepresentation()).thenReturn(user);
        when(sessionRepository.findByPrincipalName("customer1")).thenReturn(Map.of("sess-1", session1));

        adminIdentityService.logoutAllSessions("user-123");

        verify(userResource).logout();
        verify(sessionRepository).deleteById("sess-1");
    }
}
