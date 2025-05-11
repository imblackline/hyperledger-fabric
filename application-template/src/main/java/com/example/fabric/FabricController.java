package com.example.fabric;

import org.hyperledger.fabric.client.GatewayException;
import org.hyperledger.fabric.client.CommitException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/basil")
@CrossOrigin(origins = "http://localhost:4200", allowCredentials = "true")
public class FabricController {

    private final FabricService fabricService;

    @Autowired
    public FabricController(FabricService fabricService) {
        this.fabricService = fabricService;
    }

    @PostMapping
    public ResponseEntity<String> createBasil(@RequestParam String id, @RequestParam String country) {
        try {
            String result = fabricService.createBasil(id, country);
            return ResponseEntity.ok(result);
        } catch (GatewayException | CommitException e) {
            return ResponseEntity.internalServerError().body("Error creating basil: " + e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<String> readBasil(@PathVariable String id) {
        try {
            String result = fabricService.readBasil(id);
            return ResponseEntity.ok(result);
        } catch (GatewayException e) {
            return ResponseEntity.internalServerError().body("Error reading basil: " + e.getMessage());
        }
    }
} 