
package pt.utp.estg.pp.printhub.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import pt.utp.estg.pp.printhub.dto.QuoteResponse;
import pt.utp.estg.pp.printhub.entity.Material;
import pt.utp.estg.pp.printhub.entity.ModelFile;
import pt.utp.estg.pp.printhub.exception.FileStorageException;
import pt.utp.estg.pp.printhub.exception.InvalidFileException;
import pt.utp.estg.pp.printhub.model.FileFormat;
import pt.utp.estg.pp.printhub.repository.MaterialRepository;
import pt.utp.estg.pp.printhub.repository.ModelFileRepository;
import pt.utp.estg.pp.printhub.util.StlParser;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

@Service
public class ModelFileService {

    private static final long MAX_FILE_SIZE_BYTES = 50L * 1024 * 1024;
    private static final int MAX_FILE_NAME_LENGTH = 255;

    private final ModelFileRepository repository;
    private final Path uploadPath;
    private final MaterialRepository materialRepository;
    private final StockService stockService;

    public ModelFileService(
            ModelFileRepository repository,
            @Value("${app.uploads.dir}") String uploadPath,
            MaterialRepository materialRepository,
            StockService stockService) {

        this.repository = repository;
        this.uploadPath = Path.of(uploadPath).toAbsolutePath().normalize();
        this.materialRepository = materialRepository;
        this.stockService = stockService;
    }

    /**
     * Valida o ficheiro, guarda-o em uploads/
     * e regista-o na base de dados.
     */
    public ModelFile save(MultipartFile file, Integer userId)
            throws InvalidFileException {

        if (file == null || file.isEmpty()) {
            throw new InvalidFileException(
                    "O ficheiro está vazio ou não foi enviado."
            );
        }

        if (userId == null) {
            throw new InvalidFileException(
                    "O ID do utilizador é obrigatório."
            );
        }

        String originalName = sanitizeFileName(file.getOriginalFilename());

        FileFormat format = FileFormat.fromFileName(originalName);

        if (format == null) {
            throw new InvalidFileException(
                    "Formato não suportado. Por favor use .stl, .3mf ou .obj."
            );
        }

        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new InvalidFileException(
                    "O ficheiro excede o limite máximo de 50 MB."
            );
        }

        String savedFileName = UUID.randomUUID()
                + "."
                + format.getDbValue().toLowerCase();

        Path targetPath = uploadPath.resolve(savedFileName).normalize();

        if (!targetPath.startsWith(uploadPath)) {
            throw new InvalidFileException(
                    "Caminho de ficheiro inválido."
            );
        }

        saveToDisk(file, targetPath);

        ModelFile modelFile = new ModelFile();

        modelFile.setOriginalName(originalName);
        modelFile.setFilePath(savedFileName);
        modelFile.setSizeBytes((int) file.getSize());
        modelFile.setFormat(format);
        modelFile.setUserId(userId);

        if (format == FileFormat.STL) {

            try (InputStream inputStream = file.getInputStream()) {

                StlParser.StlAnalysisResult analysis =
                        StlParser.parseBinaryStl(inputStream);

                modelFile.setVolumeCm3(
                        BigDecimal.valueOf(analysis.volumeCm3())
                );

                modelFile.setDimensionX(
                        BigDecimal.valueOf(analysis.widthX())
                );

                modelFile.setDimensionY(
                        BigDecimal.valueOf(analysis.depthY())
                );

                modelFile.setDimensionZ(
                        BigDecimal.valueOf(analysis.heightZ())
                );

            } catch (Exception e) {

                deleteQuietly(targetPath);

                throw new InvalidFileException(
                        "Falha ao analisar a geometria do ficheiro STL: "
                                + e.getMessage()
                );
            }
        }

