package pt.utp.estg.pp.printhub.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.utp.estg.pp.printhub.dto.ChangePasswordRequest;
import pt.utp.estg.pp.printhub.dto.UpdateProfileRequest;
import pt.utp.estg.pp.printhub.dto.UserResponse;
import pt.utp.estg.pp.printhub.entity.User;
import pt.utp.estg.pp.printhub.exception.InvalidCredentialsException;
import pt.utp.estg.pp.printhub.exception.InvalidPasswordException;
import pt.utp.estg.pp.printhub.repository.UserRepository;

/**
 * Dados e perfil do utilizador com sessão iniciada (RF-003).
 */
@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(Integer userId) {
        return UserResponse.from(findUser(userId));
    }

    @Transactional
    public UserResponse updateProfile(Integer userId, UpdateProfileRequest request) {
        User user = findUser(userId);
        user.setName(request.name().trim());
        user.setPhone(blankToNull(request.phone()));
        user.setAddress(blankToNull(request.address()));
        return UserResponse.from(user);
    }

    @Transactional
    public void changePassword(Integer userId, ChangePasswordRequest request) {
        User user = findUser(userId);

        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("A palavra-passe atual está incorreta.");
        }
        if (passwordEncoder.matches(request.newPassword(), user.getPasswordHash())) {
            throw new InvalidPasswordException("A nova palavra-passe tem de ser diferente da atual.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.newPassword()));
    }


    private User findUser(Integer userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new InvalidCredentialsException("Sessão inválida."));
    }

    /**
     * Guarda campos opcionais vazios como null, em vez de texto em branco.
     */
    private static String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}