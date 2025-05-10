package com.fvp.repository;

import com.fvp.entity.LinkModelShard1;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LinkModelShard1Repository extends JpaRepository<LinkModelShard1, Long> {
    // All query methods are now implemented in LinkModelDynamicQueryBuilder
    // This repository interface is kept for JPA entity management
} 