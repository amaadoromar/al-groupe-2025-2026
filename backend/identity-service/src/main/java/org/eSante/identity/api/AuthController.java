package org.eSante.identity.api;

import jakarta.validation.Valid;
import org.eSante.identity.api.dto.LoginRequest;
import org.eSante.identity.api.dto.LoginResponse;
import org.eSante.identity.domain.Utilisateur;
import org.eSante.identity.repository.PatientRepository;
import org.eSante.identity.repository.ProcheRepository;
import org.eSante.identity.repository.UtilisateurRepository;
import org.eSante.identity.security.JwtService;
import org.eSante.identity.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UtilisateurRepository utilisateurs;
    private final PatientRepository patients;
    private final ProcheRepository proches;
    private final UserService userService;
    private final JwtService jwtService;

    public AuthController(UtilisateurRepository utilisateurs,
                          PatientRepository patients,
                          ProcheRepository proches,
                          UserService userService,
                          JwtService jwtService) {
        this.utilisateurs = utilisateurs;
        this.patients = patients;
        this.proches = proches;
        this.userService = userService;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest req) {
        Utilisateur u = utilisateurs.findByEmail(req.email).orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!userService.matchesPassword(req.password, u.getMotDePasse())) {
            throw new IllegalArgumentException("Invalid credentials");
        }

        String role = u.getRole().getNom();
        Integer patientId = null;
        Integer friendOfPatientId = null;
        if ("PATIENT".equals(role)) {
            patientId = patients.findByUtilisateur(u).map(p -> p.getId()).orElse(null);
        } else if ("PROCHE".equals(role)) {
            friendOfPatientId = proches.findByUtilisateur(u).map(p -> p.getPatient().getId()).orElse(null);
        }

        Map<String, Object> claims = new HashMap<>();
        claims.put("roles", List.of(role));
        if (patientId != null) claims.put("patientId", patientId);
        if (friendOfPatientId != null) claims.put("friendOfPatientId", friendOfPatientId);
        String token = jwtService.create(u.getEmail(), claims);

        LoginResponse resp = new LoginResponse();
        resp.accessToken = token;
        resp.userId = u.getId();
        resp.email = u.getEmail();
        resp.nom = u.getNom();
        resp.prenom = u.getPrenom();
        resp.role = role;
        resp.patientId = patientId;
        resp.friendOfPatientId = friendOfPatientId;
        resp.roles = List.of(role);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> out = new HashMap<>();
        if (auth == null) return ResponseEntity.ok(out);
        out.put("sub", auth.getName());
        if (auth.getDetails() instanceof Map<?,?> m) {
            out.putAll((Map<String, Object>) m);
        }
        return ResponseEntity.ok(out);
    }
}

