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
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.NoSuchElementException;
import org.eSante.identity.domain.Utilisateur;

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
        Utilisateur user = utilisateurs.findById(req.utilisateurId).orElseThrow(() -> new IllegalArgumentException("Utilisateur not found"));
        Patient p = new Patient();
        p.setUtilisateur(user);
        if (req.dateNaissance != null && !req.dateNaissance.trim().isEmpty()) {
            p.setDateNaissance(LocalDate.parse(req.dateNaissance));
        }
        p.setSexe(req.sexe);
        p.setTailleCm(req.tailleCm);
        if (req.poidsKg != null) {
            p.setPoidsKg(BigDecimal.valueOf(req.poidsKg));
        }
        p.setPathologiePrincipale(req.pathologiePrincipale);
        Patient saved = patients.save(p);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.getId());
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DOCTEUR','INFIRMIER')")
    public ResponseEntity<List<Map<String,Object>>> list() {
        List<Patient> list = patients.findAllWithUtilisateur();
        List<Map<String,Object>> out = list.stream().map(p -> {
            Map<String,Object> m = new HashMap<>();
            m.put("id", p.getId());
            m.put("utilisateurId", p.getUtilisateur().getId());
            m.put("nom", p.getUtilisateur().getNom());
            m.put("prenom", p.getUtilisateur().getPrenom());
            m.put("email", p.getUtilisateur().getEmail());
            return m;
        }).collect(java.util.stream.Collectors.toList());
        return ResponseEntity.ok(out);
    }

    @GetMapping("/{id}/form")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTEUR','INFIRMIER')")
    public ResponseEntity<Map<String,Object>> getForm(@PathVariable Integer id) {
        Patient p = patients.findById(id).orElseThrow(() -> new NoSuchElementException("Patient not found"));
        Map<String,Object> out = new HashMap<>();
        out.put("form", p.getFormJson() != null ? p.getFormJson() : "{}");
        return ResponseEntity.ok(out);
    }

    public static class PatientFormUpdate { public String form; }

    @PutMapping("/{id}/form")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTEUR','INFIRMIER')")
    public ResponseEntity<Void> updateForm(@PathVariable Integer id, @RequestBody PatientFormUpdate body) {
        Patient p = patients.findById(id).orElseThrow(() -> new NoSuchElementException("Patient not found"));
        p.setFormJson(body.form != null ? body.form : "{}");
        patients.save(p);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/by-user/{userId}/ensure")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTEUR','INFIRMIER')")
    public ResponseEntity<Map<String,Integer>> ensurePatient(@PathVariable Integer userId) {
        Utilisateur user = utilisateurs.findById(userId).orElseThrow(() -> new NoSuchElementException("Utilisateur not found"));
        Patient p = patients.findByUtilisateur(user).orElse(null);
        if (p == null) {
            p = new Patient();
            p.setUtilisateur(user);
            p = patients.save(p);
        }
        Map<String,Integer> out = new HashMap<>();
        out.put("patientId", p.getId());
        return ResponseEntity.ok(out);
    }
}


