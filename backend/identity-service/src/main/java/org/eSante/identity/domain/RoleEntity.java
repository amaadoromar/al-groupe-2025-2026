package org.eSante.identity.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "roles")
public class RoleEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // SERIAL in SQL init

    @Column(name = "nom", nullable = false, unique = true, length = 50)
    private String nom; // ADMIN, DOCTEUR, INFIRMIER, PATIENT, PROCHE

    public Integer getId() { return id; }
    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }
}

