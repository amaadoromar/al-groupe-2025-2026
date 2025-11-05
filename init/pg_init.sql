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
                              role_id INT NOT NULL REFERENCES roles(id)
);

-- ====================================================
-- 2. GESTION DES PATIENTS & PROCHES
-- ====================================================

CREATE TABLE patients (
                          id SERIAL PRIMARY KEY,
                          utilisateur_id INT UNIQUE NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
                          date_naissance DATE,
                          sexe VARCHAR(10),
                          taille_cm INT,
                          poids_kg DECIMAL(5,2),
                          pathologie_principale VARCHAR(255)
);

CREATE TABLE proches (
                         id SERIAL PRIMARY KEY,
                         utilisateur_id INT UNIQUE NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
                         patient_id INT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
                         lien VARCHAR(100)
);

-- ====================================================
-- 3. PLANIFICATION & SUIVI
-- ====================================================

CREATE TABLE rendez_vous (
                             id SERIAL PRIMARY KEY,
                             patient_id INT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
                             date_rdv TIMESTAMP NOT NULL,
                             type_rdv VARCHAR(100),
                             commentaire TEXT
);

-- ====================================================
-- 4. ALERTES & URGENCES
-- ====================================================

CREATE TABLE alertes (
                         id SERIAL PRIMARY KEY,
                         patient_id INT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
                         type_alerte VARCHAR(100),
                         niveau VARCHAR(50),
                         message TEXT,
                         date_creation TIMESTAMP DEFAULT NOW(),
                         etat VARCHAR(20) DEFAULT 'EN_ATTENTE'
);

-- ====================================================
-- 5. RAPPORTS & BILANS (✅ corrigé pour correspondre à la classe Report.java)
-- ====================================================

CREATE TABLE IF NOT EXISTS reports (
                                       id SERIAL PRIMARY KEY,
                                       patient_id INT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    report_type VARCHAR(100),
    content TEXT,
    report_date TIMESTAMP DEFAULT NOW(),
    period_start TIMESTAMP,
    period_end TIMESTAMP,
    export_format VARCHAR(50),
    file_path VARCHAR(500),
    status VARCHAR(20) DEFAULT 'GENERATING'
    );

-- Observations for doctors (notes linked to patients)
CREATE TABLE IF NOT EXISTS observations (
    id SERIAL PRIMARY KEY,
    patient_id INT NOT NULL REFERENCES patients(id) ON DELETE CASCADE,
    author_user_id INT NOT NULL REFERENCES utilisateurs(id) ON DELETE CASCADE,
    content TEXT,
    kind VARCHAR(20) DEFAULT 'NOTE',
    created_at TIMESTAMP DEFAULT NOW()
);

-- Extend patients with a JSON form filled by the patient
DO $$ BEGIN
    ALTER TABLE patients ADD COLUMN form_json TEXT;
EXCEPTION WHEN duplicate_column THEN NULL; END $$;

-- Extend patients with structured fields extracted from form_json
DO $$ BEGIN ALTER TABLE patients ADD COLUMN fumeur VARCHAR(3); EXCEPTION WHEN duplicate_column THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE patients ADD COLUMN alcool VARCHAR(3); EXCEPTION WHEN duplicate_column THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE patients ADD COLUMN activite VARCHAR(12); EXCEPTION WHEN duplicate_column THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE patients ADD COLUMN douleur INT; EXCEPTION WHEN duplicate_column THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE patients ADD COLUMN symptomes TEXT; EXCEPTION WHEN duplicate_column THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE patients ADD COLUMN medicaments TEXT; EXCEPTION WHEN duplicate_column THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE patients ADD COLUMN allergies TEXT; EXCEPTION WHEN duplicate_column THEN NULL; END $$;
DO $$ BEGIN ALTER TABLE patients ADD COLUMN antecedents TEXT; EXCEPTION WHEN duplicate_column THEN NULL; END $$;

-- ====================================================
-- 6. DONNÉES D'INITIALISATION
-- ====================================================

-- Rôles de base
INSERT INTO roles (nom) VALUES
                            ('ADMIN'),
                            ('DOCTEUR'),
                            ('INFIRMIER'),
                            ('PATIENT'),
                            ('PROCHE');

