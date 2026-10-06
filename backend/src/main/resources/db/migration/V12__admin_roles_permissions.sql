INSERT INTO roles(id,code,name,description,system_role,created_at) VALUES
('01990000-0000-7000-8000-000000000011','CONTENT_EDITOR','Content editor','Draft knowledge and prompts',TRUE,CURRENT_TIMESTAMP),
('01990000-0000-7000-8000-000000000012','CONTENT_APPROVER','Content approver','Review and publish knowledge',TRUE,CURRENT_TIMESTAMP),
('01990000-0000-7000-8000-000000000013','SAFETY_ADMIN','Safety administrator','Manage approved safety policy',TRUE,CURRENT_TIMESTAMP),
('01990000-0000-7000-8000-000000000014','SYSTEM_ADMIN','System administrator','Operate accounts and jobs',TRUE,CURRENT_TIMESTAMP),
('01990000-0000-7000-8000-000000000015','SUPPORT_AGENT','Support agent','Read limited account metadata',TRUE,CURRENT_TIMESTAMP),
('01990000-0000-7000-8000-000000000016','AUDITOR','Auditor','Read sanitized audit metadata',TRUE,CURRENT_TIMESTAMP);

INSERT INTO permissions(id,code,description,created_at) VALUES
('01990000-0000-7000-8000-000000000101','knowledge:write','Create and submit knowledge drafts',CURRENT_TIMESTAMP),
('01990000-0000-7000-8000-000000000102','knowledge:review','Review and publish knowledge',CURRENT_TIMESTAMP),
('01990000-0000-7000-8000-000000000103','safety:manage','Manage approved safety content',CURRENT_TIMESTAMP),
('01990000-0000-7000-8000-000000000104','users:read-metadata','Read limited user metadata',CURRENT_TIMESTAMP),
('01990000-0000-7000-8000-000000000105','users:suspend','Suspend and restore user accounts',CURRENT_TIMESTAMP),
('01990000-0000-7000-8000-000000000106','jobs:read','Read sanitized job metadata',CURRENT_TIMESTAMP),
('01990000-0000-7000-8000-000000000107','jobs:retry','Retry terminal jobs',CURRENT_TIMESTAMP),
('01990000-0000-7000-8000-000000000108','audit:read','Read sanitized audit metadata',CURRENT_TIMESTAMP),
('01990000-0000-7000-8000-000000000109','admin:dashboard','Read cohort-limited aggregate metrics',CURRENT_TIMESTAMP);

INSERT INTO role_permissions(role_id,permission_id,created_at)
SELECT r.id,p.id,CURRENT_TIMESTAMP FROM roles r JOIN permissions p ON
    (r.code='CONTENT_EDITOR' AND p.code='knowledge:write') OR
    (r.code='CONTENT_APPROVER' AND p.code='knowledge:review') OR
    (r.code='SAFETY_ADMIN' AND p.code='safety:manage') OR
    (r.code='SUPPORT_AGENT' AND p.code='users:read-metadata') OR
    (r.code='AUDITOR' AND p.code='audit:read') OR
    (r.code='SYSTEM_ADMIN' AND p.code IN ('users:read-metadata','users:suspend','jobs:read','jobs:retry','admin:dashboard'));
