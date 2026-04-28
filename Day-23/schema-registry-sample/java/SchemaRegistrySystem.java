import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class SchemaRegistrySystem {
    public static final class SchemaField {
        private final String name;
        private final String fieldType;
        private final boolean required;

        public SchemaField(String name, String fieldType, boolean required) {
            this.name = name;
            this.fieldType = fieldType;
            this.required = required;
        }

        public String getName() {
            return name;
        }

        public String getFieldType() {
            return fieldType;
        }

        public boolean isRequired() {
            return required;
        }
    }

    public static final class SchemaVersion {
        private final int version;
        private final List<SchemaField> fields;

        public SchemaVersion(int version, List<SchemaField> fields) {
            this.version = version;
            this.fields = fields;
        }

        public int getVersion() {
            return version;
        }

        public List<SchemaField> getFields() {
            return fields;
        }
    }

    public static final class SchemaRegistry {
        private final Map<String, List<SchemaVersion>> subjects = new LinkedHashMap<>();

        public SchemaVersion register(String subject, List<SchemaField> fields) {
            if (subject == null || subject.isBlank()) {
                throw new IllegalArgumentException("subject must be non-empty");
            }
            if (fields == null || fields.isEmpty()) {
                throw new IllegalArgumentException("fields must be non-empty");
            }

            List<SchemaVersion> versions = subjects.computeIfAbsent(subject, key -> new ArrayList<>());
            if (!versions.isEmpty()) {
                SchemaVersion previous = versions.get(versions.size() - 1);
                assertBackwardCompatible(previous, fields);
            }

            int nextVersion = versions.size() + 1;
            SchemaVersion schema = new SchemaVersion(nextVersion, new ArrayList<>(fields));
            versions.add(schema);
            return schema;
        }

        public SchemaVersion latest(String subject) {
            List<SchemaVersion> versions = subjects.get(subject);
            if (versions == null || versions.isEmpty()) {
                throw new IllegalArgumentException("unknown subject: " + subject);
            }
            return versions.get(versions.size() - 1);
        }

        public SchemaVersion get(String subject, int version) {
            List<SchemaVersion> versions = subjects.get(subject);
            if (versions == null || version <= 0 || version > versions.size()) {
                throw new IllegalArgumentException("schema version not found: " + subject + " v" + version);
            }
            return versions.get(version - 1);
        }

        private static void assertBackwardCompatible(SchemaVersion previous, List<SchemaField> nextFields) {
            Map<String, SchemaField> nextMap = new LinkedHashMap<>();
            for (SchemaField field : nextFields) {
                nextMap.put(field.getName(), field);
            }

            for (SchemaField oldField : previous.getFields()) {
                SchemaField newField = nextMap.get(oldField.getName());
                if (newField == null) {
                    throw new IllegalArgumentException("incompatible: removed field '" + oldField.getName() + "'");
                }
                if (!newField.getFieldType().equals(oldField.getFieldType())) {
                    throw new IllegalArgumentException(
                        "incompatible: field '"
                            + oldField.getName()
                            + "' type changed "
                            + oldField.getFieldType()
                            + " -> "
                            + newField.getFieldType()
                    );
                }
                if (oldField.isRequired() && !newField.isRequired()) {
                    throw new IllegalArgumentException(
                        "incompatible: required field '" + oldField.getName() + "' became optional"
                    );
                }
            }
        }
    }

    public static final class MessageValidator {
        public static void validate(SchemaVersion schema, Map<String, Object> payload) {
            Map<String, SchemaField> schemaMap = new LinkedHashMap<>();
            for (SchemaField field : schema.getFields()) {
                schemaMap.put(field.getName(), field);
                if (field.isRequired() && !payload.containsKey(field.getName())) {
                    throw new IllegalArgumentException("missing required field: " + field.getName());
                }
            }

            for (Map.Entry<String, Object> entry : payload.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                SchemaField field = schemaMap.get(key);
                if (field == null) {
                    throw new IllegalArgumentException("unknown field: " + key);
                }
                if (!matches(field.getFieldType(), value)) {
                    throw new IllegalArgumentException(
                        "field '"
                            + key
                            + "' type mismatch: expected "
                            + field.getFieldType()
                            + ", got "
                            + value.getClass().getSimpleName()
                    );
                }
            }
        }

        private static boolean matches(String expectedType, Object value) {
            return ("string".equals(expectedType) && value instanceof String)
                || ("int".equals(expectedType) && value instanceof Integer)
                || ("bool".equals(expectedType) && value instanceof Boolean);
        }
    }

    public static final class ProducerSimulator {
        private final SchemaRegistry registry;
        private final String subject;

        public ProducerSimulator(SchemaRegistry registry, String subject) {
            this.registry = registry;
            this.subject = subject;
        }

        public String publish(int version, Map<String, Object> payload) {
            SchemaVersion schema = registry.get(subject, version);
            MessageValidator.validate(schema, payload);
            return "published " + subject + " v" + version + ": " + payload;
        }
    }
}
