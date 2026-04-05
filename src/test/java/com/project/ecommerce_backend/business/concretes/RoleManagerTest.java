package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.entities.concretes.Role;
import com.project.ecommerce_backend.repositories.abstracts.RoleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RoleManagerTest {

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private MessageService messageService;

    @InjectMocks
    private RoleManager roleManager;

    @Test
    void findRoleByName_whenRoleExists_shouldReturnRole() {
        String roleName = "ROLE_USER";
        Role role = new Role();
        role.setName(roleName);

        when(roleRepository.findByName(roleName)).thenReturn(Optional.of(role));

        Role result = roleManager.findRoleByName(roleName);

        assertNotNull(result);
        assertEquals(roleName, result.getName());
        verify(roleRepository).findByName(roleName);
    }

    @Test
    void findRoleByName_whenRoleMissing_shouldThrowNotFoundException() {
        String roleName = "ROLE_UNKNOWN";
        when(roleRepository.findByName(roleName)).thenReturn(Optional.empty());
        when(messageService.getMessageWithParams(any(), any())).thenReturn("Error");

        assertThrows(NotFoundException.class, () -> roleManager.findRoleByName(roleName));
    }
}
