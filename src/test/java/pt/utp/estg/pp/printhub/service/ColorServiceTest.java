
package pt.utp.estg.pp.printhub.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pt.utp.estg.pp.printhub.dto.ColorDTO;
import pt.utp.estg.pp.printhub.entity.Color;
import pt.utp.estg.pp.printhub.entity.Material;
import pt.utp.estg.pp.printhub.repository.ColorRepository;
import pt.utp.estg.pp.printhub.repository.MaterialRepository;

import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ColorServiceTest {

    @Mock
    private ColorRepository colorRepository;

    @Mock
    private MaterialRepository materialRepository;

    private ColorService colorService;

    @BeforeEach
    void setUp() {
        colorService = new ColorService(
                colorRepository,
                materialRepository
        );
    }

    private ColorDTO criarDTO(String nome, String hexadecimal, Integer materialId) {
        return new ColorDTO(
                null,
                nome,
                hexadecimal,
                true,
                materialId
        );
    }

    private Material criarMaterialAtivo() {
        Material material = new Material();
        material.setId(1);
        material.setName("PLA");
        material.setActive(true);
        return material;
    }

    @Test
    void deveCriarCor() {
        Material material = criarMaterialAtivo();

        when(materialRepository.findById(1))
                .thenReturn(Optional.of(material));

        when(colorRepository.save(any(Color.class)))
                .thenAnswer(invocation -> {
                    Color color = invocation.getArgument(0);
                    color.setId(1);
                    return color;
                });

        ColorDTO resultado = colorService.createColor(
                criarDTO(" Vermelho ", "#ff0000", 1)
        );

        assertEquals(1, resultado.id());
        assertEquals("Vermelho", resultado.name());
        assertEquals("#FF0000", resultado.hexCode());
        assertTrue(resultado.available());
        assertEquals(1, resultado.materialId());

        verify(colorRepository).save(any(Color.class));
    }

    @Test
    void naoDeveCriarCorSemNome() {
        assertThrows(
                IllegalArgumentException.class,
                () -> colorService.createColor(
                        criarDTO(" ", "#FF0000", 1)
                )
        );

        verify(colorRepository, never()).save(any());
    }

    @Test
    void naoDeveCriarCorComHexadecimalInvalido() {
        assertThrows(
                IllegalArgumentException.class,
                () -> colorService.createColor(
                        criarDTO("Vermelho", "vermelho", 1)
                )
        );

        verify(colorRepository, never()).save(any());
    }

    @Test
    void naoDeveCriarCorSemMaterial() {
        assertThrows(
                IllegalArgumentException.class,
                () -> colorService.createColor(
                        criarDTO("Vermelho", "#FF0000", null)
                )
        );

        verify(colorRepository, never()).save(any());
    }

    @Test
    void naoDeveCriarCorComMaterialInexistente() {
        when(materialRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> colorService.createColor(
                        criarDTO("Vermelho", "#FF0000", 99)
                )
        );
    }

    @Test
    void naoDeveCriarCorComMaterialInativo() {
        Material material = criarMaterialAtivo();
        material.setActive(false);

        when(materialRepository.findById(1))
                .thenReturn(Optional.of(material));

        assertThrows(
                IllegalArgumentException.class,
                () -> colorService.createColor(
                        criarDTO("Vermelho", "#FF0000", 1)
                )
        );
    }

    @Test
    void naoDeveCriarCorDuplicadaNoMesmoMaterial() {
        Material material = criarMaterialAtivo();

        when(materialRepository.findById(1))
                .thenReturn(Optional.of(material));

        when(colorRepository.existsByNameIgnoreCaseAndMaterialId(
                "Vermelho", 1
        )).thenReturn(true);

        assertThrows(
                IllegalArgumentException.class,
                () -> colorService.createColor(
                        criarDTO("Vermelho", "#FF0000", 1)
                )
        );

        verify(colorRepository, never()).save(any());
    }

    @Test
    void deveAtualizarCor() {
        Material material = criarMaterialAtivo();

        Color color = new Color();
        color.setId(1);
        color.setName("Vermelho");
        color.setHexCode("#FF0000");
        color.setAvailable(true);
        color.setMaterial(material);

        when(colorRepository.findById(1))
                .thenReturn(Optional.of(color));

        when(materialRepository.findById(1))
                .thenReturn(Optional.of(material));

        when(colorRepository.save(any(Color.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        ColorDTO resultado = colorService.updateColor(
                1,
                criarDTO("Azul", "#0000ff", 1)
        );

        assertEquals("Azul", resultado.name());
        assertEquals("#0000FF", resultado.hexCode());
        assertTrue(resultado.available());

        verify(colorRepository).save(color);
    }

    @Test
    void naoDeveAtualizarCorInexistente() {
        when(colorRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> colorService.updateColor(
                        99,
                        criarDTO("Azul", "#0000FF", 1)
                )
        );
    }

    @Test
    void deveDesativarCor() {
        Color color = new Color();
        color.setId(1);
        color.setAvailable(true);

        when(colorRepository.findById(1))
                .thenReturn(Optional.of(color));

        colorService.deleteColor(1);

        assertFalse(color.getAvailable());

        verify(colorRepository).save(color);
        verify(colorRepository, never()).delete(any(Color.class));
    }

    @Test
    void naoDeveDesativarCorInexistente() {
        when(colorRepository.findById(99))
                .thenReturn(Optional.empty());

        assertThrows(
                NoSuchElementException.class,
                () -> colorService.deleteColor(99)
        );
    }
}
