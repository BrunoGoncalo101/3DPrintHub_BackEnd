package pt.utp.estg.pp.printhub.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.utp.estg.pp.printhub.dto.LoginRequest;
import pt.utp.estg.pp.printhub.dto.LoginResponse;
import pt.utp.estg.pp.printhub.dto.RegisterRequest;
import pt.utp.estg.pp.printhub.dto.UserResponse;
import pt.utp.estg.pp.printhub.entity.User;
import pt.utp.estg.pp.printhub.exception.EmailAlreadyExistsException;
import pt.utp.estg.pp.printhub.exception.InvalidCredentialsException;
import pt.utp.estg.pp.printhub.model.Role;
import pt.utp.estg.pp.printhub.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TokenService tokenService;

    @InjectMocks
    private AuthService authService;

    @Test
    void register_createsCustomerWithEncodedPasswordAndLowercaseEmail() {
        RegisterRequest request = new RegisterRequest("  Ana Silva  ", "Ana@Exemplo.PT", "password123", "912345678", true);
        when(userRepository.existsByEmail("ana@exemplo.pt")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("hash");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        UserResponse response = authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Ana Silva");
        assertThat(saved.getEmail()).isEqualTo("ana@exemplo.pt");
        assertThat(saved.getPasswordHash()).isEqualTo("hash");
        assertThat(saved.getRole()).isEqualTo(Role.CUSTOMER);
        assertThat(saved.getTermsAcceptedAt()).isNotNull();
        assertThat(response.email()).isEqualTo("ana@exemplo.pt");
    }

    @Test
    void register_rejectsDuplicateEmail() {
        when(userRepository.existsByEmail("ana@exemplo.pt")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Ana", "ana@exemplo.pt", "password123", null, true)))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void login_returnsTokenWhenCredentialsAreValid() {
        User user = user(1, "ana@exemplo.pt", "hash");
        when(userRepository.findByEmail("ana@exemplo.pt")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);
        when(tokenService.generateToken(user)).thenReturn("jwt-token");
        when(tokenService.getExpirationSeconds()).thenReturn(86400L);

        LoginResponse response = authService.login(new LoginRequest("ANA@exemplo.pt", "password123"));

        assertThat(response.token()).isEqualTo("jwt-token");
        assertThat(response.tokenType()).isEqualTo("Bearer");
        assertThat(response.expiresIn()).isEqualTo(86400L);
        assertThat(response.user().id()).isEqualTo(1);
    }

    @Test
    void login_rejectsWrongPassword() {
        User user = user(1, "ana@exemplo.pt", "hash");
        when(userRepository.findByEmail("ana@exemplo.pt")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada123", "hash")).thenReturn(false);

        assertThatThrownBy(() -> authService.login(new LoginRequest("ana@exemplo.pt", "errada123")))
                .isInstanceOf(InvalidCredentialsException.class);

        verify(tokenService, never()).generateToken(any());
    }

    @Test
    void login_rejectsUnknownEmail() {
        when(userRepository.findByEmail("ninguem@exemplo.pt")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(new LoginRequest("ninguem@exemplo.pt", "password123")))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    private static User user(Integer id, String email, String passwordHash) {
        User user = new User();
        user.setId(id);
        user.setName("Ana");
        user.setEmail(email);
        user.setPasswordHash(passwordHash);
        user.setRole(Role.CUSTOMER);
        user.setActive(true);
        return user;
    }
}