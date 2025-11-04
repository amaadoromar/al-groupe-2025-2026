package org.eSante.identity.api;

import jakarta.validation.Valid;
import org.eSante.identity.api.dto.ProcheCreateRequest;
import org.eSante.identity.domain.Proche;
import org.eSante.identity.repository.PatientRepository;
import org.eSante.identity.repository.ProcheRepository;
import org.eSante.identity.repository.UtilisateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/proches")
public class ProchesController {
    private final ProcheRepository proches;
    private final UtilisateurRepository utilisateurs;
    private final PatientRepository patients;

    public ProchesController(ProcheRepository proches, UtilisateurRepository utilisateurs, PatientRepository patients) {
        this.proches = proches;
        this.utilisateurs = utilisateurs;
        this.patients = patients;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Integer> create(@Valid @RequestBody ProcheCreateRequest req) {
        var u = utilisateurs.findById(req.utilisateurId).orElseThrow(() -> new IllegalArgumentException("Utilisateur not found"));
        var p = patients.findById(req.patientId).orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        Proche pr = new Proche();
        pr.setUtilisateur(u);
        pr.setPatient(p);
        pr.setLien(req.lien);
        var saved = proches.save(pr);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.getId());
    }
}

