package pt.utp.estg.pp.printhub.service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import pt.utp.estg.pp.printhub.entity.User;
import pt.utp.estg.pp.printhub.model.Role;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TokenServiceTest {

    private static final SecretKey KEY = key("chave-de-teste-com-pelo-menos-32-caracteres");

    private final JwtEncoder encoder = new NimbusJwtEncoder(new ImmutableSecret<>(KEY));
    private final JwtDecoder decoder = NimbusJwtDecoder.withSecretKey(KEY).macAlgorithm(MacAlgorithm.HS256).build();

    @Test
    void generateToken_containsUserClaimsAndExpiresIn24Hours() {
        TokenService tokenService = new TokenService(encoder, 24);

        Jwt jwt = decoder.decode(tokenService.generateToken(admin()));

        assertThat(jwt.getSubject()).isEqualTo("3");
        assertThat(jwt.getClaimAsString("email")).isEqualTo("admin@exemplo.pt");
        assertThat(jwt.getClaimAsString("role")).isEqualTo("ADMIN");
        assertThat(Duration.between(jwt.getIssuedAt(), jwt.getExpiresAt())).isEqualTo(Duration.ofHours(24));
        assertThat(tokenService.getExpirationSeconds()).isEqualTo(86400);
    }

    @Test
    void expiredToken_isRejected() {
        // Token emitido há 2 horas e expirado há 1 hora, como um token antigo guardado no browser
        Instant issuedAt = Instant.now().minus(2, ChronoUnit.HOURS);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("printhub")
                .subject("3")
                .claim("role", "ADMIN")
                .issuedAt(issuedAt)
                .expiresAt(issuedAt.plus(1, ChronoUnit.HOURS))
                .build();
        String token = encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(JwtValidationException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void tokenSignedWithAnotherKey_isRejected() {
        JwtEncoder otherEncoder = new NimbusJwtEncoder(new ImmutableSecret<>(key("outra-chave-diferente-com-mais-de-32-caracteres")));
        String token = new TokenService(otherEncoder, 24).generateToken(admin());

        assertThatThrownBy(() -> decoder.decode(token))
                .isInstanceOf(BadJwtException.class);
    }

    private static User admin() {
        User user = new User();
        user.setId(3);
        user.setEmail("admin@exemplo.pt");
        user.setRole(Role.ADMIN);
        return user;
    }

    private static SecretKey key(String secret) {
        return new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }
}