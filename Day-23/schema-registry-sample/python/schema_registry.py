from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class SchemaField:
    name: str
    field_type: str
    required: bool


@dataclass(frozen=True)
class SchemaVersion:
    version: int
    fields: list[SchemaField]


class SchemaRegistry:
    def __init__(self) -> None:
        self._subjects: dict[str, list[SchemaVersion]] = {}

    def register(self, subject: str, fields: list[SchemaField]) -> SchemaVersion:
        if not subject:
            raise ValueError("subject must be non-empty")
        if not fields:
            raise ValueError("fields must be non-empty")

        versions = self._subjects.setdefault(subject, [])
        if versions:
            previous = versions[-1]
            self._assert_backward_compatible(previous, fields)

        next_version = len(versions) + 1
        schema = SchemaVersion(version=next_version, fields=fields[:])
        versions.append(schema)
        return schema

    def latest(self, subject: str) -> SchemaVersion:
        versions = self._subjects.get(subject)
        if not versions:
            raise ValueError(f"unknown subject: {subject}")
        return versions[-1]

    def get(self, subject: str, version: int) -> SchemaVersion:
        versions = self._subjects.get(subject)
        if not versions or version <= 0 or version > len(versions):
            raise ValueError(f"schema version not found: {subject} v{version}")
        return versions[version - 1]

    @staticmethod
    def _assert_backward_compatible(previous: SchemaVersion, next_fields: list[SchemaField]) -> None:
        next_map = {field.name: field for field in next_fields}
        for old_field in previous.fields:
            new_field = next_map.get(old_field.name)
            if new_field is None:
                raise ValueError(f"incompatible: removed field '{old_field.name}'")
            if new_field.field_type != old_field.field_type:
                raise ValueError(
                    f"incompatible: field '{old_field.name}' type changed "
                    f"{old_field.field_type} -> {new_field.field_type}"
                )
            if old_field.required and not new_field.required:
                raise ValueError(
                    f"incompatible: required field '{old_field.name}' became optional"
                )


class MessageValidator:
    @staticmethod
    def validate(schema: SchemaVersion, payload: dict[str, Any]) -> None:
        schema_map = {field.name: field for field in schema.fields}

        for field in schema.fields:
            if field.required and field.name not in payload:
                raise ValueError(f"missing required field: {field.name}")

        for key, value in payload.items():
            field = schema_map.get(key)
            if field is None:
                raise ValueError(f"unknown field: {key}")
            if not MessageValidator._matches(field.field_type, value):
                raise ValueError(
                    f"field '{key}' type mismatch: expected {field.field_type}, got {type(value).__name__}"
                )

    @staticmethod
    def _matches(expected_type: str, value: Any) -> bool:
        return (
            (expected_type == "string" and isinstance(value, str))
            or (expected_type == "int" and isinstance(value, int))
            or (expected_type == "bool" and isinstance(value, bool))
        )


class ProducerSimulator:
    def __init__(self, registry: SchemaRegistry, subject: str) -> None:
        self._registry = registry
        self._subject = subject

    def publish(self, version: int, payload: dict[str, Any]) -> str:
        schema = self._registry.get(self._subject, version)
        MessageValidator.validate(schema, payload)
        return f"published {self._subject} v{version}: {payload}"
