package org.eSante.identity.api.dto;

import jakarta.validation.constraints.NotNull;

public class PatientCreateRequest {
    @NotNull
    public Integer utilisateurId;
    public String dateNaissance; // ISO yyyy-MM-dd
    public String sexe;
    public Integer tailleCm;
    public Double poidsKg;
    public String pathologiePrincipale;
}

