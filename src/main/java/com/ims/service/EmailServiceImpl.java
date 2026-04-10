package com.ims.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class EmailServiceImpl implements EmailService {
	
	@Autowired
    private JavaMailSender mailSender;
	
	@Value("${spring.mail.username}")
    private String senderEmail;
	
	@Override
	@Async
    public void sendHtmlEmail(String to, String subject, String htmlContent) {
        try {
            log.info("Sending HTML email to: {}", to);
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            
            helper.setFrom(senderEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true); 
            
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send HTML email: {}", e.getMessage());
        }
    }
	
}
