package org.eSante.identity.api;

import jakarta.validation.constraints.NotNull;
import org.eSante.identity.domain.Observation;
import org.eSante.identity.domain.Patient;
import org.eSante.identity.domain.Utilisateur;
import org.eSante.identity.repository.ObservationRepository;
import org.eSante.identity.repository.PatientRepository;
import org.eSante.identity.repository.UtilisateurRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/observations")
public class ObservationsController {
    private final ObservationRepository observations;
    private final PatientRepository patients;
    private final UtilisateurRepository users;

    public ObservationsController(ObservationRepository observations, PatientRepository patients, UtilisateurRepository users) {
        this.observations = observations;
        this.patients = patients;
        this.users = users;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DOCTEUR','INFIRMIER')")
    public ResponseEntity<List<Map<String,Object>>> list(@RequestParam(name = "patientId") Integer patientId) {
        List<Observation> list = observations.findByPatientId(patientId);
        return ResponseEntity.ok(list.stream().map(o -> {
            Map<String,Object> m = new HashMap<>();
            m.put("id", o.getId());
            m.put("patientId", o.getPatient().getId());
            m.put("authorId", o.getAuthor().getId());
            m.put("content", o.getContent());
            m.put("type", o.getType());
            m.put("createdAt", o.getCreatedAt().toString());
            return m;
        }).collect(java.util.stream.Collectors.toList()));
    }

    public static class ObservationCreateRequest {
        @NotNull public Integer patientId;
        @NotNull public String content;
        public String type; // NOTE or QUESTION
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','DOCTEUR','INFIRMIER','PATIENT')")
    public ResponseEntity<Integer> create(@RequestBody ObservationCreateRequest req) {
        Utilisateur author = currentUser();
        Patient p;
        // If a PATIENT is posting, bind to their own patient record regardless of body
        if (author.getRole() != null && "PATIENT".equalsIgnoreCase(author.getRole().getNom())) {
            p = patients.findByUtilisateur(author).orElseThrow(() -> new IllegalArgumentException("Patient profile not found for user"));
        } else {
            p = patients.findById(req.patientId).orElseThrow(() -> new IllegalArgumentException("Patient not found"));
        }
        Observation o = new Observation();
        o.setPatient(p);
        o.setAuthor(author);
        o.setContent(req.content);
        if (req.type != null && !req.type.trim().isEmpty()) {
            o.setType(req.type.trim().toUpperCase());
        }
        Observation saved = observations.save(o);
        return ResponseEntity.status(HttpStatus.CREATED).body(saved.getId());
    }

    private Utilisateur currentUser() {
        Authentication a = SecurityContextHolder.getContext().getAuthentication();
        String email = a.getName();
        return users.findByEmail(email).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }
}

