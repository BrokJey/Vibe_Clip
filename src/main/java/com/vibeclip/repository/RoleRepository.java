package com.vibeclip.repository;

import com.vibeclip.entity.Role;
import com.vibeclip.entity.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);
}

