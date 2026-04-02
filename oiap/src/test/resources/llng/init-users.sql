CREATE TABLE IF NOT EXISTS users (
    username VARCHAR(255) NOT NULL PRIMARY KEY,
    password VARCHAR(255) NOT NULL,
    cn       VARCHAR(255),
    mail     VARCHAR(255),
    roles    VARCHAR(255)
);

INSERT INTO users (username, password, cn, mail, roles) VALUES ('dwho',     'dwho',     'Doctor Who',  'dwho@badwolf.org',     'admin,user');
INSERT INTO users (username, password, cn, mail, roles) VALUES ('rtyler',   'rtyler',   'Rose Tyler',  'rtyler@badwolf.org',   'user');
INSERT INTO users (username, password, cn, mail, roles) VALUES ('testuser', 'testuser', 'Test User',   'testuser@example.com', 'user');
