FROM debian:bookworm-slim

# Optional toolsets; the base (Claude Code, Node, openspec, gh, git, ssh) is always included
ARG WITH_JAVA=true
ARG WITH_E2E=true

# Keep in sync with frontend/.nvmrc and @playwright/test in frontend/package.json
ARG NODE_MAJOR=24
ARG PLAYWRIGHT_VERSION=1.64.1
ARG OPENSPEC_VERSION=1.14.2

ENV DEBIAN_FRONTEND=noninteractive \
    LANG=C.UTF-8 \
    PLAYWRIGHT_BROWSERS_PATH=/ms-playwright

RUN case "$WITH_JAVA:$WITH_E2E" in \
        true:true|true:false|false:false) ;; \
        false:true) echo "ERROR: WITH_E2E=true requires WITH_JAVA=true (E2E runs against the local backend)" >&2; exit 1 ;; \
        *) echo "ERROR: WITH_JAVA and WITH_E2E must be true or false" >&2; exit 1 ;; \
    esac

RUN apt-get update && apt-get install -y --no-install-recommends \
        ca-certificates curl wget gnupg git openssh-client jq lsof procps psmisc \
        python3 unzip xz-utils less vim-tiny gosu \
    && install -d -m 0755 /etc/apt/keyrings \
    && curl -fsSL https://cli.github.com/packages/githubcli-archive-keyring.gpg \
        -o /etc/apt/keyrings/githubcli.gpg \
    && echo "deb [signed-by=/etc/apt/keyrings/githubcli.gpg] https://cli.github.com/packages stable main" \
        > /etc/apt/sources.list.d/github-cli.list \
    && apt-get update && apt-get install -y --no-install-recommends gh \
    && rm -rf /var/lib/apt/lists/*

# Node is part of the base: openspec, plugin MCP servers (npx) and plugin scripts need it
RUN curl -fsSLO "https://nodejs.org/dist/latest-v${NODE_MAJOR}.x/SHASUMS256.txt" \
    && NODE_DIST=$(grep -oE "node-v[0-9.]+-linux-x64\.tar\.xz" SHASUMS256.txt | head -1) \
    && curl -fsSLO "https://nodejs.org/dist/latest-v${NODE_MAJOR}.x/${NODE_DIST}" \
    && grep " ${NODE_DIST}\$" SHASUMS256.txt | sha256sum -c - \
    && tar -xJf "${NODE_DIST}" -C /usr/local --strip-components=1 --exclude='*.md' --exclude=LICENSE \
    && rm "${NODE_DIST}" SHASUMS256.txt \
    && npm install -g "@fission-ai/openspec@${OPENSPEC_VERSION}" \
    && npm cache clean --force

RUN if [ "$WITH_JAVA" = true ]; then \
        curl -fsSL https://packages.adoptium.net/artifactory/api/gpg/key/public \
            | gpg --dearmor -o /etc/apt/keyrings/adoptium.gpg \
        && echo "deb [signed-by=/etc/apt/keyrings/adoptium.gpg] https://packages.adoptium.net/artifactory/deb bookworm main" \
            > /etc/apt/sources.list.d/adoptium.list \
        && apt-get update && apt-get install -y --no-install-recommends temurin-21-jdk \
        && rm -rf /var/lib/apt/lists/*; \
    fi

# chromium serves `npm run test:e2e`, chrome serves the Playwright MCP plugin.
# World-writable so Claude can install a newer revision after a Playwright bump.
RUN if [ "$WITH_E2E" = true ]; then \
        npx -y "playwright@${PLAYWRIGHT_VERSION}" install --with-deps chromium chrome \
        && chmod -R a+rwX "${PLAYWRIGHT_BROWSERS_PATH}" \
        && rm -rf /var/lib/apt/lists/* /root/.npm; \
    fi

ENV WITH_JAVA=${WITH_JAVA} \
    WITH_E2E=${WITH_E2E} \
    PLAYWRIGHT_MCP_BROWSER=chrome \
    PLAYWRIGHT_MCP_HEADLESS=true \
    PLAYWRIGHT_MCP_NO_SANDBOX=true

# UID/GID are remapped at startup to the owner of the mounted repository (entrypoint.sh)
RUN groupadd --gid 1000 dev \
    && useradd --uid 1000 --gid dev --create-home --shell /bin/bash dev \
    && install -d -o dev -g dev /mnt/ramdisk

COPY bin/ /usr/local/bin/
COPY seed/ /opt/claude-dev/seed/
COPY entrypoint.sh /opt/claude-dev/
RUN chmod +x /usr/local/bin/* /opt/claude-dev/entrypoint.sh /opt/claude-dev/seed/hooks/*

ENV PATH=/home/dev/.local/bin:${PATH}

ENTRYPOINT ["/opt/claude-dev/entrypoint.sh"]
CMD ["rc"]
