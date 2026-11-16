package pt.utp.estg.pp.printhub.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import pt.utp.estg.pp.printhub.dto.ChangePasswordRequest;
import pt.utp.estg.pp.printhub.dto.UpdateProfileRequest;
import pt.utp.estg.pp.printhub.entity.User;
import pt.utp.estg.pp.printhub.exception.InvalidCredentialsException;
import pt.utp.estg.pp.printhub.exception.InvalidPasswordException;
import pt.utp.estg.pp.printhub.model.Role;
import pt.utp.estg.pp.printhub.repository.UserRepository;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = new User();
        user.setId(1);
        user.setName("Ana");
        user.setEmail("ana@exemplo.pt");
        user.setPasswordHash("hash");
        user.setRole(Role.CUSTOMER);
        user.setActive(true);
    }

    @Test
    void updateProfile_trimsNameAndStoresBlankFieldsAsNull() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));

        userService.updateProfile(1, new UpdateProfileRequest("  Ana Silva  ", "   ", ""));

        assertThat(user.getName()).isEqualTo("Ana Silva");
        assertThat(user.getPhone()).isNull();
        assertThat(user.getAddress()).isNull();
    }

    @Test
    void getCurrentUser_rejectsUnknownUser() {
        when(userRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getCurrentUser(99))
                .isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void changePassword_rejectsWrongCurrentPassword() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("errada123", "hash")).thenReturn(false);

        assertThatThrownBy(() -> userService.changePassword(1, new ChangePasswordRequest("errada123", "novapass123")))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("A palavra-passe atual está incorreta.");

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void changePassword_rejectsNewPasswordEqualToCurrent() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);

        assertThatThrownBy(() -> userService.changePassword(1, new ChangePasswordRequest("password123", "password123")))
                .isInstanceOf(InvalidPasswordException.class)
                .hasMessage("A nova palavra-passe tem de ser diferente da atual.");
    }

    @Test
    void changePassword_storesNewHash() {
        when(userRepository.findById(1)).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("password123", "hash")).thenReturn(true);
        when(passwordEncoder.matches("novapass123", "hash")).thenReturn(false);
        when(passwordEncoder.encode("novapass123")).thenReturn("novo-hash");

        userService.changePassword(1, new ChangePasswordRequest("password123", "novapass123"));

        assertThat(user.getPasswordHash()).isEqualTo("novo-hash");
    }
}