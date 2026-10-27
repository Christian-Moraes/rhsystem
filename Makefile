.PHONY: up down restart logs build clean ps prod prod-down prod-logs prod-build desktop-package desktop-start desktop-stop

# Desenvolvimento (padrão)
up:
	docker-compose up -d

down:
	docker-compose down

restart:
	docker-compose restart

logs:
	docker-compose logs -f

build:
	docker-compose build

clean:
	docker-compose down -v
	docker system prune -f

ps:
	docker-compose ps

# Produção
prod:
	docker-compose -f docker-compose.prod.yml up -d

prod-down:
	docker-compose -f docker-compose.prod.yml down

prod-logs:
	docker-compose -f docker-compose.prod.yml logs -f

prod-build:
	docker-compose -f docker-compose.prod.yml build

prod-restart:
	docker-compose -f docker-compose.prod.yml restart

# Desktop offline (Tauri + backend local + postgres local)
desktop-package:
	./desktop/package-desktop.sh

desktop-start:
	./desktop/run-desktop-stack.sh

desktop-stop:
	./desktop/stop-desktop-stack.sh
