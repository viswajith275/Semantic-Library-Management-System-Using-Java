package com.library.backend.repository;

import com.library.backend.dto.MemberSummaryDto;
import com.library.backend.entity.Role;
import com.library.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailIgnoreCase(String email);

    Optional<User> findByCustomCode(String customCode);

    Optional<User> findByApiToken(String apiToken);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByCustomCode(String customCode);

    boolean existsByRole(Role role);

    @Query("""
        SELECT new com.library.backend.dto.MemberSummaryDto(
            u.id, u.name, u.email, u.customCode, u.active, COUNT(lr.id)
        )
        FROM User u
        LEFT JOIN LoanRecord lr ON lr.member = u AND lr.returnedAt IS NULL
        WHERE u.role = com.library.backend.entity.Role.MEMBER
        GROUP BY u.id, u.name, u.email, u.customCode, u.active
        ORDER BY u.id DESC
    """)
    // shortcut: returns every member; add paging when a branch holds thousands of them.
    List<MemberSummaryDto> findAllMembersWithActiveLoanCount();
}
