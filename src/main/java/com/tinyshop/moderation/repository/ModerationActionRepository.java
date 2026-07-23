package com.tinyshop.moderation.repository;

import com.tinyshop.moderation.domain.ModerationAction;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModerationActionRepository extends JpaRepository<ModerationAction, UUID> {
}
