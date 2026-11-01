-- ============================================================================
-- Seed inicial: roles y usuario administrador
-- ----------------------------------------------------------------------------
-- Motor: SQL Server / Azure SQL (script manual, no lo ejecuta la aplicacion).
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

MERGE INTO roles WITH (HOLDLOCK) AS target
USING (VALUES
    ('11111111-1111-4111-8111-000000000001', 'BOSS'),
    ('11111111-1111-4111-8111-000000000002', 'ADMIN'),
    ('11111111-1111-4111-8111-000000000003', 'BRANCH_MANAGER'),
    ('11111111-1111-4111-8111-000000000004', 'WAREHOUSE_HEAD'),
    ('11111111-1111-4111-8111-000000000005', 'ADMIN_SUPPORT')
) AS source (id, name)
ON target.id = source.id
WHEN MATCHED THEN
    UPDATE SET name = source.name
WHEN NOT MATCHED THEN
    INSERT (id, name)
    VALUES (source.id, source.name);

MERGE INTO users WITH (HOLDLOCK) AS target
USING (VALUES
    ('22222222-2222-4222-8222-000000000001',
     'Administrador',
     'admin@ai901.local',
     '$2a$10$N0W5aSWtGlanaiDq53Q5y.4Gx0QB0akm2TS7EezwfkOPxZetBITHO',
     '11111111-1111-4111-8111-000000000002',
     CAST(NULL AS uniqueidentifier),
     CAST(NULL AS uniqueidentifier),
     1)
) AS source (id, name, email, password, role_id, branch_id, warehouse_id, active)
ON target.id = source.id
WHEN MATCHED THEN
    UPDATE SET
        name = source.name,
        password = source.password,
        role_id = source.role_id,
        active = source.active,
        updated_at = SYSDATETIME()
WHEN NOT MATCHED THEN
    INSERT (id, name, email, password, role_id, branch_id, warehouse_id, active, created_at, updated_at)
    VALUES (source.id, source.name, source.email, source.password, source.role_id, source.branch_id, source.warehouse_id, source.active, SYSDATETIME(), SYSDATETIME());
