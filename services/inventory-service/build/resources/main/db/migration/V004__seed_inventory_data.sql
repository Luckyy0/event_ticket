-- Seed Inventory records matching Catalog Service Seed Data

-- 1. Summer Music Festival - Show 1
INSERT INTO inventories (id, show_id, ticket_type_id, total_quantity, available_quantity, reserved_quantity, sold_quantity, version, created_at, updated_at)
VALUES
    ('10000000-0000-0000-0000-000000000001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0011', 100, 100, 0, 0, 0, NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000002', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0012', 500, 500, 0, 0, 0, NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000003', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0013', 200, 200, 0, 0, 0, NOW(), NOW());

-- 2. Summer Music Festival - Show 2
INSERT INTO inventories (id, show_id, ticket_type_id, total_quantity, available_quantity, reserved_quantity, sold_quantity, version, created_at, updated_at)
VALUES
    ('10000000-0000-0000-0000-000000000004', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0002', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0021', 100, 100, 0, 0, 0, NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000005', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0002', 'aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0022', 500, 500, 0, 0, 0, NOW(), NOW());

-- 3. Tech Conference 2026 - Show 1
INSERT INTO inventories (id, show_id, ticket_type_id, total_quantity, available_quantity, reserved_quantity, sold_quantity, version, created_at, updated_at)
VALUES
    ('10000000-0000-0000-0000-000000000006', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0001', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0011', 50, 50, 0, 0, 0, NOW(), NOW()),
    ('10000000-0000-0000-0000-000000000007', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0001', 'bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbb0012', 300, 300, 0, 0, 0, NOW(), NOW());

-- 4. Flash Sale Concert - Show 1 (Target for Flash Sale concurrency tests)
INSERT INTO inventories (id, show_id, ticket_type_id, total_quantity, available_quantity, reserved_quantity, sold_quantity, version, created_at, updated_at)
VALUES
    ('10000000-0000-0000-0000-000000000008', 'cccccccc-cccc-cccc-cccc-cccccccc0001', 'cccccccc-cccc-cccc-cccc-cccccccc0002', 100, 100, 0, 0, 0, NOW(), NOW());
