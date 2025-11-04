package org.eSante.identity.domain;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "observations")
public class Observation {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "author_user_id", nullable = false)
    private Utilisateur author;

    @Column(name = "content", columnDefinition = "text")
    private String content;

    @Column(name = "kind", length = 20)
    private String type = "NOTE";

    @Column(name = "created_at")
    private Instant createdAt = Instant.now();

    public Integer getId() { return id; }
    public Patient getPatient() { return patient; }
    public void setPatient(Patient patient) { this.patient = patient; }
    public Utilisateur getAuthor() { return author; }
    public void setAuthor(Utilisateur author) { this.author = author; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
