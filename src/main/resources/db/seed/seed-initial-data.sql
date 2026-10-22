-- ============================================================================
-- Seed inicial: roles y usuario administrador
-- ----------------------------------------------------------------------------
-- Motor: MySQL 8 (script manual, no lo ejecuta la aplicacion).
--
-- Ejecutar DESPUES del primer arranque de la aplicacion, porque Hibernate
-- crea las tablas con spring.jpa.hibernate.ddl-auto=update.
--
-- Es idempotente: se puede ejecutar varias veces sin duplicar registros.
--
-- Usuario administrador inicial:
--   email    : admin@ai901.local
--   password : Admin@123   <- rotar con PUT /user/{id}/password
--
-- Los roles usan UUID fijos para que el administrador los referencie de forma
-- estable. El usuario administrador tiene el rol ADMIN, con acceso a todos los
-- modulos de la API (unico rol habilitado ademas de BOSS para /user y /role).
-- ============================================================================

INSERT INTO roles (id, name) VALUES
    ('11111111-1111-4111-8111-000000000001', 'BOSS'),
    ('11111111-1111-4111-8111-000000000002', 'ADMIN'),
    ('11111111-1111-4111-8111-000000000003', 'BRANCH_MANAGER'),
    ('11111111-1111-4111-8111-000000000004', 'WAREHOUSE_HEAD'),
    ('11111111-1111-4111-8111-000000000005', 'ADMIN_SUPPORT')
ON DUPLICATE KEY UPDATE name = VALUES(name);

INSERT INTO users (id, name, email, password, role_id, branch_id, warehouse_id, active, created_at, updated_at)
VALUES ('22222222-2222-4222-8222-000000000001',
        'Administrador',
        'admin@ai901.local',
        '$2a$10$N0W5aSWtGlanaiDq53Q5y.4Gx0QB0akm2TS7EezwfkOPxZetBITHO',
        '11111111-1111-4111-8111-000000000002',
        NULL,
        NULL,
        b'1',
        NOW(6),
        NOW(6))
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    password = VALUES(password),
    role_id = VALUES(role_id),
    active = VALUES(active),
    updated_at = NOW(6);
