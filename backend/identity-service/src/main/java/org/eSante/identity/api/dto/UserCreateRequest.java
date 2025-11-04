package org.eSante.identity.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserCreateRequest {
    @NotBlank
    public String nom;
    @NotBlank
    public String prenom;
    @NotBlank @Email
    public String email;
    @NotBlank @Size(min = 3, max = 128)
    public String password;
    @NotBlank
    public String role; // ADMIN, DOCTEUR, INFIRMIER, PATIENT, PROCHE
}

