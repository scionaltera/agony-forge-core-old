package com.agonyengine.forge.repository;

import com.agonyengine.forge.model.Creature;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface CreatureRepository extends JpaRepository<Creature, UUID> {
    Creature findByConnectionSessionUsernameAndConnectionSessionId(String sessionUsername, String sessionId);
}
