"""A parser for the strict YAML subset of the roles and settings files.

The standard library has no YAML parser, so these files are limited to:
- comments (`# ...`) and blank lines;
- top-level `key: scalar`;
- top-level `key:` followed by indented `- scalar` items;
- top-level `key: []` or `key: [a, b]` (flow list of scalars).
Scalars are plain words or single- or double-quoted strings. Integers stay strings;
callers convert. Anything else is an error naming the line.
"""

import re

_KEY = re.compile(r"^([A-Za-z_][A-Za-z0-9_-]*):(?:\s+(.*))?$")
_ITEM = re.compile(r"^\s+-\s+(.*)$")


class YamlSubsetError(ValueError):
    def __init__(self, line, message):
        super().__init__(f"line {line}: {message}")
        self.line = line


def _strip_comment(text):
    out, quote = [], None
    for i, ch in enumerate(text):
        if quote:
            if ch == quote:
                quote = None
        elif ch in "'\"":
            quote = ch
        elif ch == "#" and (i == 0 or text[i - 1].isspace()):
            break
        out.append(ch)
    return "".join(out).rstrip()


def _scalar(text, line):
    text = text.strip()
    if not text:
        raise YamlSubsetError(line, "empty value")
    if text[0] in "'\"":
        if len(text) < 2 or text[-1] != text[0]:
            raise YamlSubsetError(line, "unterminated quoted string")
        inner = text[1:-1]
        if text[0] in inner:
            raise YamlSubsetError(line, "quotes inside a quoted string are not supported")
        return inner
    if text[0] in "[]{}&*!|>%@`" or ": " in text or text.endswith(":"):
        raise YamlSubsetError(line, f"unsupported syntax: {text!r}")
    return text


def parse(text):
    """Parse the subset into a dict of str -> str | list[str]."""
    result = {}
    current = None  # key whose block list is being read
    for number, raw in enumerate(text.splitlines(), start=1):
        if "\t" in raw[: len(raw) - len(raw.lstrip())]:
            raise YamlSubsetError(number, "tabs are not allowed for indentation")
        line = _strip_comment(raw)
        if not line.strip():
            continue
        if line[0].isspace():
            item = _ITEM.match(line)
            if not item or current is None:
                raise YamlSubsetError(number, "only '- item' lines may be indented, under a 'key:'")
            result[current].append(_scalar(item.group(1), number))
            continue
        match = _KEY.match(line)
        if not match:
            raise YamlSubsetError(number, f"expected 'key: value', got {line.strip()!r}")
        key, value = match.group(1), (match.group(2) or "").strip()
        if key in result:
            raise YamlSubsetError(number, f"duplicate key {key!r}")
        current = None
        if not value:
            result[key] = []
            current = key
        elif value.startswith("["):
            if not value.endswith("]"):
                raise YamlSubsetError(number, "unterminated flow list")
            inner = value[1:-1].strip()
            result[key] = [_scalar(v, number) for v in inner.split(",")] if inner else []
        else:
            result[key] = _scalar(value, number)
    return result
