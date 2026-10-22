.PHONY: lint test build frontend-deps

ifdef JAVA_HOME_21_X64
export JAVA_HOME := $(JAVA_HOME_21_X64)
endif

frontend-deps:
	cd tools/openapi-bundle && npm ci
	cd frontend && npm ci

lint: frontend-deps
	cd frontend && npm run lint

test: frontend-deps
	cd backend && ./gradlew test
	cd frontend && npm test

build: frontend-deps
	cd backend && ./gradlew build -x test
	cd frontend && npm run build
