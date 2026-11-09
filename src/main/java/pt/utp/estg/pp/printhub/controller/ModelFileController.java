package pt.utp.estg.pp.printhub.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import pt.utp.estg.pp.printhub.dto.ModelFileResponse;
import pt.utp.estg.pp.printhub.dto.QuoteResponse;
import pt.utp.estg.pp.printhub.entity.ModelFile;
import pt.utp.estg.pp.printhub.exception.InvalidFileException;
import pt.utp.estg.pp.printhub.service.ModelFileService;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/files")
public class ModelFileController {

    private final ModelFileService service;

    public ModelFileController(ModelFileService service) {
        this.service = service;
    }

    /**
     * Upload de um ficheiro 3D (.stl, .3mf, .obj, máx. 50 MB).
     * Pedido multipart/form-data com os campos: file, userId.
     */
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ModelFileResponse> upload(
            @RequestParam("file") MultipartFile file,
            // TODO: Quando o login (JWT) estiver pronto, o utilizador vem do token e este parâmetro sai
            @RequestParam("userId") Integer userId) throws InvalidFileException {

        ModelFile saved = service.save(file, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(ModelFileResponse.from(saved));
    }

    /**
     * Calcula o orçamento estimado para a impressão 3D de um ficheiro específico.
     * Recebe parâmetros opcionais de material e percentagem de preenchimento (infill).
     */
    @GetMapping("/{id}/budget")
    public ResponseEntity<QuoteResponse> getQuote(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "1") Integer materialId,
            @RequestParam(defaultValue = "20") int infillPercent) throws InvalidFileException {

        QuoteResponse quote = service.calculateQuote(id, materialId, infillPercent);
        return ResponseEntity.ok(quote);
    }
}