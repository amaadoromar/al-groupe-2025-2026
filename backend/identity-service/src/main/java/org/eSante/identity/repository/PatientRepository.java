package org.eSante.identity.repository;

import org.eSante.identity.domain.Patient;
import org.eSante.identity.domain.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PatientRepository extends JpaRepository<Patient, Integer> {
    Optional<Patient> findByUtilisateur(Utilisateur utilisateur);
}

