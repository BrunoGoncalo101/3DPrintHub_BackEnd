package pt.utp.estg.pp.printhub.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import pt.utp.estg.pp.printhub.entity.CustomRequest;
import pt.utp.estg.pp.printhub.entity.ModelFile;
import pt.utp.estg.pp.printhub.model.CustomRequestStatus;
import pt.utp.estg.pp.printhub.repository.CustomRequestRepository;
import pt.utp.estg.pp.printhub.repository.ModelFileRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class FileCleanupService {

    private static final Logger log = LoggerFactory.getLogger(FileCleanupService.class);
    private static final int RETENTION_DAYS = 30;
    private static final List<CustomRequestStatus> TERMINAL_STATUSES = List.of(
            CustomRequestStatus.APPROVED,
            CustomRequestStatus.REJECTED
    );

    private final CustomRequestRepository customRequestRepository;
    private final ModelFileRepository modelFileRepository;

    public FileCleanupService(CustomRequestRepository customRequestRepository,
                              ModelFileRepository modelFileRepository) {
        this.customRequestRepository = customRequestRepository;
        this.modelFileRepository = modelFileRepository;
    }

    /**
     * Executa diariamente às 03:00 da manhã para remover ficheiros 3D
     * associados a pedidos terminados há mais de 30 dias (e ficheiros órfãos antigos).
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public int cleanupExpiredFiles() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(RETENTION_DAYS);
        log.info("A iniciar limpeza automática de ficheiros 3D anteriores a {}", cutoffDate);

        Set<Integer> candidateFileIds = new HashSet<>();

        // 1. Ficheiros de pedidos terminados (APPROVED ou REJECTED) há mais de 30 dias
        List<CustomRequest> expiredRequests = customRequestRepository.findFinishedBefore(
                TERMINAL_STATUSES, cutoffDate
        );
        for (CustomRequest req : expiredRequests) {
            candidateFileIds.add(req.getFileId());
        }

        int deletedCount = 0;

        for (Integer fileId : candidateFileIds) {
            // Segurança: nunca apagar se existir outro pedido ainda pendente a usar o mesmo ficheiro
            if (customRequestRepository.existsByFileIdAndStatus(fileId, CustomRequestStatus.PENDING)) {
                continue;
            }

            ModelFile modelFile = modelFileRepository.findById(fileId).orElse(null);
            if (modelFile != null && modelFile.getDeletedAt() == null) {
                if (deletePhysicalFileAndMarkDeleted(modelFile)) {
                    deletedCount++;
                }
            }
        }

        // 2. Ficheiros órfãos (sem nenhum pedido associado) enviados há mais de 30 dias
        List<ModelFile> orphanFiles = modelFileRepository.findOrphanFilesCreatedBefore(cutoffDate);
        for (ModelFile orphan : orphanFiles) {
            if (deletePhysicalFileAndMarkDeleted(orphan)) {
                deletedCount++;
            }
        }

        log.info("Limpeza automática concluída. Total de ficheiros eliminados: {}", deletedCount);
        return deletedCount;
    }

    private boolean deletePhysicalFileAndMarkDeleted(ModelFile modelFile) {
        try {
            if (modelFile.getFilePath() != null && !modelFile.getFilePath().isBlank()) {
                Path path = Paths.get(modelFile.getFilePath());
                boolean removed = Files.deleteIfExists(path);
                if (removed) {
                    log.info("Ficheiro físico removido do disco: {}", path);
                } else {
                    log.warn("Ficheiro físico já não existia no disco (a marcar como eliminado na BD): {}", path);
                }
            }

            modelFile.setDeletedAt(LocalDateTime.now());
            modelFileRepository.save(modelFile);
            return true;
        } catch (IOException ex) {
            log.error("Erro de I/O ao eliminar ficheiro físico [id={}, path={}]: {}",
                    modelFile.getId(), modelFile.getFilePath(), ex.getMessage());
            return false;
        }
    }
}