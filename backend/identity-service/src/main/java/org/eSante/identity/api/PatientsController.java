package org.eSante.identity.api;

import jakarta.validation.Valid;
import org.eSante.identity.api.dto.PatientCreateRequest;
import org.eSante.identity.domain.Patient;
import org.eSante.identity.repository.PatientRepository;
import org.eSante.identity.repository.UtilisateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/patients")
public class PatientsController {
    private final PatientRepository patients;
    private final UtilisateurRepository utilisateurs;

    public PatientsController(PatientRepository patients, UtilisateurRepository utilisateurs) {
        this.patients = patients;
        this.utilisateurs = utilisateurs;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Integer> create(@Valid @RequestBody PatientCreateRequest req) {
        var user = utilisateurs.findById(req.utilisateurId).orElseThrow(() -> new IllegalArgumentException("Utilisateur not found"));
        Patient p = new Patient();
        p.setUtilisateur(user);
        if (req.dateNaissance != null && !req.dateNaissance.isBlank()) {
            p.setDateNaissance(LocalDate.parse(req.dateNaissance));
        }
        p.setSexe(req.sexe);
        p.setTailleCm(req.tailleCm);
        p.setPoidsKg(req.poidsKg);
        p.setPathologiePrincipale(req.pathologiePrincipale);
        var saved = patients.save(p);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.getId());
    }
}

