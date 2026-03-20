CREATE TABLE IF NOT EXISTS test_table_1 (
    id SERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE,
    age INTEGER NOT NULL,
    is_valid BOOLEAN
);

INSERT INTO test_table_1 (name, email, age, is_valid) VALUES
    ('Alice Dupont', 'alice.dupont@example.com', 32, TRUE),
    ('Bob Martin', 'bob.martin@example.com', 18, FALSE),
    ('Charlie Bernard', 'charlie.bernard@example.com', 54, TRUE);

---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS test_table_2 (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);

INSERT INTO test_table_2 (name, email) VALUES
    ('Alice Dupont', 'alice.dupont@example.com'),
    ('Bob Martin', 'bob.martin@example.com'),
    ('Charlie Bernard', 'charlie.bernard@example.com');

---------------------------------------------------------------------

CREATE TABLE IF NOT EXISTS test_table_3 (
    id VARCHAR(200) PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(255) NOT NULL UNIQUE
);

INSERT INTO test_table_3 (id, name, email) VALUES
    ('id_1', 'Alice Dupont', 'alice.dupont@example.com'),
    ('id_2', 'Bob Martin', 'bob.martin@example.com'),
    ('id_3', 'Charlie Bernard', 'charlie.bernard@example.com');
