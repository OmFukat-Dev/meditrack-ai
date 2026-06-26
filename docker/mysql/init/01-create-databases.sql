CREATE DATABASE IF NOT EXISTS meditrack_db;
CREATE DATABASE IF NOT EXISTS meditrack_ai_prediction;
CREATE DATABASE IF NOT EXISTS meditrack_alert_service;
CREATE DATABASE IF NOT EXISTS meditrack_audit;
CREATE DATABASE IF NOT EXISTS meditrack_user_management;

DROP USER IF EXISTS 'meditrack_user'@'%';
DROP USER IF EXISTS 'meditrack_user'@'127.0.0.1';
DROP USER IF EXISTS 'meditrack_user'@'localhost';

CREATE USER 'meditrack_user'@'%' IDENTIFIED BY 'meditrack_pass';
CREATE USER 'meditrack_user'@'127.0.0.1' IDENTIFIED BY 'meditrack_pass';
CREATE USER 'meditrack_user'@'localhost' IDENTIFIED BY 'meditrack_pass';

GRANT ALL PRIVILEGES ON meditrack_db.* TO 'meditrack_user'@'%';
GRANT ALL PRIVILEGES ON meditrack_ai_prediction.* TO 'meditrack_user'@'%';
GRANT ALL PRIVILEGES ON meditrack_alert_service.* TO 'meditrack_user'@'%';
GRANT ALL PRIVILEGES ON meditrack_audit.* TO 'meditrack_user'@'%';
GRANT ALL PRIVILEGES ON meditrack_user_management.* TO 'meditrack_user'@'%';

GRANT ALL PRIVILEGES ON meditrack_db.* TO 'meditrack_user'@'127.0.0.1';
GRANT ALL PRIVILEGES ON meditrack_ai_prediction.* TO 'meditrack_user'@'127.0.0.1';
GRANT ALL PRIVILEGES ON meditrack_alert_service.* TO 'meditrack_user'@'127.0.0.1';
GRANT ALL PRIVILEGES ON meditrack_audit.* TO 'meditrack_user'@'127.0.0.1';
GRANT ALL PRIVILEGES ON meditrack_user_management.* TO 'meditrack_user'@'127.0.0.1';

GRANT ALL PRIVILEGES ON meditrack_db.* TO 'meditrack_user'@'localhost';
GRANT ALL PRIVILEGES ON meditrack_ai_prediction.* TO 'meditrack_user'@'localhost';
GRANT ALL PRIVILEGES ON meditrack_alert_service.* TO 'meditrack_user'@'localhost';
GRANT ALL PRIVILEGES ON meditrack_audit.* TO 'meditrack_user'@'localhost';
GRANT ALL PRIVILEGES ON meditrack_user_management.* TO 'meditrack_user'@'localhost';

FLUSH PRIVILEGES;
