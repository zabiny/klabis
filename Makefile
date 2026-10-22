# SDLC placeholder targets, added by the agentic-sdlc install skill. CI runs lint, test
# and build; it stays red until each placeholder is replaced by the real command.
.PHONY: lint test build

lint:
	@echo "SDLC: lint is not configured" >&2; exit 1

test:
	@echo "SDLC: test is not configured" >&2; exit 1

build:
	@echo "SDLC: build is not configured" >&2; exit 1
