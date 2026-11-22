package pt.utp.estg.pp.printhub.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import pt.utp.estg.pp.printhub.entity.ModelFile;

import java.time.LocalDateTime;
import java.util.List;

public interface ModelFileRepository extends JpaRepository<ModelFile, Integer> {

    List<ModelFile> findByUserId(Integer userId);

    @Query("""
            SELECT f FROM ModelFile f
            WHERE f.deletedAt IS NULL
              AND f.createdAt < :cutoffDate
              AND NOT EXISTS (
                  SELECT 1 FROM CustomRequest p WHERE p.fileId = f.id
              )
            """)
    List<ModelFile> findOrphanFilesCreatedBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}