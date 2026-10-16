package com.serveless.envio.service;

import com.serveless.envio.dto.SendEmailRequest;
import com.serveless.envio.dto.SendEmailResponse;

public interface EmailService {

	SendEmailResponse send(SendEmailRequest request);
}