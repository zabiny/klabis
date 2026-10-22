.PHONY: lint test build

ifdef JAVA_HOME_21_X64
export JAVA_HOME := $(JAVA_HOME_21_X64)
endif

lint:
	cd frontend && npm ci && npm run lint

test:
	cd backend && ./gradlew test
	cd frontend && npm ci && npm test

build:
	cd backend && ./gradlew build -x test
	cd frontend && npm ci && npm run build
