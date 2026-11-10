
package pt.utp.estg.pp.printhub.dto;

public record ColorDTO(
        Integer id,
        String name,
        String hexCode,
        Boolean available,
        Integer materialId
) {
}
