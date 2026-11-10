
package pt.utp.estg.pp.printhub.service;

import org.springframework.stereotype.Service;
import pt.utp.estg.pp.printhub.dto.ColorDTO;
import pt.utp.estg.pp.printhub.entity.Color;
import pt.utp.estg.pp.printhub.entity.Material;
import pt.utp.estg.pp.printhub.repository.ColorRepository;
import pt.utp.estg.pp.printhub.repository.MaterialRepository;

import java.util.NoSuchElementException;

@Service
public class ColorService {

    private final ColorRepository colorRepository;
    private final MaterialRepository materialRepository;

    public ColorService(
            ColorRepository colorRepository,
            MaterialRepository materialRepository) {
        this.colorRepository = colorRepository;
        this.materialRepository = materialRepository;
    }

    // Criar cor
    public ColorDTO createColor(ColorDTO dto) {

        validateColor(dto);

        String name = dto.name().trim();
        Material material = findActiveMaterial(dto.materialId());

        if (colorRepository.existsByNameIgnoreCaseAndMaterialId(
                name, dto.materialId())) {
            throw new IllegalArgumentException(
                    "Já existe uma cor com este nome neste material."
            );
        }

        Color color = new Color();
        color.setName(name);
        color.setHexCode(dto.hexCode().trim().toUpperCase());
        color.setAvailable(true);
        color.setMaterial(material);

        return toDTO(colorRepository.save(color));
    }

    // Editar cor
    public ColorDTO updateColor(Integer id, ColorDTO dto) {

        Color color = colorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Cor não encontrada."
                ));

        validateColor(dto);

        String name = dto.name().trim();
        Material material = findActiveMaterial(dto.materialId());

        if (colorRepository.existsByNameIgnoreCaseAndMaterialIdAndIdNot(
                name, dto.materialId(), id)) {
            throw new IllegalArgumentException(
                    "Já existe outra cor com este nome neste material."
            );
        }

        color.setName(name);
        color.setHexCode(dto.hexCode().trim().toUpperCase());
        color.setMaterial(material);

        if (dto.available() != null) {
            color.setAvailable(dto.available());
        }

        return toDTO(colorRepository.save(color));
    }

    // Tornar cor indisponível
    public void deleteColor(Integer id) {

        Color color = colorRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException(
                        "Cor não encontrada."
                ));

        color.setAvailable(false);
        colorRepository.save(color);
    }

    // Validar os dados
    private void validateColor(ColorDTO dto) {

        if (dto == null) {
            throw new IllegalArgumentException(
                    "Os dados da cor são obrigatórios."
            );
        }

        if (dto.name() == null || dto.name().isBlank()) {
            throw new IllegalArgumentException(
                    "O nome da cor é obrigatório."
            );
        }

        if (dto.name().trim().length() > 100) {
            throw new IllegalArgumentException(
                    "O nome não pode ultrapassar 100 caracteres."
            );
        }

        if (dto.hexCode() == null
                || !dto.hexCode().trim().matches(
                "#[0-9a-fA-F]{6}")) {
            throw new IllegalArgumentException(
                    "O código hexadecimal deve ter o formato #RRGGBB."
            );
        }

        if (dto.materialId() == null) {
            throw new IllegalArgumentException(
                    "O material é obrigatório."
            );
        }
    }

    // Procurar material válido
    private Material findActiveMaterial(Integer materialId) {

        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new NoSuchElementException(
                        "Material não encontrado."
                ));

        if (!Boolean.TRUE.equals(material.getActive())) {
            throw new IllegalArgumentException(
                    "Não é possível associar uma cor a um material inativo."
            );
        }

        return material;
    }

    // Converter entidade para DTO
    private ColorDTO toDTO(Color color) {

        return new ColorDTO(
                color.getId(),
                color.getName(),
                color.getHexCode(),
                color.getAvailable(),
                color.getMaterial().getId()
        );
    }
}
