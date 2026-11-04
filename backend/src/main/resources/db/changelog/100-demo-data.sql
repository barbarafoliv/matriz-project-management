--liquibase formatted sql

-- Demo data with fake people (FR-01). Runs once, only with the "demo" context.
-- All three accounts use the fake demo password listed in the README, stored as a BCrypt hash.
--
-- Access:  Website redesign  PM Alice, member Bob
--          Mobile launch     PM Bob, member Alice
--          Research notes    PM Carol, no other members (Alice and Bob can't see it)

--changeset matriz:100-demo-data context:demo
INSERT INTO app_user (name, email, password_hash) VALUES
    ('Alice Example', 'alice@example.com', '$2a$10$0.TD9FMAX3F6eI19UbLXG.AZX2WCVPwWYrszpj9y4BVdBcJjm6Q4y'),
    ('Bob Example',   'bob@example.com',   '$2a$10$0.TD9FMAX3F6eI19UbLXG.AZX2WCVPwWYrszpj9y4BVdBcJjm6Q4y'),
    ('Carol Example', 'carol@example.com', '$2a$10$0.TD9FMAX3F6eI19UbLXG.AZX2WCVPwWYrszpj9y4BVdBcJjm6Q4y');

INSERT INTO project (name, description, created_at, updated_at) VALUES
    ('Website redesign', 'New layout and copy for the public website.', NOW(), NOW()),
    ('Mobile launch',    'Everything needed to release the mobile app.', NOW(), NOW()),
    ('Research notes',   NULL,                                           NOW(), NOW());

INSERT INTO project_member (project_id, user_id, role)
SELECT p.id, u.id, m.role
FROM (VALUES
    ('Website redesign', 'alice@example.com', 'PROJECT_MANAGER'),
    ('Website redesign', 'bob@example.com',   'MEMBER'),
    ('Mobile launch',    'bob@example.com',   'PROJECT_MANAGER'),
    ('Mobile launch',    'alice@example.com', 'MEMBER'),
    ('Research notes',   'carol@example.com', 'PROJECT_MANAGER')
) AS m (project_name, email, role)
JOIN project p ON p.name = m.project_name
JOIN app_user u ON u.email = m.email;

-- Due dates are relative to the day the seed runs, so one task starts late.
INSERT INTO task (project_id, title, description, due_date, priority, status, created_at, updated_at)
SELECT p.id, t.title, t.description, CURRENT_DATE + t.due_in_days, t.priority, t.status, NOW(), NOW()
FROM (VALUES
    ('Website redesign', 'Audit the current pages',       NULL,                                   -10, 'LOW',    'DONE'),
    ('Website redesign', 'Write the landing page copy',   'Short, plain sentences.',                5, 'HIGH',   'IN_PROGRESS'),
    ('Website redesign', 'Update the homepage layout',    NULL,                                    -3, 'MEDIUM', 'TODO'),
    ('Website redesign', 'Choose a colour palette',       NULL,                                  NULL, 'LOW',    'TODO'),
    ('Mobile launch',    'Prepare the app store listing', 'Screenshots and description.',          10, 'HIGH',   'TODO'),
    ('Mobile launch',    'Set up crash reporting',        NULL,                                     2, 'MEDIUM', 'IN_PROGRESS'),
    ('Mobile launch',    'Plan the beta test',            NULL,                                    -5, 'MEDIUM', 'DONE'),
    ('Research notes',   'Summarise user interviews',     NULL,                                     7, 'MEDIUM', 'IN_PROGRESS')
) AS t (project_name, title, description, due_in_days, priority, status)
JOIN project p ON p.name = t.project_name;

INSERT INTO task_assignee (task_id, user_id, created_at)
SELECT t.id, u.id, NOW()
FROM (VALUES
    ('Audit the current pages',       'alice@example.com'),
    ('Write the landing page copy',   'alice@example.com'),
    ('Write the landing page copy',   'bob@example.com'),
    ('Update the homepage layout',    'bob@example.com'),
    ('Prepare the app store listing', 'alice@example.com'),
    ('Set up crash reporting',        'bob@example.com'),
    ('Plan the beta test',            'bob@example.com'),
    ('Plan the beta test',            'alice@example.com'),
    ('Summarise user interviews',     'carol@example.com')
) AS a (title, email)
JOIN task t ON t.title = a.title
JOIN app_user u ON u.email = a.email;

INSERT INTO comment (task_id, author_id, content, created_at)
SELECT t.id, u.id, c.content, NOW() - (c.minutes_ago * INTERVAL '1 minute')
FROM (VALUES
    ('Write the landing page copy', 'bob@example.com',   'I''ll have a first draft by Friday.',      120),
    ('Write the landing page copy', 'alice@example.com', 'Great, keep it under 200 words.',            60),
    ('Set up crash reporting',      'bob@example.com',   'Waiting for access to the reporting tool.',  30),
    ('Summarise user interviews',   'carol@example.com', 'Five of eight interviews summarised.',       10)
) AS c (title, email, content, minutes_ago)
JOIN task t ON t.title = c.title
JOIN app_user u ON u.email = c.email;
