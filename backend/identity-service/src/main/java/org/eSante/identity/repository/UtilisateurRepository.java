package org.eSante.identity.repository;

import org.eSante.identity.domain.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UtilisateurRepository extends JpaRepository<Utilisateur, Integer> {
    Optional<Utilisateur> findByEmail(String email);

    @Query("select u from Utilisateur u join fetch u.role where u.email = :email")
    Optional<Utilisateur> findByEmailWithRole(@Param("email") String email);

    @Query("select u from Utilisateur u join fetch u.role")
    java.util.List<Utilisateur> findAllWithRole();

    @Query("select u from Utilisateur u join fetch u.role where u.role.nom = :roleNom")
    java.util.List<Utilisateur> findAllByRoleNom(@Param("roleNom") String roleNom);
}

