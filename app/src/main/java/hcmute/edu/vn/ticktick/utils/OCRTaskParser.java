package hcmute.edu.vn.ticktick.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OCRTaskParser {

    public static class TaskDraft {
        public String title;
        public String description;
        public Long dueDate;
        public String dueTime;

        public TaskDraft(String title, String description, Long dueDate, String dueTime) {
            this.title = title;
            this.description = description;
            this.dueDate = dueDate;
            this.dueTime = dueTime;
        }
    }

    public static List<TaskDraft> parseText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) return new ArrayList<>();

        String[] lines = rawText.split("\n");
        List<String> cleanedLines = new ArrayList<>();
        for (String line : lines) {
            if (!line.trim().isEmpty()) {
                cleanedLines.add(line.trim());
            }
        }

        if (cleanedLines.isEmpty()) return new ArrayList<>();

        List<TaskDraft> drafts = new ArrayList<>();

        // Detect bullet patterns
        Pattern bulletPattern = Pattern.compile("^([-•*]|\\d+\\.|(?:\\[[ xX]?\\]))\\s+(.*)");
        // Detect date prefix pattern
        Pattern datePrefixPattern = Pattern.compile("^(\\d{1,2}[-/.\\s]+\\d{1,2}(?:[-/.\\s]+\\d{2,4})?)\\s*[-:]?\\s+(.*)");

        int parseableDates = 0;
        int bullets = 0;

        for (String line : cleanedLines) {
            if (bulletPattern.matcher(line).find()) bullets++;
            if (datePrefixPattern.matcher(line).find() || VietnameseDateTimeParser.parse(line) != null) {
                parseableDates++;
            }
        }

        boolean isMultiTask = bullets > 1 || parseableDates > 1;

        if (isMultiTask) {
            String commonContext = "";
            for (String line : cleanedLines) {
                Matcher bMatcher = bulletPattern.matcher(line);
                Matcher dMatcher = datePrefixPattern.matcher(line);

                boolean looksLikeTask = false;
                String taskTitle = line;

                if (bMatcher.find()) {
                    taskTitle = bMatcher.group(2);
                    looksLikeTask = true;
                } else if (dMatcher.find()) {
                    taskTitle = line;
                    looksLikeTask = true;
                } else if (VietnameseDateTimeParser.parse(line) != null) {
                    looksLikeTask = true;
                }

                if (looksLikeTask) {
                    VietnameseDateTimeParser.ParsedResult parsed = VietnameseDateTimeParser.parse(taskTitle);
                    Long dDate = null;
                    String dTime = null;
                    if (parsed != null) {
                        dDate = parsed.dueDate;
                        dTime = parsed.dueTime;
                    }

                    String fTitle = taskTitle;
                    if (!commonContext.isEmpty()) {
                        fTitle = commonContext + " - " + taskTitle;
                    }

                    drafts.add(new TaskDraft(fTitle, "", dDate, dTime));
                } else {
                    if (commonContext.isEmpty()) {
                        commonContext = line;
                    } else {
                        commonContext += " " + line;
                    }
                }
            }
        }

        if (drafts.isEmpty()) {
            String title = cleanedLines.get(0);
            StringBuilder desc = new StringBuilder();
            for (int i = 1; i < cleanedLines.size(); i++) {
                desc.append(cleanedLines.get(i)).append("\n");
            }

            VietnameseDateTimeParser.ParsedResult parsed = VietnameseDateTimeParser.parse(rawText);
            Long dDate = null;
            String dTime = null;
            if (parsed != null) {
                dDate = parsed.dueDate;
                dTime = parsed.dueTime;
            }
            drafts.add(new TaskDraft(title, desc.toString().trim(), dDate, dTime));
        }

        return drafts;
    }
}
