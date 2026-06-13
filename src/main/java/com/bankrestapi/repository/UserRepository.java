package com.bankrestapi.repository;

import com.bankrestapi.dto.UserDtos.UserResponse;
import com.bankrestapi.model.User;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.repository.*;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByUsernameOrEmailOrPhone(String username, String email, String phone);

    @Query("""
        select new com.bankrestapi.dto.UserDtos$UserResponse(
            u.id, u.username, u.email, u.fullName, u.phone, u.role, u.enabled, u.kyc, u.createdAt)
        from User u
        where lower(u.username) like lower(concat('%', :search, '%'))
           or lower(u.fullName) like lower(concat('%', :search, '%'))
           or lower(u.email) like lower(concat('%', :search, '%'))
        """)
    Page<UserResponse> projectUsers(String search, Pageable pageable);
}
