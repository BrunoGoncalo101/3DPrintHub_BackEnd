package pt.utp.estg.pp.printhub.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import pt.utp.estg.pp.printhub.dto.ProductionQueueItemDTO;
import pt.utp.estg.pp.printhub.service.CustomRequestService;

import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/admin/production")
public class AdminProductionController {

    private final CustomRequestService customRequestService;

    public AdminProductionController(CustomRequestService customRequestService) {
        this.customRequestService = customRequestService;
    }

    @GetMapping
    public ResponseEntity<List<ProductionQueueItemDTO>> getProductionQueue() {
        return ResponseEntity.ok(customRequestService.getProductionQueue());
    }
}