CREATE USER ${db_user} WITH PASSWORD '${db_user}';

GRANT USAGE ON SCHEMA write to "${db_user}";

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA write TO "${db_user}";
GRANT SELECT, USAGE ON ALL SEQUENCES IN SCHEMA write TO "${db_user}";

ALTER DEFAULT PRIVILEGES IN SCHEMA write GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO "${db_user}";
ALTER DEFAULT PRIVILEGES IN SCHEMA write GRANT SELECT, USAGE ON SEQUENCES TO "${db_user}";

GRANT USAGE ON SCHEMA read to "${db_user}";

GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA read TO "${db_user}";
GRANT SELECT, USAGE ON ALL SEQUENCES IN SCHEMA read TO "${db_user}";

ALTER DEFAULT PRIVILEGES IN SCHEMA read GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO "${db_user}";
ALTER DEFAULT PRIVILEGES IN SCHEMA read GRANT SELECT, USAGE ON SEQUENCES TO "${db_user}";
