package pt.utp.estg.pp.printhub.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;
import pt.utp.estg.pp.printhub.dto.ChangePasswordRequest;
import pt.utp.estg.pp.printhub.dto.UpdateProfileRequest;
import pt.utp.estg.pp.printhub.dto.UserResponse;
import pt.utp.estg.pp.printhub.service.UserService;

/** Perfil do utilizador com sessão iniciada (RF-003). */
@RestController
@RequestMapping("/utilizadores")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return userService.getCurrentUser(userId(jwt));
    }

    @PutMapping("/me")
    public UserResponse updateMe(@AuthenticationPrincipal Jwt jwt,
                                 @Valid @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(userId(jwt), request);
    }

    @PutMapping("/me/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void changePassword(@AuthenticationPrincipal Jwt jwt,
                               @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId(jwt), request);
    }

    /** O id do utilizador vai no "subject" do token. */
    private static Integer userId(Jwt jwt) {
        return Integer.valueOf(jwt.getSubject());
    }
}