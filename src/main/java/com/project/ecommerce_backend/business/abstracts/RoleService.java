package com.project.ecommerce_backend.business.abstracts;

import com.project.ecommerce_backend.entities.concretes.Role;

public interface RoleService {

    Role findRoleByName(String name);

}
