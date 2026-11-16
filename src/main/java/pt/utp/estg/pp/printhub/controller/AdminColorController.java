
package pt.utp.estg.pp.printhub.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import pt.utp.estg.pp.printhub.dto.ColorDTO;
import pt.utp.estg.pp.printhub.service.ColorService;
import pt.utp.estg.pp.printhub.service.StockService;

import java.math.BigDecimal;
import java.util.Map;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/admin/cores")
public class AdminColorController {

    private final ColorService colorService;
    private final StockService stockService;

    public AdminColorController(
            ColorService colorService,
            StockService stockService) {

        this.colorService = colorService;
        this.stockService = stockService;
    }

    // Criar cor
    @PostMapping
    public ResponseEntity<ColorDTO> createColor(
            @RequestBody ColorDTO dto) {

        try {
            ColorDTO created = colorService.createColor(dto);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(created);

        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Editar cor
    @PutMapping("/{id}")
    public ResponseEntity<ColorDTO> updateColor(
            @PathVariable Integer id,
            @RequestBody ColorDTO dto) {

        try {
            ColorDTO updated = colorService.updateColor(id, dto);

            return ResponseEntity.ok(updated);

        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    // Tornar cor indisponível
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteColor(
            @PathVariable Integer id) {

        try {
            colorService.deleteColor(id);

            return ResponseEntity.noContent().build();

        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    // Consultar stock disponível de uma cor
    @GetMapping("/{id}/stock")
    public ResponseEntity<?> getStock(
            @PathVariable Integer id,
            @RequestParam Integer materialId) {

        try {
            BigDecimal stock = stockService.getAvailableStock(
                    materialId,
                    id
            );

            return ResponseEntity.ok(
                    Map.of(
                            "colorId", id,
                            "materialId", materialId,
                            "stockGrams", stock
                    )
            );

        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // Adicionar stock a uma cor
    @PatchMapping("/{id}/stock")
    public ResponseEntity<?> addStock(
            @PathVariable Integer id,
            @RequestParam Integer materialId,
            @RequestBody Map<String, BigDecimal> body) {

        try {
            BigDecimal gramsToAdd = body.get("grams");

            BigDecimal newStock = stockService.addStock(
                    materialId,
                    id,
                    gramsToAdd
            );

            return ResponseEntity.ok(
                    Map.of(
                            "colorId", id,
                            "materialId", materialId,
                            "stockGrams", newStock
                    )
            );

        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();

        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}
