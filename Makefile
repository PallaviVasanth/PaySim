.PHONY: setup up down db test-backend build-frontend dev-backend dev-frontend

setup:            ## create .env from the template (does not overwrite)
	@test -f .env || cp .env.example .env
up: setup         ## build and run everything in Docker -> http://localhost:3000
	docker compose up --build
down:
	docker compose down
db: setup         ## start only PostgreSQL for local development
	docker compose up -d db
dev-backend:      ## run Spring Boot locally (needs `make db` first)
	set -a; . ./.env; set +a; cd backend && mvn spring-boot:run
dev-frontend:
	cd frontend && npm install && npm run dev
test-backend:     ## unit + integration tests (integration tests need Docker)
	cd backend && mvn -B verify
build-frontend:
	cd frontend && npm ci && npm run build
