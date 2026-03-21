package com.ims.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailService {
	
	@Autowired
    private JavaMailSender mailSender;
	
	@Value("${spring.mail.username}")
    private String senderEmail;
	
	@Async
	public void sendSimpleEmail(String to,String subject,String body) {
		log.info("In email service "+to+" "+subject+" "+body);
		SimpleMailMessage message = new SimpleMailMessage();
		message.setFrom(senderEmail);
		message.setTo(to);
		message.setSubject(subject);
		message.setText(body);
		mailSender.send(message);
	}
	
}
