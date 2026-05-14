-- New accounts start with 3 scan credits (was 5).
ALTER TABLE users ALTER COLUMN scan_credits SET DEFAULT 3;
