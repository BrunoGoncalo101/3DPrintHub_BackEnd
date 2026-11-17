package pt.utp.estg.pp.printhub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Envio de emails. Por agora é uma simulação: em vez de enviar,
 * regista o conteúdo no log. O envio real fica para as notificações (RF-023).
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    public void sendPasswordResetEmail(String to, String resetLink) {
        log.info("[EMAIL SIMULADO] Para: {} | Assunto: Recuperação da palavra-passe | Link: {}", to, resetLink);
    }
}