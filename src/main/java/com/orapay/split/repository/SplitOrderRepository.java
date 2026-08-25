package com.orapay.split.repository;

import com.orapay.split.model.SplitOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SplitOrderRepository extends JpaRepository<SplitOrder, UUID> {
}
