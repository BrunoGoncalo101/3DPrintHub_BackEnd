package pt.utp.estg.pp.printhub.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.utp.estg.pp.printhub.dto.ResetPasswordRequest;
import pt.utp.estg.pp.printhub.entity.User;
import pt.utp.estg.pp.printhub.exception.InvalidPasswordException;
import pt.utp.estg.pp.printhub.repository.UserRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

/** Recuperação da palavra-passe por link enviado para o email (RF-004). */
@Service
public class PasswordResetService {

    private static final Duration TOKEN_VALIDITY = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final String frontendUrl;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(UserRepository userRepository, PasswordEncoder passwordEncoder,
                                EmailService emailService, @Value("${app.frontend.url}") String frontendUrl) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
        this.frontendUrl = frontendUrl;
    }

    /** Gera um token e "envia" o link. Se o email não existir, não faz nada (e não o revela). */
    @Transactional
    public void requestReset(String email) {
        userRepository.findByEmail(email.trim().toLowerCase())
                .filter(user -> Boolean.TRUE.equals(user.getActive()))
                .ifPresent(user -> {
                    String token = generateToken();
                    user.setResetTokenHash(hash(token));
                    user.setResetTokenExpiresAt(LocalDateTime.now().plus(TOKEN_VALIDITY));
                    emailService.sendPasswordResetEmail(user.getEmail(),
                            frontendUrl + "/reset-password?token=" + token);
                });
    }

    /** Define a nova palavra-passe se o token for válido; o token deixa de funcionar a seguir. */
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByResetTokenHash(hash(request.token()))
                .filter(u -> u.getResetTokenExpiresAt() != null
                        && u.getResetTokenExpiresAt().isAfter(LocalDateTime.now()))
                .orElseThrow(() -> new InvalidPasswordException("O link de recuperação é inválido ou expirou."));

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        user.setResetTokenHash(null);
        user.setResetTokenExpiresAt(null);
    }

    /** 32 bytes aleatórios, em Base64 seguro para URLs (43 caracteres). */
    private String generateToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 indisponível", e);
        }
    }
}