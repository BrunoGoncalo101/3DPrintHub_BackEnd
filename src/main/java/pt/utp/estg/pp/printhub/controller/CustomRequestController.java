package pt.utp.estg.pp.printhub.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pt.utp.estg.pp.printhub.dto.*;
import pt.utp.estg.pp.printhub.exception.InvalidFileException;
import pt.utp.estg.pp.printhub.service.CustomRequestService;

import java.util.List;

@RestController
@RequestMapping("/custom-requests")
public class CustomRequestController {

    private final CustomRequestService service;

    public CustomRequestController(CustomRequestService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<CustomRequestResponse> create(@Valid @RequestBody CreateCustomRequestDTO dto) throws InvalidFileException {
        CustomRequestResponse response = service.createRequest(dto);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<CustomRequestResponse>> getAll() {
        return ResponseEntity.ok(service.getAllRequests());
    }

    @PatchMapping("/{id}/approve")
    public ResponseEntity<CustomRequestResponse> approve(
            @PathVariable Integer id,
            @Valid @RequestBody ApproveCustomRequestDTO dto) throws InvalidFileException {
        return ResponseEntity.ok(service.approveRequest(id, dto));
    }

    @PatchMapping("/{id}/reject")
    public ResponseEntity<CustomRequestResponse> reject(
            @PathVariable Integer id,
            @RequestBody(required = false) RejectCustomRequestDTO dto) throws InvalidFileException {
        return ResponseEntity.ok(service.rejectRequest(id, dto));
    }
}