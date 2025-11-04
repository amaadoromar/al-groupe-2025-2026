package org.eSante.identity.api.dto;

import jakarta.validation.constraints.NotNull;

public class ProcheCreateRequest {
    @NotNull
    public Integer utilisateurId; // FRIEND user id
    @NotNull
    public Integer patientId;     // linked patient id
    public String lien;           // relationship label
}

