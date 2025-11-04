package org.eSante.identity.domain;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "patients")
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id; // SERIAL

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "utilisateur_id", nullable = false, unique = true)
    private Utilisateur utilisateur;

    @Column(name = "date_naissance")
    private LocalDate dateNaissance;

    @Column(name = "sexe", length = 10)
    private String sexe;

    @Column(name = "taille_cm")
    private Integer tailleCm;

    @Column(name = "poids_kg")
    private Double poidsKg;

    @Column(name = "pathologie_principale", length = 255)
    private String pathologiePrincipale;

    public Integer getId() { return id; }
    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) { this.utilisateur = utilisateur; }
    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }
    public Integer getTailleCm() { return tailleCm; }
    public void setTailleCm(Integer tailleCm) { this.tailleCm = tailleCm; }
    public Double getPoidsKg() { return poidsKg; }
    public void setPoidsKg(Double poidsKg) { this.poidsKg = poidsKg; }
    public String getPathologiePrincipale() { return pathologiePrincipale; }
    public void setPathologiePrincipale(String pathologiePrincipale) { this.pathologiePrincipale = pathologiePrincipale; }
}

