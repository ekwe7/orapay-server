package com.orapay.split.model;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SplitOrderRepository extends JpaRepository<SplitOrder, UUID> {
}
