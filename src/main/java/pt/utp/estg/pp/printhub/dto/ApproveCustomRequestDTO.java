package pt.utp.estg.pp.printhub.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;

public record ApproveCustomRequestDTO(
        @NotNull @Positive BigDecimal finalPrice
) {}