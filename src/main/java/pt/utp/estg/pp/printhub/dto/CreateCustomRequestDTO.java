package pt.utp.estg.pp.printhub.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record CreateCustomRequestDTO(
        @NotNull Integer fileId,
        @NotNull Integer materialId,
        @NotNull Integer colorId,
        @NotNull @Min(1) Integer quantity,
        @NotNull @Min(10) Integer infillPercent,
        @NotNull Integer userId
) {}