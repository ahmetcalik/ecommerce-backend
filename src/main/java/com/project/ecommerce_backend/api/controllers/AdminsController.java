package com.project.ecommerce_backend.api.controllers;

import com.project.ecommerce_backend.business.abstracts.CustomerService;
import com.project.ecommerce_backend.core.constants.Messages;
import com.project.ecommerce_backend.core.utils.result.Result;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * Bu controller, sistemin "Komuta Merkezi" olarak görev yapar ve sadece 'ADMIN' rolüne sahip üst düzey yetkililerin erişimine açıktır.
 * Kullanıcıların yetki seviyelerini değiştirmek veya sistemden kullanıcı uzaklaştırmak gibi kritik yönetimsel operasyonlar buradan yürütülür.
 * Güvenlik tasarımı gereği metod bazlı değil, doğrudan sınıf seviyesinde yetki kontrolü yapılarak en ufak bir sızıntının önüne geçilmesi hedeflenmiştir.
 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Validated
public class AdminsController {

    private final CustomerService customerService;

    /**
     * Mevcut bir kullanıcıya yeni yetkiler tanımlamak için kullanılır.
     * Bu işlem kullanıcının sistemdeki erişim sınırlarını genişlettiği için titizlikle kullanılmalıdır.
     */
    @PutMapping("/users/{customerId}/grant-role/{roleName}")
    @ResponseStatus(HttpStatus.OK)
    public Result grantRoleToUser(
            @PathVariable @Min(value = 1, message = Messages.Validations.Auth.CUSTOMER_ID_INVALID) Long customerId,
            @PathVariable @NotBlank(message = Messages.Validations.Auth.ROLE_NAME_REQUIRED) String roleName) {
        return customerService.grantRoleToUser(customerId, roleName);
    }

    /**
     * Bir kullanıcının artık sahip olmaması gereken yetkilerini sistemden geri alır.
     */
    @PutMapping("/users/{customerId}/revoke-role/{roleName}")
    @ResponseStatus(HttpStatus.OK)
    public Result revokeRoleFromUser(
            @PathVariable @Min(value = 1, message = Messages.Validations.Auth.CUSTOMER_ID_INVALID) Long customerId,
            @PathVariable @NotBlank(message = Messages.Validations.Auth.ROLE_NAME_REQUIRED) String roleName) {
        return customerService.revokeRoleFromUser(customerId, roleName);
    }

    /**
     * Sistem kurallarını ihlal eden veya üyeliğini sonlandırmak isteyen kullanıcıları pasif hale getiririz.
     * Veri bütünlüğünü bozmamak adına fiziksel silme yerine genellikle "soft delete" (pasife çekme) yöntemi tercih edilir.
     */
    @DeleteMapping("/users/{customerId}")
    @ResponseStatus(HttpStatus.OK)
    public Result deleteUser(
            @PathVariable @Min(value = 1, message = Messages.Validations.Auth.CUSTOMER_ID_INVALID) Long customerId) {
        return customerService.deleteUser(customerId);
    }
}