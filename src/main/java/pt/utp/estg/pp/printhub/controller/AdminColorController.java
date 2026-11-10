
package pt.utp.estg.pp.printhub.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.utp.estg.pp.printhub.dto.ColorDTO;
import pt.utp.estg.pp.printhub.service.ColorService;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("/admin/cores")
public class AdminColorController {

    private final ColorService colorService;

    public AdminColorController(ColorService colorService) {
        this.colorService = colorService;
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
}
