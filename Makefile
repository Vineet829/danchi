build:
	@mkdir -p client/node_modules && { [ "$$(id -u)" != 0 ] || chown "$$(stat -c %u:%g client 2>/dev/null || stat -f %u:%g client)" client/node_modules; }
	docker compose -f local.yml up --build -d --remove-orphans

up:
	@mkdir -p client/node_modules && { [ "$$(id -u)" != 0 ] || chown "$$(stat -c %u:%g client 2>/dev/null || stat -f %u:%g client)" client/node_modules; }
	docker compose -f local.yml up -d

down:
	docker compose -f local.yml down

down-v:
	docker compose -f local.yml down -v

show-logs:
	docker compose -f local.yml logs

show-logs-api:
	docker compose -f local.yml logs api

superuser:
	docker compose -f local.yml run --rm --no-deps --entrypoint java api \
		-Dloader.main=com.danchi.admin.CreateAdminCommand -cp /app/app.jar \
		org.springframework.boot.loader.launch.PropertiesLauncher $(EMAIL) $(USERNAME)

db-volume:
	docker volume inspect $$(docker compose -f local.yml volumes --format '{{.Name}}' postgres)

mailpit-volume:
	docker volume inspect $$(docker compose -f local.yml volumes --format '{{.Name}}' mailpit)

estate-db:
	docker compose -f local.yml exec postgres sh -c 'psql -U "$$POSTGRES_USER" -d "$$POSTGRES_DB"'
