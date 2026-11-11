@echo off
setlocal
cd /d "%~dp0"

if not exist .env (
  echo ERROR: .env not found in %CD%
  echo Run:  copy .env.example .env
  exit /b 1
)

rem Load every KEY=VALUE line from .env (lines starting with # are skipped)
for /f "usebackq eol=# tokens=1,* delims==" %%A in (".env") do set "%%A=%%B"

rem Ask Docker which host port the database is published on
set DBPORT=
for /f "tokens=2 delims=:" %%P in ('docker compose port db 5432') do set DBPORT=%%P
if "%DBPORT%"=="" (
  echo ERROR: database container is not running.
  echo Run:  docker compose up -d db
  exit /b 1
)

set "PAYSIM_DB_URL=jdbc:postgresql://127.0.0.1:%DBPORT%/paysim"
echo Using database %PAYSIM_DB_URL% as user %PAYSIM_DB_USER%

cd backend
mvn spring-boot:run
