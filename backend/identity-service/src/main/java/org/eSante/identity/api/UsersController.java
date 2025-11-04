package org.eSante.identity.api;

import jakarta.validation.Valid;
import org.eSante.identity.api.dto.UserCreateRequest;
import org.eSante.identity.api.dto.UserResponse;
import org.eSante.identity.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;
import org.eSante.identity.domain.Utilisateur;

@RestController
@RequestMapping("/api/users")
public class UsersController {
    private final UserService userService;

    public UsersController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserCreateRequest req) {
        Utilisateur u = userService.createUser(req.nom, req.prenom, req.email, req.password, req.role);
        UserResponse r = new UserResponse();
        r.id = u.getId();
        r.nom = u.getNom();
        r.prenom = u.getPrenom();
        r.email = u.getEmail();
        r.role = u.getRole().getNom();
        return ResponseEntity.ok(r);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','DOCTEUR','INFIRMIER')")
    public ResponseEntity<List<UserResponse>> list(@RequestParam(name = "role", required = false) String role) {
        List<UserResponse> list = userService.listByRole(role).stream().map(u -> {
            UserResponse r = new UserResponse();
            r.id = u.getId();
            r.nom = u.getNom();
            r.prenom = u.getPrenom();
            r.email = u.getEmail();
            r.role = u.getRole().getNom();
            return r;
        }).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }
}

