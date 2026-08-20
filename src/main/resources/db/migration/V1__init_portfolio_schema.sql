-- M1: portfolio content tables only. admin_users (M3), conversations/messages (M6),
-- and vector_documents (M7) are introduced in their own milestone migrations.

CREATE TABLE profiles (
    id BIGSERIAL PRIMARY KEY,
    full_name VARCHAR(255) NOT NULL,
    title VARCHAR(255),
    bio TEXT,
    location VARCHAR(255),
    interests TEXT,
    career_goals TEXT,
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE skills (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE,
    category VARCHAR(50) NOT NULL,
    proficiency VARCHAR(50),
    is_currently_learning BOOLEAN NOT NULL DEFAULT false,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE projects (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    slug VARCHAR(255) NOT NULL UNIQUE,
    description TEXT,
    problem_solved TEXT,
    category VARCHAR(100),
    github_url VARCHAR(500),
    live_url VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE project_technologies (
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    skill_id BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    PRIMARY KEY (project_id, skill_id)
);

CREATE TABLE project_screenshots (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    url VARCHAR(500) NOT NULL,
    caption VARCHAR(255),
    sort_order INT NOT NULL DEFAULT 0
);

CREATE TABLE project_challenges (
    id BIGSERIAL PRIMARY KEY,
    project_id BIGINT NOT NULL REFERENCES projects(id) ON DELETE CASCADE,
    challenge TEXT NOT NULL,
    lesson_learned TEXT
);

CREATE TABLE experiences (
    id BIGSERIAL PRIMARY KEY,
    company VARCHAR(255) NOT NULL,
    role VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    responsibilities TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT now()
);

CREATE TABLE experience_technologies (
    experience_id BIGINT NOT NULL REFERENCES experiences(id) ON DELETE CASCADE,
    skill_id BIGINT NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    PRIMARY KEY (experience_id, skill_id)
);

CREATE TABLE experience_achievements (
    id BIGSERIAL PRIMARY KEY,
    experience_id BIGINT NOT NULL REFERENCES experiences(id) ON DELETE CASCADE,
    achievement TEXT NOT NULL
);

CREATE TABLE education (
    id BIGSERIAL PRIMARY KEY,
    institution VARCHAR(255) NOT NULL,
    degree VARCHAR(255),
    course VARCHAR(255),
    details TEXT,
    start_date DATE,
    end_date DATE
);

CREATE TABLE certifications (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    provider VARCHAR(255),
    issued_date DATE,
    credential_url VARCHAR(500)
);

CREATE TABLE achievements (
    id BIGSERIAL PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    date DATE,
    verified BOOLEAN NOT NULL DEFAULT true
);

CREATE TABLE contact_info (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL,
    linkedin_url VARCHAR(500),
    github_url VARCHAR(500),
    other_links JSONB
);

CREATE TABLE resume_files (
    id BIGSERIAL PRIMARY KEY,
    file_url VARCHAR(500) NOT NULL,
    version VARCHAR(50),
    uploaded_at TIMESTAMP NOT NULL DEFAULT now(),
    is_active BOOLEAN NOT NULL DEFAULT true
);

CREATE INDEX idx_projects_category ON projects(category);
CREATE INDEX idx_skills_category ON skills(category);
CREATE INDEX idx_achievements_verified ON achievements(verified);
