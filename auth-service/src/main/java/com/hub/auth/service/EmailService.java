package com.hub.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

/**
 * Service d'envoi d'emails via Mailtrap (ou autre SMTP).
 * Séparation des responsabilités : uniquement l'envoi.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@hub.com}")
    private String fromEmail;

    public void sendOtpEmail(String toEmail, String otp, String username) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(toEmail);
        message.setSubject("Code de vérification Hub ERP/CRM");
        message.setText(String.format(
                "Bonjour %s,\n\n" +
                "Votre code de vérification à deux facteurs est : %s\n\n" +
                "Ce code expire dans 5 minutes. Ne le partagez avec personne.\n\n" +
                "— L'équipe Hub ERP/CRM",
                username != null ? username : "Utilisateur",
                otp
        ));
        mailSender.send(message);
        log.info("Email OTP envoyé à {}", toEmail);
    }
}