--- Utilisateurs
INSERT INTO utilisateurs (nom, prenom, email, mot_de_passe, role_id) VALUES
                                                                         ('Admin', 'Système', 'admin@esante.com', 'admin123', 1),
                                                                         ('Martin', 'Jean', 'jean.martin@esante.com', 'pass123', 2),
                                                                         ('Dubois', 'Sophie', 'sophie.dubois@esante.com', 'pass123', 2),
                                                                         ('Bernard', 'Pierre', 'pierre.bernard@esante.com', 'pass123', 2),
                                                                         ('Petit', 'Marie', 'marie.petit@esante.com', 'pass123', 2),
                                                                         ('Robert', 'Luc', 'luc.robert@esante.com', 'pass123', 2),
                                                                         ('Lefebvre', 'Claire', 'claire.lefebvre@esante.com', 'pass123', 3),
                                                                         ('Moreau', 'Thomas', 'thomas.moreau@esante.com', 'pass123', 3),
                                                                         ('Simon', 'Julie', 'julie.simon@esante.com', 'pass123', 3),
                                                                         ('Laurent', 'Marc', 'marc.laurent@esante.com', 'pass123', 3),
                                                                         ('Michel', 'Anne', 'anne.michel@esante.com', 'pass123', 3),
                                                                         ('Dupont', 'Alice', 'alice.dupont@patient.com', 'pass123', 4),
                                                                         ('Durand', 'Bob', 'bob.durand@patient.com', 'pass123', 4),
                                                                         ('Lemoine', 'Charles', 'charles.lemoine@patient.com', 'pass123', 4),
                                                                         ('Roux', 'Denise', 'denise.roux@patient.com', 'pass123', 4),
                                                                         ('Garnier', 'Émile', 'emile.garnier@patient.com', 'pass123', 4),
                                                                         ('Faure', 'Françoise', 'francoise.faure@patient.com', 'pass123', 4),
                                                                         ('André', 'Georges', 'georges.andre@patient.com', 'pass123', 4),
                                                                         ('Mercier', 'Hélène', 'helene.mercier@patient.com', 'pass123', 4),
                                                                         ('Blanc', 'Isabelle', 'isabelle.blanc@patient.com', 'pass123', 4),
                                                                         ('Guerin', 'Jacques', 'jacques.guerin@patient.com', 'pass123', 4),
                                                                         ('Boyer', 'Karine', 'karine.boyer@patient.com', 'pass123', 4),
                                                                         ('Girard', 'Louis', 'louis.girard@patient.com', 'pass123', 4),
                                                                         ('Vincent', 'Martine', 'martine.vincent@patient.com', 'pass123', 4),
                                                                         ('Rousseau', 'Nicolas', 'nicolas.rousseau@patient.com', 'pass123', 4),
                                                                         ('Leroy', 'Odette', 'odette.leroy@patient.com', 'pass123', 4),
                                                                         ('Bonnet', 'Pascal', 'pascal.bonnet@patient.com', 'pass123', 4),
                                                                         ('François', 'Quentin', 'quentin.francois@patient.com', 'pass123', 4),
                                                                         ('Martinez', 'Rose', 'rose.martinez@patient.com', 'pass123', 4),
                                                                         ('David', 'Sylvie', 'sylvie.david@patient.com', 'pass123', 4),
                                                                         ('Bertrand', 'Thierry', 'thierry.bertrand@patient.com', 'pass123', 4),
                                                                         ('Dupont', 'Emma', 'emma.dupont@proche.com', 'pass123', 5),
                                                                         ('Durand', 'Léa', 'lea.durand@proche.com', 'pass123', 5);

-- Patients
INSERT INTO patients (utilisateur_id, date_naissance, sexe, taille_cm, poids_kg, pathologie_principale) VALUES
                                                                                                            (11, '1955-03-15', 'F', 165, 68.5, 'Hypertension'),
                                                                                                            (12, '1960-07-22', 'M', 178, 82.3, 'Diabète Type 2'),
                                                                                                            (13, '1948-11-30', 'M', 172, 75.0, 'Insuffisance Cardiaque'),
                                                                                                            (14, '1952-05-18', 'F', 160, 65.0, 'Hypertension + Diabète'),
                                                                                                            (15, '1958-09-08', 'M', 180, 90.5, 'Obésité'),
                                                                                                            (16, '1945-02-14', 'F', 158, 60.0, 'Ostéoporose'),
                                                                                                            (17, '1950-12-25', 'M', 175, 78.0, 'BPCO'),
                                                                                                            (18, '1963-04-11', 'F', 168, 72.0, 'Asthme'),
                                                                                                            (19, '1957-08-19', 'F', 162, 66.0, 'Arthrose'),
                                                                                                            (20, '1949-10-03', 'M', 170, 80.0, 'Hypertension');

-- Rapports déjà générés
INSERT INTO reports (patient_id, report_type, content, report_date, period_start, period_end, export_format, file_path, status) VALUES
                                                                                                                                    (1, 'WEEKLY', 'Rapport hebdomadaire patient 1', NOW() - INTERVAL '1 week', NOW() - INTERVAL '14 days', NOW() - INTERVAL '7 days', 'PDF', '/reports/weekly_1_20250101.pdf', 'READY'),
                                                                                                                                    (2, 'WEEKLY', 'Rapport hebdomadaire patient 2', NOW() - INTERVAL '1 week', NOW() - INTERVAL '14 days', NOW() - INTERVAL '7 days', 'PDF', '/reports/weekly_2_20250101.pdf', 'READY'),
                                                                                                                                    (1, 'MONTHLY', 'Rapport mensuel patient 1', NOW() - INTERVAL '1 month', NOW() - INTERVAL '60 days', NOW() - INTERVAL '30 days', 'PDF,CSV', '/reports/monthly_1_20241201.pdf;/reports/monthly_1_20241201.csv', 'READY');

DO  BEGIN
    ALTER TABLE observations ADD COLUMN kind VARCHAR(20) DEFAULT 'NOTE';
EXCEPTION WHEN duplicate_column THEN NULL; END ;

