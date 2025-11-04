package org.eSante.identity.service;

import org.eSante.identity.domain.RoleEntity;
import org.eSante.identity.domain.Utilisateur;
import org.eSante.identity.repository.RoleRepository;
import org.eSante.identity.repository.UtilisateurRepository;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class UserService implements UserDetailsService {
    private final UtilisateurRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    public UserService(UtilisateurRepository users, RoleRepository roles, PasswordEncoder encoder) {
        this.users = users;
        this.roles = roles;
        this.encoder = encoder;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        Utilisateur u = users.findByEmail(username).orElseThrow(() -> new UsernameNotFoundException("User not found"));
        String roleName = u.getRole().getNom();
        return User.withUsername(u.getEmail())
                .password(u.getMotDePasse())
                .authorities("ROLE_" + roleName)
                .build();
    }

    @Transactional
    public Utilisateur createUser(String nom, String prenom, String email, String password, String roleNom) {
        RoleEntity role = roles.findByNom(roleNom).orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleNom));
        Utilisateur u = new Utilisateur();
        u.setNom(nom);
        u.setPrenom(prenom);
        u.setEmail(email);
        u.setMotDePasse(encoder.encode(password));
        u.setRole(role);
        return users.save(u);
    }

    public boolean matchesPassword(String raw, String stored) {
        // Accept plaintext for legacy seeded users, otherwise bcrypt
        if (stored != null && stored.startsWith("$2a$") || (stored != null && stored.startsWith("$2b$")) || (stored != null && stored.startsWith("$2y$"))) {
            return encoder.matches(raw, stored);
        }
        return raw.equals(stored);
    }

    public List<Utilisateur> listByRole(String roleNom) {
        if (roleNom == null || roleNom.trim().isEmpty()) return users.findAll();
        // lightweight filter to avoid extra query methods
        return users.findAll().stream().filter(u -> roleNom.equals(u.getRole().getNom()))
                .collect(Collectors.toList());
    }
}

