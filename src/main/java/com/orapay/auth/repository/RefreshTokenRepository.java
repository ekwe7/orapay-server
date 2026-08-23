package com.orapay.auth.repository;

import com.orapay.auth.model.RefreshToken;
import com.orapay.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenValue(String tokenValue);

    void deleteByUser(User user);

    void deleteByTokenValue(String tokenValue);
}
