package com.cairo.cairobackend.repository;

import com.cairo.cairobackend.entity.AiInteraction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AiInteractionRepository extends JpaRepository<AiInteraction, Long> {
    // Used for monitoring: how many times has a user used each feature?
    long countByUserIdAndFeature(Long userId, AiInteraction.AiFeature feature);
}