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
    private final org.eSante.identity.service.UserService userService;

    public PatientsController(PatientRepository patients, UtilisateurRepository utilisateurs, org.eSante.identity.service.UserService userService) {
        this.patients = patients;
        this.utilisateurs = utilisateurs;
        this.userService = userService;
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

    public static class NewPatientBootstrap {
        public String nom;
        public String prenom;
        public String email;
        public String password;
        public String dateNaissance;
        public String sexe;
        public Integer tailleCm;
        public Double poidsKg;
        public String pathologiePrincipale;
    }

    @PostMapping("/bootstrap")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTEUR','INFIRMIER')")
    public ResponseEntity<Map<String,Integer>> createUserAndPatient(@RequestBody NewPatientBootstrap req) {
        if (req == null) throw new IllegalArgumentException("Body required");
        if (req.nom == null || req.prenom == null || req.email == null || req.password == null) {
            throw new IllegalArgumentException("Champs utilisateur requis: nom, prenom, email, password");
        }
        Utilisateur user = userService.createUser(req.nom, req.prenom, req.email, req.password, "PATIENT");
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
        p = patients.save(p);
        Map<String,Integer> out = new HashMap<>();
        out.put("userId", user.getId());
        out.put("patientId", p.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(out);
    }

    // Alternate variant using request parameter for easier client integration
    @GetMapping("/form")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTEUR','INFIRMIER')")
    public ResponseEntity<Map<String,Object>> getFormByQuery(@RequestParam("patientId") Integer id) {
        return getForm(id);
    }

    public static class PatientFormUpdate { public String form; }

    @PutMapping("/{id}/form")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTEUR','INFIRMIER')")
    public ResponseEntity<Void> updateForm(@PathVariable Integer id, @RequestBody PatientFormUpdate body) {
        Patient p = patients.findById(id).orElseThrow(() -> new NoSuchElementException("Patient not found"));
        p.setFormJson(body.form != null ? body.form : "{}");
        // Try to map some structured fields from JSON into columns (optional)
        try {
            com.fasterxml.jackson.databind.ObjectMapper om = new com.fasterxml.jackson.databind.ObjectMapper();
            com.fasterxml.jackson.databind.JsonNode n = om.readTree(p.getFormJson());
            if (n.has("tailleCm") && !n.get("tailleCm").isNull()) {
                p.setTailleCm(n.get("tailleCm").asInt());
            }
            if (n.has("poidsKg") && !n.get("poidsKg").isNull()) {
                java.math.BigDecimal kg = new java.math.BigDecimal(n.get("poidsKg").asDouble());
                p.setPoidsKg(kg);
            }
            if (n.has("fumeur")) p.setFumeur(safeUpper(n.get("fumeur")));
            if (n.has("alcool")) p.setAlcool(safeUpper(n.get("alcool")));
            if (n.has("activite")) p.setActivite(safeUpper(n.get("activite")));
            if (n.has("douleur") && !n.get("douleur").isNull()) p.setDouleur(n.get("douleur").asInt());
            if (n.has("symptomes")) p.setSymptomes(safeStr(n.get("symptomes")));
            if (n.has("medicaments")) p.setMedicaments(safeStr(n.get("medicaments")));
            if (n.has("allergies")) p.setAllergies(safeStr(n.get("allergies")));
            if (n.has("antecedents")) p.setAntecedents(safeStr(n.get("antecedents")));
            // Pathologie principale could be derived from antecedents/symptomes if desired
        } catch (Exception ignore) {}
        patients.save(p);
        return ResponseEntity.noContent().build();
    }

    // Alternate variant using request parameter for easier client integration
    @PutMapping("/form")
    @PreAuthorize("hasAnyRole('ADMIN','DOCTEUR','INFIRMIER')")
    public ResponseEntity<Void> updateFormByQuery(@RequestParam("patientId") Integer id, @RequestBody PatientFormUpdate body) {
        return updateForm(id, body);
    }

    private static String safeUpper(com.fasterxml.jackson.databind.JsonNode n) {
        if (n == null || n.isNull()) return null;
        String s = n.asText("").trim();
        return s.isEmpty() ? null : s.toUpperCase();
    }
    private static String safeStr(com.fasterxml.jackson.databind.JsonNode n) {
        if (n == null || n.isNull()) return null;
        String s = n.asText("").trim();
        return s.isEmpty() ? null : s;
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


