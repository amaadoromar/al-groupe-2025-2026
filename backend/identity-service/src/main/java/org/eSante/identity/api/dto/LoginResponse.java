package org.eSante.identity.api.dto;

import java.util.List;

public class LoginResponse {
    public String accessToken;
    public Integer userId;
    public String email;
    public String nom;
    public String prenom;
    public String role; // ADMIN, DOCTEUR, INFIRMIER, PATIENT, PROCHE
    public Integer patientId; // present if PATIENT
    public Integer friendOfPatientId; // present if PROCHE
    public List<String> roles; // duplicate for convenience
}

