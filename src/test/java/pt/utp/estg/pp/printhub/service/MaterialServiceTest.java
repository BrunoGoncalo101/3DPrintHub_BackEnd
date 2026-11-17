
package pt.utp.estg.pp.printhub.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pt.utp.estg.pp.printhub.dto.MaterialDTO;
import pt.utp.estg.pp.printhub.entity.Material;
import pt.utp.estg.pp.printhub.repository.MaterialRepository;

import java.math.BigDecimal;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    @Mock
    private MaterialRepository materialRepository;

    private MaterialService materialService;

    @BeforeEach
    void setUp() {
        materialService = new MaterialService(materialRepository);
    }

    private MaterialDTO criarDTO(String nome, String preco, String densidade) {
        return new MaterialDTO(
                null,
                nome,
                new BigDecimal(preco),
                new BigDecimal(densidade),
                true
        );
    }

    @Test
    void deveCriarMaterial() {
        MaterialDTO dto = criarDTO(" PLA ", "0.0250", "1.240");

        when(materialRepository.save(any(Material.class)))
                .thenAnswer(invocation -> {
                    Material material = invocation.getArgument(0);
                    material.setId(1);
                    return material;
                });

        MaterialDTO resultado = materialService.createMaterial(dto);

        assertEquals(1, resultado.id());
        assertEquals("PLA", resultado.name());
        assertEquals(new BigDecimal("0.0250"), resultado.pricePerGram());
        assertTrue(resultado.active());

        verify(materialRepository).save(any(Material.class));
    }

    @Test
    void naoDeveCriarMaterialSemNome() {
        MaterialDTO dto = criarDTO(" ", "0.0250", "1.240");

        assertThrows(
                IllegalArgumentException.class,
                () -> materialService.createMaterial(dto)
        );

        verify(materialRepository, never()).save(any());
    }

    @Test
    void naoDeveCriarMaterialDuplicado() {
        MaterialDTO dto = criarDTO("PLA", "0.0250", "1.240");

        when(materialRepository.existsByNameIgnoreCase("PLA"))
                .thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> materialService.createMaterial(dto)
        );

        verify(materialRepository, never()).save(any());
    }

    @Test
    void naoDeveCriarMaterialComPrecoInvalido() {
        MaterialDTO dto = criarDTO("PLA", "0", "1.240");

        assertThrows(
                IllegalArgumentException.class,
                () -> materialService.createMaterial(dto)
        );

        verify(materialRepository, never()).save(any());
    }

    @Test
    void naoDeveCriarMaterialComDensidadeInvalida() {
        MaterialDTO dto = criarDTO("PLA", "0.0250", "-1");

        assertThrows(
                IllegalArgumentException.class,
                () -> materialService.createMaterial(dto)
        );

        verify(materialRepository, never()).save(any());
    }

    @Test
    void naoDeveCriarMaterialComPrecoForaDaPrecisao() {
        MaterialDTO dto = criarDTO("PLA", "0.02501", "1.240");

        assertThrows(
                IllegalArgumentException.class,
                () -> materialService.createMaterial(dto)
        );
    }

    @Test
    void deveAtualizarMaterial() {
        Material material = new Material();
        material.setId(1);
        material.setName("PLA");
        material.setPricePerGram(new BigDecimal("0.0250"));
        material.setDensity(new BigDecimal("1.240"));
        material.setActive(true);

        when(materialRepository.findById(1))
                .thenReturn(Optional.of(material));

        when(materialRepository.save(any(Material.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        MaterialDTO dto = criarDTO("PETG", "0.0300", "1.270");

        MaterialDTO resultado = materialService.updateMaterial(1, dto);

        assertEquals("PETG", resultado.name());
        assertEquals(new BigDecimal("0.0300"), resultado.pricePerGram());
        assertEquals(new BigDecimal("1.270"), resultado.density());

        verify(materialRepository).save(material);
    }

    @Test
    void naoDeveAtualizarMaterialInexistente() {
        when(materialRepository.findById(99))
                .thenReturn(Optional.empty());

        MaterialDTO dto = criarDTO("PLA", "0.0250", "1.240");

        assertThrows(
                NoSuchElementException.class,
                () -> materialService.updateMaterial(99, dto)
        );
    }

    @Test
    void deveDesativarMaterial() {
        Material material = new Material();
        material.setId(1);
        material.setName("PLA");
        material.setActive(true);

        when(materialRepository.findById(1))
                .thenReturn(Optional.of(material));

        materialService.deleteMaterial(1);

        assertFalse(material.getActive());

        verify(materialRepository).save(material);
        verify(materialRepository, never()).delete(any(Material.class));
    }

    @Test
    void naoDeveDesativarMaterialInexistente() {
        when(materialRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> materialService.deleteMaterial(99)
        );
    }
}
