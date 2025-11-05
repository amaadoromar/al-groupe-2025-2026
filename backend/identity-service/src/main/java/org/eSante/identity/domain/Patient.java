package org.eSante.identity.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
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

    @Column(name = "poids_kg", precision = 5, scale = 2)
    private BigDecimal poidsKg;

    @Column(name = "pathologie_principale", length = 255)
    private String pathologiePrincipale;

    @Column(name = "form_json")
    private String formJson;

    // Structured fields extracted from form_json for querying
    @Column(name = "fumeur", length = 3)
    private String fumeur; // OUI/NON

    @Column(name = "alcool", length = 3)
    private String alcool; // OUI/NON

    @Column(name = "activite", length = 12)
    private String activite; // PEU/MODEREE/REGULIERE

    @Column(name = "douleur")
    private Integer douleur; // 0-10

    @Column(name = "symptomes", columnDefinition = "text")
    private String symptomes;

    @Column(name = "medicaments", columnDefinition = "text")
    private String medicaments;

    @Column(name = "allergies", columnDefinition = "text")
    private String allergies;

    @Column(name = "antecedents", columnDefinition = "text")
    private String antecedents;

    public Integer getId() { return id; }
    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) { this.utilisateur = utilisateur; }
    public LocalDate getDateNaissance() { return dateNaissance; }
    public void setDateNaissance(LocalDate dateNaissance) { this.dateNaissance = dateNaissance; }
    public String getSexe() { return sexe; }
    public void setSexe(String sexe) { this.sexe = sexe; }
    public Integer getTailleCm() { return tailleCm; }
    public void setTailleCm(Integer tailleCm) { this.tailleCm = tailleCm; }
    public BigDecimal getPoidsKg() { return poidsKg; }
    public void setPoidsKg(BigDecimal poidsKg) { this.poidsKg = poidsKg; }
    public String getPathologiePrincipale() { return pathologiePrincipale; }
    public void setPathologiePrincipale(String pathologiePrincipale) { this.pathologiePrincipale = pathologiePrincipale; }
    public String getFormJson() { return formJson; }
    public void setFormJson(String formJson) { this.formJson = formJson; }

    public String getFumeur() { return fumeur; }
    public void setFumeur(String fumeur) { this.fumeur = fumeur; }
    public String getAlcool() { return alcool; }
    public void setAlcool(String alcool) { this.alcool = alcool; }
    public String getActivite() { return activite; }
    public void setActivite(String activite) { this.activite = activite; }
    public Integer getDouleur() { return douleur; }
    public void setDouleur(Integer douleur) { this.douleur = douleur; }
    public String getSymptomes() { return symptomes; }
    public void setSymptomes(String symptomes) { this.symptomes = symptomes; }
    public String getMedicaments() { return medicaments; }
    public void setMedicaments(String medicaments) { this.medicaments = medicaments; }
    public String getAllergies() { return allergies; }
    public void setAllergies(String allergies) { this.allergies = allergies; }
    public String getAntecedents() { return antecedents; }
    public void setAntecedents(String antecedents) { this.antecedents = antecedents; }
}

