package org.eSante.identity.repository;

import org.eSante.identity.domain.Proche;
import org.eSante.identity.domain.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcheRepository extends JpaRepository<Proche, Integer> {
    Optional<Proche> findByUtilisateur(Utilisateur utilisateur);
}

