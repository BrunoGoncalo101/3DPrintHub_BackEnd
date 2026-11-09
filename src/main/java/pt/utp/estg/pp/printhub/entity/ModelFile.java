package pt.utp.estg.pp.printhub.entity;

import jakarta.persistence.*;
import pt.utp.estg.pp.printhub.model.FileFormat;
import pt.utp.estg.pp.printhub.model.FileFormatConverter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ficheiros_modelo")
public class ModelFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome_original", nullable = false, length = 255)
    private String originalName;

    @Column(name = "caminho", nullable = false, length = 500)
    private String filePath;

    @Column(name = "tamanho_bytes", nullable = false)
    private Integer sizeBytes;

    @Convert(converter = FileFormatConverter.class)
    @Column(name = "formato", nullable = false, length = 45)
    private FileFormat format;

    @Column(name = "volume_cm3", precision = 10, scale = 2)
    private BigDecimal volumeCm3;

    @Column(name ="dimensao_x", precision = 10, scale = 2)
    private BigDecimal dimensionX;

    @Column(name = "dimensao_y", precision = 10, scale = 2)
    private BigDecimal dimensionY;

    @Column(name = "dimensao_z", precision = 10, scale = 2)
    private BigDecimal dimensionZ;

    @Column(name = "utilizador_id", nullable = false)
    private Integer userId;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public Integer getSizeBytes() {
        return sizeBytes;
    }

    public void setSizeBytes(Integer sizeBytes) {
        this.sizeBytes = sizeBytes;
    }

    public FileFormat getFormat() {
        return format;
    }

    public void setFormat(FileFormat format) {
        this.format = format;
    }

    public BigDecimal getVolumeCm3() {
        return volumeCm3;
    }

    public void setVolumeCm3(BigDecimal volumeCm3) {
        this.volumeCm3 = volumeCm3;
    }

    public BigDecimal getDimensionX() {
        return dimensionX;
    }

    public void setDimensionX(BigDecimal dimensionX) {
        this.dimensionX = dimensionX;
    }

    public BigDecimal getDimensionY() {
        return dimensionY;
    }

    public void setDimensionY(BigDecimal dimensionY) {
        this.dimensionY = dimensionY;
    }

    public BigDecimal getDimensionZ() {
        return dimensionZ;
    }

    public void setDimensionZ(BigDecimal dimensionZ) {
        this.dimensionZ = dimensionZ;
    }

    public Integer getUserId() {
        return userId;
    }

    public void setUserId(Integer userId) {
        this.userId = userId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}