#!/bin/bash
set -e

# Create multiple databases
psql -v ON_ERROR_STOP=1 --username "$POSTGRES_USER" <<-EOSQL
    CREATE DATABASE userdb;
    CREATE DATABASE orderdb;
    CREATE DATABASE inventorydb;
    CREATE DATABASE paymentdb;
    CREATE DATABASE notificationdb;
    CREATE DATABASE authdb;
    CREATE DATABASE analyticsdb;
    CREATE DATABASE reportingdb;
    
    -- Grant all privileges on databases
    GRANT ALL PRIVILEGES ON DATABASE userdb TO postgres;
    GRANT ALL PRIVILEGES ON DATABASE orderdb TO postgres;
    GRANT ALL PRIVILEGES ON DATABASE inventorydb TO postgres;
    GRANT ALL PRIVILEGES ON DATABASE paymentdb TO postgres;
    GRANT ALL PRIVILEGES ON DATABASE notificationdb TO postgres;
    GRANT ALL PRIVILEGES ON DATABASE authdb TO postgres;
    GRANT ALL PRIVILEGES ON DATABASE analyticsdb TO postgres;
    GRANT ALL PRIVILEGES ON DATABASE reportingdb TO postgres;
EOSQL