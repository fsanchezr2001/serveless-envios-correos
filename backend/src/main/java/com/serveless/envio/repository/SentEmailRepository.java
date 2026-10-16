package com.serveless.envio.repository;

import com.serveless.envio.domain.SentEmail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

public interface SentEmailRepository extends JpaRepository<SentEmail, String> {

	/**
	 * Atomically transitions a row to {@code SENT}. Runs as a single UPDATE through
	 * the repository proxy, so it is committed independently of the caller.
	 */
	@Modifying
	@Transactional
	@Query("update SentEmail e set e.status = 'SENT', e.providerMessageId = :providerMessageId, "
			+ "e.sentAt = :sentAt where e.id = :id")
	int markSent(@Param("id") String id,
			@Param("providerMessageId") String providerMessageId,
			@Param("sentAt") Instant sentAt);

	/**
	 * Atomically transitions a row to {@code FAILED} and records the error.
	 */
	@Modifying
	@Transactional
	@Query("update SentEmail e set e.status = 'FAILED', e.errorMessage = :errorMessage, "
			+ "e.sentAt = :sentAt where e.id = :id")
	int markFailed(@Param("id") String id,
			@Param("errorMessage") String errorMessage,
			@Param("sentAt") Instant sentAt);
}
