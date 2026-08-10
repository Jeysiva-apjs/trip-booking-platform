-- Runs once, on an empty data volume, via MySQL's /docker-entrypoint-initdb.d hook.
--
-- Two schemas with two separate accounts: neither service can read the other's tables, which is what
-- keeps them independently deployable. Matches the manual setup in DEVELOPMENT.md.
CREATE DATABASE IF NOT EXISTS booking CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'booking'@'%' IDENTIFIED BY 'booking';
GRANT ALL PRIVILEGES ON booking.* TO 'booking'@'%';

CREATE DATABASE IF NOT EXISTS payment CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'payment'@'%' IDENTIFIED BY 'payment';
GRANT ALL PRIVILEGES ON payment.* TO 'payment'@'%';

FLUSH PRIVILEGES;
