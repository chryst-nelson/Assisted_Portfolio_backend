-- M3: admin auth. First admin row is bootstrapped from ADMIN_EMAIL/ADMIN_PASSWORD
-- env vars at startup (see AdminBootstrapRunner) — nothing seeded here.

CREATE TABLE admin_users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'ADMIN',
    created_at TIMESTAMP NOT NULL DEFAULT now()
);
