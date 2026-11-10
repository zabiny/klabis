"""The roles file: who is a Stakeholder, a Developer or an agent account.

Shared by the CLI and the server-side automation. The install copies this module,
`yamlsubset.py` and `naming.py` unchanged into the target repository's
`.github/sdlc/`, where they are imported as top-level modules. Keep it standard-library
only and import nothing from `sdlc` but `yamlsubset`.
"""

from dataclasses import dataclass, field
from typing import List

if __package__:
    from . import yamlsubset
else:  # vendored copy in .github/sdlc/
    import yamlsubset

ROLES_PATH = ".github/sdlc-roles.yml"
ROLE_KEYS = ("stakeholders", "developers", "agents")


class RolesError(ValueError):
    """The roles file is outside the supported subset. `line` is set when one line is at fault."""

    def __init__(self, message, line=None):
        super().__init__(message)
        self.line = line


@dataclass
class Roles:
    stakeholders: List[str] = field(default_factory=list)
    developers: List[str] = field(default_factory=list)
    agents: List[str] = field(default_factory=list)

    @staticmethod
    def _has(accounts, login):
        return bool(login) and login.lower() in {a.lower() for a in accounts}

    def is_stakeholder(self, login):
        return self._has(self.stakeholders, login)

    def is_developer(self, login):
        return self._has(self.developers, login)

    def is_agent(self, login):
        return self._has(self.agents, login)

    def is_role_holder(self, login):
        """Stakeholder or Developer: the people who own gates."""
        return self.is_stakeholder(login) or self.is_developer(login)

    def roles_of(self, login):
        names = []
        if self.is_stakeholder(login):
            names.append("stakeholder")
        if self.is_developer(login):
            names.append("developer")
        if self.is_agent(login):
            names.append("agent")
        return names

    def accounts_for(self, role):
        return list({"stakeholders": self.stakeholders, "developers": self.developers}[role])


def parse_roles(text):
    """Parse the roles file. Supported: the keys `stakeholders`, `developers` and `agents`,
    each a list of GitHub logins; anything else raises RolesError. An empty or missing list
    is allowed here (callers decide whether an agent must be listed), but an account listed
    both as an agent and as a Stakeholder or Developer is not."""
    try:
        data = yamlsubset.parse(text)
    except yamlsubset.YamlSubsetError as exc:
        raise RolesError(str(exc), line=exc.line) from None
    unknown = sorted(set(data) - set(ROLE_KEYS))
    if unknown:
        raise RolesError(f"unknown key {unknown[0]!r}; only {', '.join(ROLE_KEYS)} are supported")
    roles = Roles()
    for key in ROLE_KEYS:
        value = data.get(key, [])
        if not isinstance(value, list):
            raise RolesError(f"{key} must be a list of logins")
        for login in value:
            if not login or login.startswith("@") or any(c.isspace() for c in login):
                raise RolesError(f"{key}: {login!r} is not a GitHub login (no @, no spaces)")
        setattr(roles, key, value)
    for agent in roles.agents:
        human = [r for r in roles.roles_of(agent) if r != "agent"]
        if human:
            raise RolesError(f"agent account {agent} is also listed as a {' and '.join(human)};"
                             " the agent account cannot hold a human role")
    return roles
