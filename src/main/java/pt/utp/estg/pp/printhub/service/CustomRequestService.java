
package pt.utp.estg.pp.printhub.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import pt.utp.estg.pp.printhub.dto.*;
import pt.utp.estg.pp.printhub.entity.CustomRequest;
import pt.utp.estg.pp.printhub.entity.ModelFile;
import pt.utp.estg.pp.printhub.exception.InvalidFileException;
import pt.utp.estg.pp.printhub.model.CustomRequestStatus;
import pt.utp.estg.pp.printhub.repository.CustomRequestRepository;
import pt.utp.estg.pp.printhub.repository.ModelFileRepository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class CustomRequestService {

    private final CustomRequestRepository customRequestRepository;
    private final ModelFileService modelFileService;
    private final ModelFileRepository modelFileRepository;
    private final StockService stockService;
    private final NotificationService notificationService;

    public CustomRequestService(
            CustomRequestRepository customRequestRepository,
            ModelFileService modelFileService,
            ModelFileRepository modelFileRepository,
            StockService stockService,
            NotificationService notificationService) {

        this.customRequestRepository = customRequestRepository;
        this.modelFileService = modelFileService;
        this.modelFileRepository = modelFileRepository;
        this.stockService = stockService;
        this.notificationService = notificationService;
    }

    @Transactional
    public CustomRequestResponse createRequest(CreateCustomRequestDTO dto)
            throws InvalidFileException {

        // 1. Validar a quantidade de peças
        if (dto.quantity() == null || dto.quantity() < 1) {
            throw new IllegalArgumentException(
                    "A quantidade deve ser superior a zero."
            );
        }

        // 2. Calcular o orçamento de uma peça
        QuoteResponse quote = modelFileService.calculateQuote(
                dto.fileId(),
                dto.materialId(),
                dto.infillPercent()
        );

        // 3. Calcular o peso total necessário
        BigDecimal requiredGrams = quote.estimatedWeightGrams()
                .multiply(BigDecimal.valueOf(dto.quantity()))
                .setScale(2, RoundingMode.HALF_UP);

        // 4. Validar e descontar o stock da cor selecionada
        stockService.deductStock(
                dto.materialId(),
                dto.colorId(),
                requiredGrams
        );

        // 5. Calcular o preço total estimado
        BigDecimal totalEstimatedPrice = quote.totalPrice()
                .multiply(BigDecimal.valueOf(dto.quantity()));

        // 6. Criar o pedido personalizado
        CustomRequest request = new CustomRequest();

        request.setFileId(dto.fileId());
        request.setMaterialId(dto.materialId());
        request.setColorId(dto.colorId());
        request.setQuantity(dto.quantity());
        request.setInfillPercent(dto.infillPercent());
        request.setEstimatedPrice(totalEstimatedPrice);
        request.setReservedStockGrams(requiredGrams);
        request.setUserId(dto.userId());
        request.setStatus(CustomRequestStatus.PENDING);

        // 7. Guardar o pedido
        CustomRequest saved = customRequestRepository.save(request);

        return CustomRequestResponse.from(saved);
    }

    public List<ProductionQueueItemDTO> getProductionQueue() {

        return customRequestRepository
                .findByStatusOrderByCreatedAtAsc(CustomRequestStatus.APPROVED)
                .stream()
                .map(req -> {

                    String fileName = modelFileRepository
                            .findById(req.getFileId())
                            .map(ModelFile::getOriginalName)
                            .orElse("ficheiro_desconhecido");

                    return new ProductionQueueItemDTO(
                            req.getId(),
                            req.getFileId(),
                            fileName,
                            req.getMaterialId(),
                            req.getColorId(),
                            req.getQuantity(),
                            req.getInfillPercent(),
                            req.getFinalPrice(),
                            req.getCreatedAt()
                    );
                })
                .toList();
    }

    public CustomRequestResponse approveRequest(
            Integer requestId,
            ApproveCustomRequestDTO dto) throws InvalidFileException {

        CustomRequest request = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new InvalidFileException(
                        "Pedido personalizado não encontrado com ID: "
                                + requestId
                ));

        request.setStatus(CustomRequestStatus.APPROVED);
        request.setFinalPrice(dto.finalPrice());

        request.setFinishedAt(LocalDateTime.now());

        CustomRequest saved = customRequestRepository.save(request);

        notificationService.notifyCustomRequestStatusChange(
                saved.getUserId(),
                saved.getId(),
                CustomRequestStatus.APPROVED,
                "Preço final: " + saved.getFinalPrice() + " €."
        );

        return CustomRequestResponse.from(saved);
    }


    @Transactional
    public CustomRequestResponse rejectRequest(
            Integer requestId,
            RejectCustomRequestDTO dto) throws InvalidFileException {

        CustomRequest request = customRequestRepository.findByIdForUpdate(requestId)
                .orElseThrow(() -> new InvalidFileException(
                        "Pedido personalizado não encontrado com ID: " + requestId
                ));

        // Impedir rejeições repetidas e alterações a pedidos aprovados
        if (request.getStatus() != CustomRequestStatus.PENDING) {
            throw new IllegalStateException(
                    "Apenas pedidos pendentes podem ser rejeitados."
            );
        }

        // Devolver o stock reservado, quando existir
        BigDecimal reservedGrams = request.getReservedStockGrams();

        if (reservedGrams != null &&
                reservedGrams.compareTo(BigDecimal.ZERO) > 0) {

            stockService.restoreStock(
                    request.getMaterialId(),
                    request.getColorId(),
                    reservedGrams
            );
        }

        // Atualizar o estado do pedido
        request.setStatus(CustomRequestStatus.REJECTED);

        request.setRejectionReason(
                dto.reason() != null
                        ? dto.reason()
                        : "Sem motivo especificado."
        );

        request.setFinishedAt(LocalDateTime.now());

        CustomRequest saved = customRequestRepository.save(request);

        notificationService.notifyCustomRequestStatusChange(
                saved.getUserId(),
                saved.getId(),
                CustomRequestStatus.REJECTED,
                saved.getRejectionReason()
        );

        return CustomRequestResponse.from(saved);
    }


    public List<CustomRequestResponse> getAllRequests() {

        return customRequestRepository.findAll()
                .stream()
                .map(CustomRequestResponse::from)
                .toList();
    }
}
