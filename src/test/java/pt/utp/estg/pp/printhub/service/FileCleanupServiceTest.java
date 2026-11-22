package pt.utp.estg.pp.printhub.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pt.utp.estg.pp.printhub.entity.CustomRequest;
import pt.utp.estg.pp.printhub.entity.ModelFile;
import pt.utp.estg.pp.printhub.model.CustomRequestStatus;
import pt.utp.estg.pp.printhub.repository.CustomRequestRepository;
import pt.utp.estg.pp.printhub.repository.ModelFileRepository;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FileCleanupServiceTest {

    @Mock
    private CustomRequestRepository customRequestRepository;

    @Mock
    private ModelFileRepository modelFileRepository;

    @InjectMocks
    private FileCleanupService fileCleanupService;

    @TempDir
    Path tempDir;

    @Test
    void cleanupExpiredFiles_ShouldDeletePhysicalFileAndMarkDeleted_WhenRequestFinishedOver30DaysAgo() throws IOException {
        Path stlFile = tempDir.resolve("peca_antiga.stl");
        Files.writeString(stlFile, "solid test endsolid test");
        assertTrue(Files.exists(stlFile));

        CustomRequest oldRequest = new CustomRequest();
        oldRequest.setId(1);
        oldRequest.setFileId(10);
        oldRequest.setStatus(CustomRequestStatus.APPROVED);
        oldRequest.setFinishedAt(LocalDateTime.now().minusDays(31));

        ModelFile modelFile = new ModelFile();
        modelFile.setId(10);
        modelFile.setFilePath(stlFile.toString());
        modelFile.setDeletedAt(null);

        when(customRequestRepository.findFinishedBefore(any(), any())).thenReturn(List.of(oldRequest));
        when(customRequestRepository.existsByFileIdAndStatus(10, CustomRequestStatus.PENDING)).thenReturn(false);
        when(modelFileRepository.findById(10)).thenReturn(Optional.of(modelFile));
        when(modelFileRepository.findOrphanFilesCreatedBefore(any())).thenReturn(List.of());

        int deleted = fileCleanupService.cleanupExpiredFiles();

        assertEquals(1, deleted);
        assertFalse(Files.exists(stlFile), "O ficheiro físico deve ser removido do disco");
        assertNotNull(modelFile.getDeletedAt(), "O registo na BD deve ter deletedAt preenchido");
        verify(modelFileRepository, times(1)).save(modelFile);
    }

    @Test
    void cleanupExpiredFiles_ShouldSkipDeletion_WhenAnotherPendingRequestUsesSameFile() throws IOException {
        Path stlFile = tempDir.resolve("peca_partilhada.stl");
        Files.writeString(stlFile, "solid shared endsolid shared");

        CustomRequest oldRequest = new CustomRequest();
        oldRequest.setId(2);
        oldRequest.setFileId(20);
        oldRequest.setStatus(CustomRequestStatus.REJECTED);
        oldRequest.setFinishedAt(LocalDateTime.now().minusDays(40));

        when(customRequestRepository.findFinishedBefore(any(), any())).thenReturn(List.of(oldRequest));
        when(customRequestRepository.existsByFileIdAndStatus(eq(20), eq(CustomRequestStatus.PENDING))).thenReturn(true);
        when(modelFileRepository.findOrphanFilesCreatedBefore(any())).thenReturn(List.of());

        int deleted = fileCleanupService.cleanupExpiredFiles();

        assertEquals(0, deleted);
        assertTrue(Files.exists(stlFile), "O ficheiro físico não deve ser apagado se houver pedido pendente");
        verify(modelFileRepository, never()).save(any());
    }

    @Test
    void cleanupExpiredFiles_ShouldDeleteOrphanFilesOlderThan30Days() throws IOException {
        Path orphanStl = tempDir.resolve("orfao.stl");
        Files.writeString(orphanStl, "solid orphan endsolid orphan");

        ModelFile orphanModel = new ModelFile();
        orphanModel.setId(30);
        orphanModel.setFilePath(orphanStl.toString());
        orphanModel.setCreatedAt(LocalDateTime.now().minusDays(35));

        when(customRequestRepository.findFinishedBefore(any(), any())).thenReturn(List.of());
        when(modelFileRepository.findOrphanFilesCreatedBefore(any())).thenReturn(List.of(orphanModel));

        int deleted = fileCleanupService.cleanupExpiredFiles();

        assertEquals(1, deleted);
        assertFalse(Files.exists(orphanStl));
        assertNotNull(orphanModel.getDeletedAt());
        verify(modelFileRepository, times(1)).save(orphanModel);
    }
}