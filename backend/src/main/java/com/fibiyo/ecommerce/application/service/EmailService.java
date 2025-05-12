package com.fibiyo.ecommerce.application.service;

/**
 * E-posta gönderme işlemleri için arayüz.
 */
public interface EmailService {

    /**
     * Basit bir metin e-postası gönderir.
     *
     * @param to      Alıcı e-posta adresi.
     * @param subject E-posta konusu.
     * @param text    E-posta içeriği (düz metin).
     */
    void sendSimpleMessage(String to, String subject, String text);

    /**
     * HTML içerikli bir e-posta gönderir.
     *
     * @param to      Alıcı e-posta adresi.
     * @param subject E-posta konusu.
     * @param htmlBody E-posta içeriği (HTML formatında).
     */
    void sendHtmlMessage(String to, String subject, String htmlBody);

    /**
     * Eki olan bir e-posta gönderir (İleride gerekebilir).
     * @param to Alıcı e-posta adresi.
     * @param subject E-posta konusu.
     * @param text E-posta içeriği.
     * @param pathToAttachment Eklenecek dosyanın yolu.
     */
    // void sendMessageWithAttachment(String to, String subject, String text, String pathToAttachment);

}