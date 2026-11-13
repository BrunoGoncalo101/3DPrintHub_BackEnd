package pt.utp.estg.pp.printhub.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.utp.estg.pp.printhub.dto.ProductionQueueItemDTO;
import pt.utp.estg.pp.printhub.entity.CustomRequest;
import pt.utp.estg.pp.printhub.entity.ModelFile;
import pt.utp.estg.pp.printhub.model.CustomRequestStatus;
import pt.utp.estg.pp.printhub.repository.CustomRequestRepository;
import pt.utp.estg.pp.printhub.repository.ModelFileRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomRequestServiceTest {

    @Mock
    private CustomRequestRepository customRequestRepository;

    @Mock
    private ModelFileService modelFileService;

    @Mock
    private ModelFileRepository modelFileRepository;

    @InjectMocks
    private CustomRequestService service;

    @Test
    @DisplayName("Deve devolver a fila de produção ordenada apenas com pedidos APPROVED")
    void getProductionQueue_Success() {
        CustomRequest req1 = new CustomRequest();
        req1.setId(10);
        req1.setFileId(1);
        req1.setMaterialId(1);
        req1.setColorId(2);
        req1.setQuantity(2);
        req1.setInfillPercent(20);
        req1.setFinalPrice(new BigDecimal("15.50"));
        req1.setStatus(CustomRequestStatus.APPROVED);
        req1.setCreatedAt(LocalDateTime.now().minusHours(2));

        ModelFile file1 = new ModelFile();
        file1.setId(1);
        file1.setOriginalName("suporte_motor.stl");

        when(customRequestRepository.findByStatusOrderByCreatedAtAsc(CustomRequestStatus.APPROVED))
                .thenReturn(List.of(req1));
        when(modelFileRepository.findById(1)).thenReturn(Optional.of(file1));

        List<ProductionQueueItemDTO> queue = service.getProductionQueue();

        assertNotNull(queue);
        assertEquals(1, queue.size());
        assertEquals(10, queue.get(0).requestId());
        assertEquals("suporte_motor.stl", queue.get(0).fileName());
    }
}