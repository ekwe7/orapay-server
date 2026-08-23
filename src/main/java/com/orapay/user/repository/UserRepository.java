package com.orapay.user.repository;

import com.orapay.user.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<User, UUID> {

    boolean existsByUserEmailAddress(String userEmailAddress);

    boolean existsByPhoneNumber(String PhoneNumber);

    Optional<User> findByUserEmailAddress(String userEmailAddress);

    Optional<User> findByPhoneNumber(String PhoneNumber);
}
