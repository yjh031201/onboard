package com.kanban.backend.auth.oauth2;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OAuthExchangeCodeRepository extends JpaRepository<OAuthExchangeCode, Long> {

    Optional<OAuthExchangeCode> findByCodeHash(String codeHash);
}
