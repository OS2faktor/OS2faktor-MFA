ALTER TABLE clients ADD supports_challenge_flow BOOLEAN NOT NULL DEFAULT 0;
-- these clients already support the new challenge flow, so flag them as such
UPDATE clients SET supports_challenge_flow = 1 WHERE client_type IN ('IOS', 'ANDROID') AND client_version = '3.0.0';
UPDATE clients SET supports_challenge_flow = 1 WHERE client_type = 'FIREFOX';
