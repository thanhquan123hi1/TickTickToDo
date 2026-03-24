package hcmute.edu.vn.ticktick.schoolcalendar;

import androidx.annotation.NonNull;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import hcmute.edu.vn.ticktick.database.SchoolCalendarEventEntity;

public class IcsParser {

    private static final DateTimeFormatter DATE_TIME_BASIC =
            DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss", Locale.US);
    private static final DateTimeFormatter DATE_BASIC =
            DateTimeFormatter.BASIC_ISO_DATE;

    @NonNull
    public List<SchoolCalendarEventEntity> parse(String icsContent, String sourceUrl, long syncedAtMillis) {
        List<SchoolCalendarEventEntity> events = new ArrayList<>();
        if (icsContent == null || icsContent.trim().isEmpty()) {
            return events;
        }

        List<String> lines = unfoldLines(icsContent);
        boolean inEvent = false;
        Map<String, FieldValue> fields = new LinkedHashMap<>();

        for (String line : lines) {
            if ("BEGIN:VEVENT".equals(line)) {
                inEvent = true;
                fields.clear();
                continue;
            }
            if ("END:VEVENT".equals(line)) {
                if (inEvent) {
                    SchoolCalendarEventEntity entity = mapEvent(fields, sourceUrl, syncedAtMillis);
                    if (entity != null) {
                        events.add(entity);
                    }
                }
                inEvent = false;
                fields.clear();
                continue;
            }
            if (!inEvent) {
                continue;
            }

            ParsedLine parsed = parseLine(line);
            if (parsed == null) {
                continue;
            }
            fields.put(parsed.name, new FieldValue(parsed.value, parsed.params));
        }

        return events;
    }

    private SchoolCalendarEventEntity mapEvent(Map<String, FieldValue> fields, String sourceUrl, long syncedAtMillis) {
        FieldValue uidField = fields.get("UID");
        if (uidField == null || uidField.value == null || uidField.value.trim().isEmpty()) {
            return null;
        }

        Long startMillis = parseDateTime(fields.get("DTSTART"));
        Long endMillis = parseDateTime(fields.get("DTEND"));
        if (startMillis == null && endMillis == null) {
            return null;
        }

        long normalizedStart = startMillis != null ? startMillis : endMillis;
        long normalizedEnd = endMillis != null ? endMillis : normalizedStart;

        SchoolCalendarEventEntity entity = new SchoolCalendarEventEntity();
        entity.setUid(uidField.value.trim());
        entity.setTitle(normalizeTitle(fields.get("SUMMARY")));
        entity.setDescription(unescapeText(getValue(fields.get("DESCRIPTION"))));
        entity.setCategory(unescapeText(getValue(fields.get("CATEGORIES"))));
        entity.setStartTimeMillis(normalizedStart);
        entity.setEndTimeMillis(normalizedEnd);
        entity.setReferenceTimeMillis(normalizedEnd > 0 ? normalizedEnd : normalizedStart);
        entity.setLastModifiedMillis(parseDateTime(fields.get("LAST-MODIFIED")) == null
                ? 0L
                : parseDateTime(fields.get("LAST-MODIFIED")));
        entity.setSyncedAtMillis(syncedAtMillis);
        entity.setSourceUrl(sourceUrl);
        return entity;
    }

    private String normalizeTitle(FieldValue summaryField) {
        String summary = unescapeText(getValue(summaryField));
        if (summary == null || summary.trim().isEmpty()) {
            return "(No title)";
        }
        return summary.trim();
    }

    private String getValue(FieldValue value) {
        return value == null ? null : value.value;
    }

    private Long parseDateTime(FieldValue field) {
        if (field == null || field.value == null || field.value.trim().isEmpty()) {
            return null;
        }

        String raw = field.value.trim();
        Map<String, String> params = field.params;

        try {
            if ("DATE".equalsIgnoreCase(params.get("VALUE"))) {
                LocalDate date = LocalDate.parse(raw, DATE_BASIC);
                return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }

            if (raw.endsWith("Z")) {
                LocalDateTime utcDateTime = LocalDateTime.parse(raw.substring(0, raw.length() - 1), DATE_TIME_BASIC);
                return utcDateTime.toInstant(ZoneOffset.UTC).toEpochMilli();
            }

            String tzId = params.get("TZID");
            ZoneId zoneId = tzId != null && !tzId.isEmpty() ? ZoneId.of(tzId) : ZoneId.systemDefault();
            LocalDateTime dateTime = LocalDateTime.parse(raw, DATE_TIME_BASIC);
            ZonedDateTime zoned = dateTime.atZone(zoneId);
            return zoned.toInstant().toEpochMilli();
        } catch (Exception ignore) {
            return null;
        }
    }

    private List<String> unfoldLines(String text) {
        String[] rawLines = text.replace("\r\n", "\n").replace('\r', '\n').split("\n");
        List<String> lines = new ArrayList<>();
        StringBuilder current = new StringBuilder();

        for (String rawLine : rawLines) {
            if (rawLine.startsWith(" ") || rawLine.startsWith("\t")) {
                current.append(rawLine.substring(1));
            } else {
                if (current.length() > 0) {
                    lines.add(current.toString());
                }
                current = new StringBuilder(rawLine);
            }
        }

        if (current.length() > 0) {
            lines.add(current.toString());
        }
        return lines;
    }

    private ParsedLine parseLine(String line) {
        int colonIndex = line.indexOf(':');
        if (colonIndex <= 0) {
            return null;
        }

        String left = line.substring(0, colonIndex);
        String value = colonIndex == line.length() - 1 ? "" : line.substring(colonIndex + 1);

        String[] segments = left.split(";");
        String name = segments[0].trim().toUpperCase(Locale.US);
        Map<String, String> params = new LinkedHashMap<>();

        for (int i = 1; i < segments.length; i++) {
            String segment = segments[i];
            int eq = segment.indexOf('=');
            if (eq > 0 && eq < segment.length() - 1) {
                String key = segment.substring(0, eq).trim().toUpperCase(Locale.US);
                String paramValue = segment.substring(eq + 1).trim();
                params.put(key, paramValue);
            }
        }

        return new ParsedLine(name, value, params);
    }

    private String unescapeText(String raw) {
        if (raw == null) {
            return null;
        }
        return raw
                .replace("\\n", "\n")
                .replace("\\N", "\n")
                .replace("\\,", ",")
                .replace("\\;", ";")
                .replace("\\\\", "\\");
    }

    private static class ParsedLine {
        final String name;
        final String value;
        final Map<String, String> params;

        ParsedLine(String name, String value, Map<String, String> params) {
            this.name = name;
            this.value = value;
            this.params = params;
        }
    }

    private static class FieldValue {
        final String value;
        final Map<String, String> params;

        FieldValue(String value, Map<String, String> params) {
            this.value = value;
            this.params = params;
        }
    }
}
