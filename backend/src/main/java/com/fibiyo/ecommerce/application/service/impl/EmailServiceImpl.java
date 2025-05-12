package com.fibiyo.ecommerce.application.service.impl;

import com.fibiyo.ecommerce.application.service.EmailService;
import jakarta.mail.MessagingException; // HTML mail için
import jakarta.mail.internet.MimeMessage; // HTML mail için
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value; // Gönderen adresi için
import org.springframework.mail.MailException; // Hatalar için
import org.springframework.mail.SimpleMailMessage; // Basit metin mail için
import org.springframework.mail.javamail.JavaMailSender; // Mail gönderme sınıfı
import org.springframework.mail.javamail.MimeMessageHelper; // HTML mail helper
import org.springframework.scheduling.annotation.Async; // Asenkron gönderme için
import org.springframework.stereotype.Service;


@Service
public class EmailServiceImpl implements EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailServiceImpl.class);

    private final JavaMailSender emailSender;

    // Opsiyonel: application.properties'dan gönderen adresi al
    @Value("${spring.mail.username}") // Genellikle username gönderen olur
    private String fromAddress;
    // @Value("${mail.sender.name:Fibiyo E-Commerce}") // İsim için default değer
    // private String fromName;


    @Autowired
    public EmailServiceImpl(JavaMailSender emailSender) {
        this.emailSender = emailSender;
    }

    @Override
    @Async // E-posta gönderimini ana thread'den ayır (performans için)
    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress); // veya mail.sender.address
            message.setTo(to);
            message.setSubject(subject);
            message.setText(text);
            emailSender.send(message);
            logger.info("Simple email sent successfully to {}", to);
        } catch (MailException exception) {
             logger.error("Failed to send simple email to {}: {}", to, exception.getMessage(), exception);
             // Hata durumunda ne yapılmalı? Loglamak genellikle yeterlidir.
             // Belki bir notification atılabilir veya tekrar deneme mekanizması eklenebilir.
         }
    }

    @Override
    @Async // Asenkron yapalım
    public void sendHtmlMessage(String to, String subject, String htmlBody) {
         MimeMessage message = emailSender.createMimeMessage();
         try {
             // true -> multipart message (ek için vb. gerekli olabilir)
             // UTF-8 -> karakter kodlaması
             MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromAddress); // Opsiyonel olarak: helper.setFrom(fromAddress, fromName);
             helper.setTo(to);
             helper.setSubject(subject);
             helper.setText(htmlBody, true); // true -> içeriğin HTML olduğunu belirtir

             emailSender.send(message);
             logger.info("HTML email sent successfully to {}", to);
          } catch (MessagingException | MailException e) {
             logger.error("Failed to send HTML email to {}: {}", to, e.getMessage(), e);
         }
     }

    // Attachment metodu
    /*
     @Override
     @Async
     public void sendMessageWithAttachment(String to, String subject, String text, String pathToAttachment) {
         try {
             MimeMessage message = emailSender.createMimeMessage();
             MimeMessageHelper helper = new MimeMessageHelper(message, true); // true -> multipart

             helper.setFrom(fromAddress);
             helper.setTo(to);
             helper.setSubject(subject);
             helper.setText(text);

             FileSystemResource file = new FileSystemResource(new File(pathToAttachment));
             helper.addAttachment(file.getFilename(), file); // Eki ekle

             emailSender.send(message);
             logger.info("Email with attachment sent successfully to {}", to);
         } catch (MessagingException | MailException e) {
              logger.error("Failed to send email with attachment to {}: {}", to, e.getMessage(), e);
         }
     }
    */

}