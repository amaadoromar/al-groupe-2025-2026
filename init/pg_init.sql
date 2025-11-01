-- ====================================================
-- 1. GESTION DES UTILISATEURS & RÔLES
-- ====================================================

CREATE TABLE roles (
                       id SERIAL PRIMARY KEY,
                       nom VARCHAR(50) UNIQUE NOT NULL
);

CREATE TABLE utilisateurs (
                              id SERIAL PRIMARY KEY,
                              nom VARCHAR(100) NOT NULL,
                              prenom VARCHAR(100) NOT NULL,
                              email VARCHAR(150) UNIQUE NOT NULL,
                              mot_de_passe VARCHAR(255) NOT NULL,
                              role_id INT REFERENCES roles(id)
);

-- ====================================================
-- 2. GESTION DES PATIENTS
-- ====================================================

CREATE TABLE patients (
                          id SERIAL PRIMARY KEY,
                          utilisateur_id INT REFERENCES utilisateurs(id),
                          date_naissance DATE,
                          sexe VARCHAR(10),
                          taille_cm INT,
                          poids_kg DECIMAL(5,2),
                          pathologie_principale VARCHAR(255)
);

CREATE TABLE proches (
                         id SERIAL PRIMARY KEY,
                         patient_id INT REFERENCES patients(id),
                         nom VARCHAR(100),
                         prenom VARCHAR(100),
                         telephone VARCHAR(20),
                         email VARCHAR(150)
);

-- ====================================================
-- 3. PLANIFICATION & SUIVI
-- ====================================================

CREATE TABLE rendez_vous (
                             id SERIAL PRIMARY KEY,
                             patient_id INT REFERENCES patients(id),
                             date_rdv TIMESTAMP NOT NULL,
                             type_rdv VARCHAR(100),
                             commentaire TEXT
);

-- ====================================================
-- 4. ALERTES & URGENCES
-- ====================================================

CREATE TABLE alertes (
                         id SERIAL PRIMARY KEY,
                         patient_id INT REFERENCES patients(id),
                         type_alerte VARCHAR(100),
                         niveau VARCHAR(50),
                         message TEXT,
                         date_creation TIMESTAMP DEFAULT NOW(),
                         etat VARCHAR(20) DEFAULT 'EN_ATTENTE'
);

-- ====================================================
-- 5. RAPPORTS & BILANS
-- ====================================================

CREATE TABLE rapports (
                          id SERIAL PRIMARY KEY,
                          patient_id INT REFERENCES patients(id),
                          type_rapport VARCHAR(100),
                          contenu TEXT,
                          date_rapport TIMESTAMP DEFAULT NOW()
);

-- ====================================================
-- 6. DONNÉES D'INITIALISATION
-- ====================================================

INSERT INTO roles (nom) VALUES
                            ('ADMIN'), ('DOCTEUR'), ('INFIRMIER'), ('PATIENT'), ('PROCHE');

INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe, role_id) VALUES
                                                                         ('Dupont', 'Alice', 'alice.dupont@example.com', 'pass', 4),
                                                                         ('Martin', 'Lucas', 'lucas.martin@example.com', 'pass', 2),
                                                                         ('Najar', 'Sarra', 'sarra.najar@example.com', 'pass', 1);

INSERT INTO patients (utilisateur_id, date_naissance, sexe, taille_cm, poids_kg, pathologie_principale) VALUES
                                                                                                            (1, '1960-04-12', 'F', 168, 72.8, 'Hypertension'),
                                                                                                            (2, '1955-09-23', 'M', 182, 85.4, 'Diabète');

INSERT INTO rendez_vous (patient_id, date_rdv, type_rdv, commentaire) VALUES
                                                                          (1, NOW() + INTERVAL '3 days', 'Suivi tension', 'Vérification du traitement'),
                                                                          (2, NOW() + INTERVAL '5 days', 'Contrôle glycémie', 'Bilan post-repas');

INSERT INTO alertes (patient_id, type_alerte, niveau, message) VALUES
                                                                   (1, 'Tension élevée', 'ALERTE', 'SBP > 160 détecté à 08:45'),
                                                                   (2, 'Glycémie critique', 'URGENCE', 'Valeur > 300 mg/dL');