        try {
            return repository.save(modelFile);

        } catch (RuntimeException e) {

            deleteQuietly(targetPath);
            throw e;
        }
    }

    /**
     * Calcula o orçamento estimado com base
     * no volume do ficheiro e no material escolhido.
     */
    public QuoteResponse calculateQuote(
            Integer fileId,
            Integer materialId,
            int infillPercent) throws InvalidFileException {

        ModelFile modelFile = repository.findById(fileId)
                .orElseThrow(() -> new InvalidFileException(
                        "Ficheiro não encontrado com o id: " + fileId
                ));

        if (modelFile.getVolumeCm3() == null) {
            throw new InvalidFileException(
                    "O orçamento automático por volume está atualmente "
                            + "disponível apenas para ficheiros .STL. "
                            + "Ficheiros .OBJ e .3MF requerem análise manual."
            );
        }

        Material material = materialRepository.findById(materialId)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Material não encontrado."
                ));

        if (!Boolean.TRUE.equals(material.getActive())) {
            throw new IllegalArgumentException(
                    "Material indisponível."
            );
        }

        if (infillPercent < 10 || infillPercent > 100) {
            throw new IllegalArgumentException(
                    "A percentagem de preenchimento deve estar entre 10 e 100."
            );
        }

        double densityGcm3 = material.getDensity().doubleValue();
        double pricePerGram = material.getPricePerGram().doubleValue();
        String materialName = material.getName();

        double volume = modelFile.getVolumeCm3().doubleValue();
        double infillFactor = infillPercent / 100.0;

        // Peso (g) = Volume (cm3) * Densidade (g/cm3) * Infill
        double weightGrams = volume * densityGcm3 * infillFactor;

        // Preço (€) = Peso (g) * Preço por grama (€/g)
        double price = weightGrams * pricePerGram;

        BigDecimal estimatedWeight = BigDecimal.valueOf(weightGrams)
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal totalPrice = BigDecimal.valueOf(price)
                .setScale(2, RoundingMode.HALF_UP);

        return new QuoteResponse(
                modelFile.getId(),
                modelFile.getOriginalName(),
                modelFile.getVolumeCm3(),
                materialId,
                materialName,
                infillPercent,
                estimatedWeight,
                totalPrice
        );
    }

    /**
     * Valida se existe stock suficiente para
     * produzir a quantidade de peças pretendida.
     *
     * Esta operação não desconta stock.
     */
    public void validateQuoteStock(
            Integer materialId,
            Integer colorId,
            BigDecimal estimatedWeightGrams,
            Integer quantity) {

        if (quantity == null || quantity < 1) {
            throw new IllegalArgumentException(
                    "A quantidade deve ser superior a zero."
            );
        }

        if (estimatedWeightGrams == null ||
                estimatedWeightGrams.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "O peso estimado deve ser superior a zero."
            );
        }

        BigDecimal requiredGrams = estimatedWeightGrams
                .multiply(BigDecimal.valueOf(quantity));

        stockService.validateStock(
                materialId,
                colorId,
                requiredGrams
        );
    }

    /**
     * Guarda o ficheiro na pasta uploads/.
     */
    private void saveToDisk(MultipartFile file, Path targetPath) {

        try {
            Files.createDirectories(uploadPath);

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath);
            }

        } catch (IOException e) {
            throw new FileStorageException(
                    "Não foi possível guardar o ficheiro no disco.",
                    e
            );
        }
    }

    /**
     * Apaga o ficheiro caso ocorra um erro.
     */
    private void deleteQuietly(Path path) {

        try {
            Files.deleteIfExists(path);

        } catch (IOException ignored) {
            // Limpeza best-effort
        }
    }

    /**
     * Extrai apenas o nome do ficheiro e valida o tamanho.
     */
    private static String sanitizeFileName(String fileName)
            throws InvalidFileException {

        if (fileName == null || fileName.isBlank()) {
            throw new InvalidFileException(
                    "O nome do ficheiro não pode estar vazio."
            );
        }

        String cleanPath = StringUtils.cleanPath(fileName);

        int lastSeparator = Math.max(
                cleanPath.lastIndexOf('/'),
                cleanPath.lastIndexOf('\\')
        );

        String nameOnly = (
                lastSeparator >= 0
                        ? cleanPath.substring(lastSeparator + 1)
                        : cleanPath
        ).trim();

        if (nameOnly.isEmpty()) {
            throw new InvalidFileException(
                    "O nome do ficheiro não pode estar vazio."
            );
        }

        if (nameOnly.length() > MAX_FILE_NAME_LENGTH) {
            throw new InvalidFileException(
                    "O nome do ficheiro é demasiado longo."
            );
        }

        return nameOnly;
    }
}
