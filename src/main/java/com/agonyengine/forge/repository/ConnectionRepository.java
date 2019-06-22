package com.agonyengine.forge.repository;

import com.agonyengine.forge.model.Connection;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ConnectionRepository extends JpaRepository<Connection, UUID> {
}
