package com.ims.service;

import org.springframework.scheduling.annotation.Async;

public interface EmailService {

	void sendHtmlEmail(String to, String subject, String htmlContent);

}