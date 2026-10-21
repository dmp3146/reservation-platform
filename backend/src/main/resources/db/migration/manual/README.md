# Service availability database change

The application has no configured Flyway or Liquibase dependency. The SQL file
in this directory is therefore a reviewable, one-time manual migration; Spring
Boot will not execute it automatically. New installations must use the updated
`backend/src/main/resources/db/schema.sql` baseline.

## Change contents

The additive `disponibilidad_servicio` table stores a service, concrete date,
start/end times, and creation timestamp. Its foreign key references
`servicio(id)`, its check constraint requires the end time to be later than the
start time, and its index supports queries by service and date. Existing
`agenda`, `horario_disponible`, and `reserva` tables are unchanged.

Overlapping inserts are rejected by a PostgreSQL trigger. The trigger takes a
write lock on the parent service row before checking the half-open overlap
condition. Concurrent inserts for the same service are consequently
serialized without a PostgreSQL extension. The service layer takes the same
parent-row lock before its early overlap check; the trigger remains the final
database-side guard for other insert paths. SQLSTATE `23P01` is translated to
the API's `409 SCHEDULE_OVERLAP` response.

The trigger function is `SECURITY DEFINER` so its overlap query can see
existing rows when row-level security is enabled. It uses a fixed search path,
fully-qualified table names, and has public function privileges revoked. The
function must be created by a trusted migration role that owns the referenced
tables and function. Do not add permissive RLS policies to make this operation
work.

## Controlled application to an existing database

1. Review the migration SQL and confirm the migration role, schema name, RLS
   behavior, and backend database role.
2. Take a backup and confirm the target is the intended non-production or
   approved database. Do not run the script against Supabase until explicitly
   approved.
3. Apply the manual migration exactly once through the approved PostgreSQL
   administration process. The script is intentionally not idempotent; a
   failure should be investigated rather than hidden by `IF NOT EXISTS`.
4. Verify the table, foreign key, check constraint, index, trigger, function
   owner, and RLS flag in PostgreSQL catalogs.
5. Start the application with `spring.jpa.hibernate.ddl-auto=validate`, then
   run the PostgreSQL integration test using a dedicated test database. Set
   `SCHEDULES_POSTGRES_TEST_URL`, `SCHEDULES_POSTGRES_TEST_USERNAME`, and
   `SCHEDULES_POSTGRES_TEST_PASSWORD` for that test only.
6. Confirm the backend connection role has only the required database access.
   RLS is enabled and this change creates no policies; access must be reviewed
   for the actual service role without disabling RLS or opening policies.

The SQL has not been executed against any database as part of this change.
