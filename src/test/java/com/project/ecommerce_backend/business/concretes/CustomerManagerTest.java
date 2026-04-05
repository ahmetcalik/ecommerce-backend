package com.project.ecommerce_backend.business.concretes;

import com.project.ecommerce_backend.core.security.abstracts.JwtService;
import com.project.ecommerce_backend.business.abstracts.RoleService;
import com.project.ecommerce_backend.business.dtos.requests.auth.RegisterRequest;
import com.project.ecommerce_backend.business.dtos.responses.auth.AuthenticationResponse;
import com.project.ecommerce_backend.business.helpers.CacheHelper;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.exceptions.types.BusinessException;
import com.project.ecommerce_backend.core.exceptions.types.NotFoundException;
import com.project.ecommerce_backend.core.internationalization.MessageService;
import com.project.ecommerce_backend.core.utils.result.DataResult;
import com.project.ecommerce_backend.core.utils.result.Result;
import com.project.ecommerce_backend.entities.concretes.Customer;
import com.project.ecommerce_backend.entities.concretes.Role;
import com.project.ecommerce_backend.repositories.abstracts.CustomerRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerManagerTest {

    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private RoleService roleService;
    @Mock
    private MessageService messageService;
    @Mock
    private JwtService jwtService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CacheManager cacheManager;
    @Mock
    private CacheHelper cacheHelper;

    @InjectMocks
    private CustomerManager customerManager;

    private Customer customer;
    private Role roleUser;
    private Role roleAdmin;

    @BeforeEach
    void setUp() {
        roleUser = new Role();
        roleUser.setId(1L);
        roleUser.setName("ROLE_USER");

        roleAdmin = new Role();
        roleAdmin.setId(2L);
        roleAdmin.setName("ROLE_ADMIN");

        customer = new Customer();
        customer.setId(1L);
        customer.setEmailAddress("test@test.com");
        customer.setPasswordHash("encodedPassword");
        customer.setRoles(new HashSet<>(Set.of(roleUser)));
        customer.setIsActive(true);
    }

    // --- REGISTER TESTS ---

    @Test
    void register_ShouldRegisterUser_WhenEmailIsUnique() {
        RegisterRequest request = new RegisterRequest();
        request.setContactName("Test User");
        request.setEmailAddress("new@test.com");
        request.setPhoneNumber("1234567890");
        request.setPassword("password");

        when(customerRepository.findByEmailAddress(request.getEmailAddress())).thenReturn(Optional.empty());
        when(roleService.findRoleByName("ROLE_USER")).thenReturn(roleUser);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encodedPassword");
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("accessToken");
        when(jwtService.generateRefreshToken(any(UserDetails.class))).thenReturn("refreshToken");
        when(jwtService.extractExpiration(anyString())).thenReturn(new Date());
        when(messageService.getMessage(Messages.Auth.REGISTER_SUCCESSFUL)).thenReturn("Registration successful");

        DataResult<AuthenticationResponse> result = customerManager.register(request);

        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertEquals("accessToken", result.getData().getAccessToken());
        assertEquals("Registration successful", result.getMessage());

        verify(customerRepository).save(any(Customer.class));
    }

    @Test
    void register_ShouldThrowBusinessException_WhenEmailAlreadyExists() {
        RegisterRequest request = new RegisterRequest();
        request.setEmailAddress("test@test.com");

        when(customerRepository.findByEmailAddress(request.getEmailAddress())).thenReturn(Optional.of(customer));
        when(messageService.getMessage(Messages.Auth.EMAIL_ALREADY_EXISTS)).thenReturn("Email already exists");

        BusinessException exception = assertThrows(BusinessException.class, () -> customerManager.register(request));
        assertEquals("Email already exists", exception.getMessage());

        verify(customerRepository, never()).save(any(Customer.class));
    }

    // --- LOAD USER BY USERNAME TESTS ---

    @Test
    void loadUserByUsername_ShouldReturnUserDetails_WhenUserExists() {
        when(customerRepository.findByEmailAddress("test@test.com")).thenReturn(Optional.of(customer));

        UserDetails userDetails = customerManager.loadUserByUsername("test@test.com");

        assertNotNull(userDetails);
        assertEquals(customer.getEmailAddress(), userDetails.getUsername());
    }

    @Test
    void loadUserByUsername_ShouldThrowUsernameNotFoundException_WhenUserDoesNotExist() {
        when(customerRepository.findByEmailAddress("unknown@test.com")).thenReturn(Optional.empty());
        when(messageService.getMessageWithParams(Messages.Auth.USER_NOT_FOUND_WITH_EMAIL_ADDRESS, "unknown@test.com"))
                .thenReturn("User not found");

        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class,
                () -> customerManager.loadUserByUsername("unknown@test.com"));
        assertEquals("User not found", exception.getMessage());
    }

    // --- GRANT ROLE TO USER TESTS ---

    @Test
    void grantRoleToUser_ShouldAddRole_WhenUserExists() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(roleService.findRoleByName("ROLE_ADMIN")).thenReturn(roleAdmin);
        when(messageService.getMessage(Messages.Auth.ROLE_GRANTED_SUCCESSFULLY)).thenReturn("Role granted");

        Result result = customerManager.grantRoleToUser(1L, "ADMIN");

        assertTrue(result.isSuccess());
        assertTrue(customer.getRoles().contains(roleAdmin));
        assertEquals("Role granted", result.getMessage());

        verify(customerRepository).save(customer);
    }

    @Test
    void grantRoleToUser_ShouldHandleRoleWithPrefix() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(roleService.findRoleByName("ROLE_ADMIN")).thenReturn(roleAdmin);
        when(messageService.getMessage(Messages.Auth.ROLE_GRANTED_SUCCESSFULLY)).thenReturn("Role granted");

        Result result = customerManager.grantRoleToUser(1L, "ROLE_ADMIN");

        assertTrue(result.isSuccess());
        assertTrue(customer.getRoles().contains(roleAdmin));
        verify(roleService).findRoleByName("ROLE_ADMIN");
    }

    @Test
    void grantRoleToUser_ShouldThrowNotFoundException_WhenUserDoesNotExist() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageService.getMessageWithParams(Messages.Auth.USER_NOT_FOUND_WITH_ID, 99L))
                .thenReturn("User not found");

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> customerManager.grantRoleToUser(99L, "ADMIN"));
        assertEquals("User not found", exception.getMessage());

        verify(customerRepository, never()).save(any(Customer.class));
    }

    // --- REVOKE ROLE FROM USER TESTS ---

    @Test
    void revokeRoleFromUser_ShouldRemoveRole_WhenUserHasRole() {
        customer.getRoles().add(roleAdmin);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(roleService.findRoleByName("ROLE_ADMIN")).thenReturn(roleAdmin);
        when(messageService.getMessage(Messages.Auth.ROLE_REVOKED_SUCCESSFULLY)).thenReturn("Role revoked");

        Result result = customerManager.revokeRoleFromUser(1L, "ADMIN");

        assertTrue(result.isSuccess());
        assertFalse(customer.getRoles().contains(roleAdmin));
        assertEquals("Role revoked", result.getMessage());

        verify(customerRepository).save(customer);
    }

    @Test
    void revokeRoleFromUser_ShouldHandleRoleWithPrefix() {
        customer.getRoles().add(roleAdmin);
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(roleService.findRoleByName("ROLE_ADMIN")).thenReturn(roleAdmin);
        when(messageService.getMessage(Messages.Auth.ROLE_REVOKED_SUCCESSFULLY)).thenReturn("Role revoked");

        Result result = customerManager.revokeRoleFromUser(1L, "ROLE_ADMIN");

        assertTrue(result.isSuccess());
        assertFalse(customer.getRoles().contains(roleAdmin));
    }

    @Test
    void revokeRoleFromUser_ShouldThrowBusinessException_WhenUserDoesNotHaveRole() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(roleService.findRoleByName("ROLE_ADMIN")).thenReturn(roleAdmin);
        when(messageService.getMessage(Messages.Auth.USER_DOES_NOT_HAVE_ROLE)).thenReturn("User does not have role");

        BusinessException exception = assertThrows(BusinessException.class,
                () -> customerManager.revokeRoleFromUser(1L, "ADMIN"));
        assertEquals("User does not have role", exception.getMessage());

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void revokeRoleFromUser_ShouldThrowNotFoundException_WhenUserDoesNotExist() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageService.getMessageWithParams(Messages.Auth.USER_NOT_FOUND_WITH_ID, 99L))
                .thenReturn("User not found");

        NotFoundException exception = assertThrows(NotFoundException.class,
                () -> customerManager.revokeRoleFromUser(99L, "ADMIN"));
        assertEquals("User not found", exception.getMessage());
    }

    // --- DELETE USER TESTS ---

    @Test
    void deleteUser_ShouldDeleteUser_WhenConstraintsMet() {
        Customer userToDelete = new Customer();
        userToDelete.setId(2L);
        userToDelete.setEmailAddress("delete@test.com");
        userToDelete.setRoles(new HashSet<>(Set.of(roleUser)));
        userToDelete.setIsActive(true);

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(1L); // Admin ID

        when(customerRepository.findById(2L)).thenReturn(Optional.of(userToDelete));
        when(messageService.getMessage(Messages.Auth.USER_DELETED_SUCCESSFULLY)).thenReturn("User deleted");

        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("customer")).thenReturn(cache);

        Result result = customerManager.deleteUser(2L);

        assertTrue(result.isSuccess());
        assertFalse(userToDelete.getIsActive());
        assertEquals("User deleted", result.getMessage());

        verify(customerRepository).save(userToDelete);
        verify(cache).evict("email:delete@test.com");
    }

    @Test
    void deleteUser_ShouldDeleteAdmin_WhenOtherAdminsExist() {
        Customer adminToDelete = new Customer();
        adminToDelete.setId(2L);
        adminToDelete.setEmailAddress("admin@test.com");
        adminToDelete.setRoles(new HashSet<>(Set.of(roleAdmin)));
        adminToDelete.setIsActive(true);

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(1L); // Another admin performing deletion

        when(customerRepository.findById(2L)).thenReturn(Optional.of(adminToDelete));
        // isDeletingAdmin is true.
        when(customerRepository.countByRoles_Name("ROLE_ADMIN")).thenReturn(2L); // More than 1 admin

        when(messageService.getMessage(Messages.Auth.USER_DELETED_SUCCESSFULLY)).thenReturn("User deleted");

        Cache cache = mock(Cache.class);
        when(cacheManager.getCache("customer")).thenReturn(cache);

        Result result = customerManager.deleteUser(2L);

        assertTrue(result.isSuccess());
        assertFalse(adminToDelete.getIsActive());
        verify(customerRepository).save(adminToDelete);
    }

    @Test
    void deleteUser_ShouldNotEvictCache_WhenCacheNotFound() {
        Customer userToDelete = new Customer();
        userToDelete.setId(2L);
        userToDelete.setEmailAddress("delete@test.com");
        userToDelete.setRoles(new HashSet<>(Set.of(roleUser)));
        userToDelete.setIsActive(true);

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(1L);

        when(customerRepository.findById(2L)).thenReturn(Optional.of(userToDelete));
        when(messageService.getMessage(Messages.Auth.USER_DELETED_SUCCESSFULLY)).thenReturn("User deleted");

        when(cacheManager.getCache("customer")).thenReturn(null); // Cache not found

        Result result = customerManager.deleteUser(2L);

        assertTrue(result.isSuccess());
        verify(customerRepository).save(userToDelete);
        // No exception thrown
    }

    @Test
    void deleteUser_ShouldThrowBusinessException_WhenAdminTriesToDeleteSelf() {
        when(cacheHelper.getAuthenticatedUserId()).thenReturn(1L); // Same ID as user to delete

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(messageService.getMessage(Messages.Auth.ADMIN_CANNOT_DELETE_HIMSELF)).thenReturn("Admin cannot delete himself");

        BusinessException exception = assertThrows(BusinessException.class, () -> customerManager.deleteUser(1L));
        assertEquals("Admin cannot delete himself", exception.getMessage());

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void deleteUser_ShouldThrowBusinessException_WhenDeletingLastAdmin() {
        Customer adminToDelete = new Customer();
        adminToDelete.setId(2L);
        adminToDelete.setRoles(new HashSet<>(Set.of(roleAdmin)));

        when(cacheHelper.getAuthenticatedUserId()).thenReturn(1L); // Another admin performing deletion

        when(customerRepository.findById(2L)).thenReturn(Optional.of(adminToDelete));
        when(customerRepository.countByRoles_Name("ROLE_ADMIN")).thenReturn(1L); // Only 1 admin left
        when(messageService.getMessage(Messages.Auth.CANNOT_DELETE_LAST_ADMIN)).thenReturn("Cannot delete last admin");

        BusinessException exception = assertThrows(BusinessException.class, () -> customerManager.deleteUser(2L));
        assertEquals("Cannot delete last admin", exception.getMessage());

        verify(customerRepository, never()).save(any(Customer.class));
    }

    @Test
    void deleteUser_ShouldThrowNotFoundException_WhenUserDoesNotExist() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageService.getMessageWithParams(Messages.Auth.USER_NOT_FOUND_WITH_ID, 99L))
                .thenReturn("User not found");

        NotFoundException exception = assertThrows(NotFoundException.class, () -> customerManager.deleteUser(99L));
        assertEquals("User not found", exception.getMessage());
    }

    // --- GET BY ID AS ENTITY TESTS ---

    @Test
    void getByIdAsEntity_ShouldReturnCustomer_WhenUserExists() {
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        Customer result = customerManager.getByIdAsEntity(1L);

        assertNotNull(result);
        assertEquals(customer.getId(), result.getId());
    }

    @Test
    void getByIdAsEntity_ShouldThrowNotFoundException_WhenUserDoesNotExist() {
        when(customerRepository.findById(99L)).thenReturn(Optional.empty());
        when(messageService.getMessageWithParams(Messages.Auth.USER_NOT_FOUND_WITH_ID, 99L))
                .thenReturn("User not found");

        NotFoundException exception = assertThrows(NotFoundException.class, () -> customerManager.getByIdAsEntity(99L));
        assertEquals("User not found", exception.getMessage());
    }
}