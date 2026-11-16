
package pt.utp.estg.pp.printhub.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import pt.utp.estg.pp.printhub.dto.QuoteResponse;
import pt.utp.estg.pp.printhub.entity.Material;
import pt.utp.estg.pp.printhub.entity.ModelFile;
import pt.utp.estg.pp.printhub.exception.InvalidFileException;
import pt.utp.estg.pp.printhub.repository.MaterialRepository;
import pt.utp.estg.pp.printhub.repository.ModelFileRepository;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ModelFileServiceTest {

    @Mock
    private ModelFileRepository repository;

    @Mock
    private MaterialRepository materialRepository;

    @Mock
    private StockService stockService;

    private ModelFileService service;

    @BeforeEach
    void setUp() {
        service = new ModelFileService(
                repository,
                "uploads",
                materialRepository,
                stockService
        );
    }

    @Test
    @DisplayName("Deve calcular o orçamento corretamente para um ficheiro com volume válido")
    void calculateQuote_Success() throws InvalidFileException {

        ModelFile file = new ModelFile();
        file.setId(1);
        file.setOriginalName("peca_teste.stl");
        file.setVolumeCm3(BigDecimal.valueOf(10.0));

        Material material = new Material();
        material.setId(1);
        material.setName("PLA");
        material.setActive(true);
        material.setDensity(new BigDecimal("1.24"));
        material.setPricePerGram(new BigDecimal("0.05"));

        when(repository.findById(1))
                .thenReturn(Optional.of(file));

        when(materialRepository.findById(1))
                .thenReturn(Optional.of(material));

        QuoteResponse quote = service.calculateQuote(1, 1, 20);

        assertNotNull(quote);
        assertEquals(1, quote.fileId());
        assertEquals(new BigDecimal("2.48"), quote.estimatedWeightGrams());
        assertEquals(new BigDecimal("0.12"), quote.totalPrice());
    }

    @Test
    @DisplayName("Deve lançar exceção ao calcular orçamento de ficheiro sem volume")
    void calculateQuote_NullVolume_ThrowsException() {

        ModelFile file = new ModelFile();
        file.setId(2);
        file.setOriginalName("modelo.obj");
        file.setVolumeCm3(null);

        when(repository.findById(2))
                .thenReturn(Optional.of(file));

        assertThrows(
                InvalidFileException.class,
                () -> service.calculateQuote(2, 1, 20)
        );
    }
}
