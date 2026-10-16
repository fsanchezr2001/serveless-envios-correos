package com.serveless.envio.repository;

import com.serveless.envio.domain.SentEmail;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SentEmailRepository extends JpaRepository<SentEmail, String> {
}