#!/usr/bin/env bash
# Runs as root: aligns the dev user with the owner of the mounted repository, then drops privileges.
set -euo pipefail

: "${KLABIS_DIR:?KLABIS_DIR must point to the mounted klabis repository}"
[[ -e "$KLABIS_DIR/.git" ]] || { echo "ERROR: $KLABIS_DIR is not a git repository (is it mounted?)" >&2; exit 1; }

repo_uid=$(stat -c %u "$KLABIS_DIR")
repo_gid=$(stat -c %g "$KLABIS_DIR")
if [[ "$repo_uid" == 0 || "$repo_gid" == 0 ]]; then
    echo "ERROR: $KLABIS_DIR is owned by root; mount a repository owned by a regular user and group" >&2
    exit 1
fi

if [[ "$(id -g dev)" != "$repo_gid" ]]; then
    existing_group=$(getent group "$repo_gid" | cut -d: -f1 || true)
    if [[ -n "$existing_group" && "$existing_group" != dev ]]; then
        usermod -g "$repo_gid" dev
    else
        groupmod -g "$repo_gid" dev
    fi
fi
[[ "$(id -u dev)" == "$repo_uid" ]] || usermod -u "$repo_uid" -o dev

# Named volume keeps ownership from the previous UID; fix it only when it differs
if [[ "$(stat -c %u:%g /home/dev)" != "$repo_uid:$repo_gid" ]]; then
    chown -R "$repo_uid:$repo_gid" /home/dev
fi
install -d -o "$repo_uid" -g "$repo_gid" /mnt/ramdisk /mnt/ramdisk/klabis /mnt/ramdisk/claude

exec gosu dev claude-dev "$@"
