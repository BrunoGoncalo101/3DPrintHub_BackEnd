package pt.utp.estg.pp.printhub.service;

import org.springframework.stereotype.Service;
import pt.utp.estg.pp.printhub.dto.*;
import pt.utp.estg.pp.printhub.entity.CustomRequest;
import pt.utp.estg.pp.printhub.entity.ModelFile;
import pt.utp.estg.pp.printhub.exception.InvalidFileException;
import pt.utp.estg.pp.printhub.model.CustomRequestStatus;
import pt.utp.estg.pp.printhub.repository.CustomRequestRepository;
import pt.utp.estg.pp.printhub.repository.ModelFileRepository;

import java.math.BigDecimal;
import java.util.List;

@Service
public class CustomRequestService {

    private final CustomRequestRepository customRequestRepository;
    private final ModelFileService modelFileService;
    private final ModelFileRepository modelFileRepository;

    public CustomRequestService(CustomRequestRepository customRequestRepository, ModelFileService modelFileService, ModelFileRepository modelFileRepository) {
        this.customRequestRepository = customRequestRepository;
        this.modelFileService = modelFileService;
        this.modelFileRepository = modelFileRepository;
    }

    public CustomRequestResponse createRequest(CreateCustomRequestDTO dto) throws InvalidFileException {
        QuoteResponse quote = modelFileService.calculateQuote(dto.fileId(), dto.materialId(), dto.infillPercent());

        BigDecimal totalEstimatedPrice = quote.totalPrice().multiply(BigDecimal.valueOf(dto.quantity()));

        CustomRequest request = new CustomRequest();
        request.setFileId(dto.fileId());
        request.setMaterialId(dto.materialId());
        request.setColorId(dto.colorId());
        request.setQuantity(dto.quantity());
        request.setInfillPercent(dto.infillPercent());
        request.setEstimatedPrice(totalEstimatedPrice);
        request.setUserId(dto.userId());
        request.setStatus(CustomRequestStatus.PENDING);

        CustomRequest saved = customRequestRepository.save(request);
        return CustomRequestResponse.from(saved);
    }

    public List<ProductionQueueItemDTO> getProductionQueue() {
        return customRequestRepository.findByStatusOrderByCreatedAtAsc(CustomRequestStatus.APPROVED)
                .stream()
                .map(req -> {
                    String fileName = modelFileRepository.findById(req.getFileId())
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

    public CustomRequestResponse approveRequest(Integer requestId, ApproveCustomRequestDTO dto) throws InvalidFileException {
        CustomRequest request = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new InvalidFileException("Pedido personalizado não encontrado com ID: " + requestId));

        request.setStatus(CustomRequestStatus.APPROVED);
        request.setFinalPrice(dto.finalPrice());

        CustomRequest saved = customRequestRepository.save(request);
        return CustomRequestResponse.from(saved);
    }

    public CustomRequestResponse rejectRequest(Integer requestId, RejectCustomRequestDTO dto) throws InvalidFileException {
        CustomRequest request = customRequestRepository.findById(requestId)
                .orElseThrow(() -> new InvalidFileException("Pedido personalizado não encontrado com ID: " + requestId));

        request.setStatus(CustomRequestStatus.REJECTED);
        request.setRejectionReason(dto.reason() != null ? dto.reason() : "Sem motivo especificado.");

        CustomRequest saved = customRequestRepository.save(request);
        return CustomRequestResponse.from(saved);
    }

    public List<CustomRequestResponse> getAllRequests() {
        return customRequestRepository.findAll().stream()
                .map(CustomRequestResponse::from)
                .toList();
    }
}