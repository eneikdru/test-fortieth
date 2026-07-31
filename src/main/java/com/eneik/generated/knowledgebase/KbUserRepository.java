package com.eneik.generated.knowledgebase;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface KbUserRepository extends JpaRepository<KbUser, Long> {
    Optional<KbUser> findByUsername(String username);
}
