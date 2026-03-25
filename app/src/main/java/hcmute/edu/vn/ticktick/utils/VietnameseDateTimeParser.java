package hcmute.edu.vn.ticktick.utils;

import java.text.Normalizer;
import java.util.Calendar;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VietnameseDateTimeParser {

    public static class ParsedResult {
        public Long dueDate; // midnight timestamp
        public String dueTime; // "HH:mm"
    }

    private static String removeAccents(String s) {
        if (s == null) return "";
        String normalized = Normalizer.normalize(s, Normalizer.Form.NFD);
        return normalized.replaceAll("\\p{InCombiningDiacriticalMarks}+", "").replaceAll("đ", "d").replaceAll("Đ", "D");
    }

    public static ParsedResult parse(String text) {
        if (text == null || text.trim().isEmpty()) return null;

        String[] lines = text.split("\n");
        ParsedResult bestResult = null;

        for (String line : lines) {
            ParsedResult attempt = parseLine(line);
            if (attempt != null) {
                // Return the first confident line
                return attempt;
            }
        }
        return null;
    }

    private static ParsedResult parseLine(String rawLine) {
        String originalText = rawLine.trim().toLowerCase(Locale.ROOT);
        String noAccentText = removeAccents(originalText);

        // Pre-normalize OCR mistakes
        noAccentText = noAccentText.replace("l8h", "18h")
                                   .replace("i5/", "15/")
                                   .replace("i5-", "15-")
                                   .replace("1s/", "15/")
                                   .replace("1s-", "15-")
                                   .replace("2o/", "20/")
                                   .replace("2o-", "20-");

        noAccentText = noAccentText.replaceAll("\\bthu(\\d)\\b", "thu $1");

        ParsedResult result = new ParsedResult();
        boolean foundDate = false;
        boolean foundTime = false;

        Calendar now = Calendar.getInstance();
        int currentYear = now.get(Calendar.YEAR);

        Calendar targetDate = Calendar.getInstance();
        targetDate.set(Calendar.HOUR_OF_DAY, 0);
        targetDate.set(Calendar.MINUTE, 0);
        targetDate.set(Calendar.SECOND, 0);
        targetDate.set(Calendar.MILLISECOND, 0);

        // 1. Detect Abs Date
        // Matches dd/mm/yyyy, dd-mm-yyyy, dd.mm.yyyy, dd/mm, etc.
        Pattern absDatePattern = Pattern.compile("\\b(\\d{1,2})[-/.](\\d{1,2})(?:[-/.](\\d{2,4}))?\\b");
        Matcher mDate = absDatePattern.matcher(noAccentText);
        if (mDate.find()) {
            int d = Integer.parseInt(mDate.group(1));
            int m = Integer.parseInt(mDate.group(2)) - 1;
            int y = currentYear;
            if (mDate.group(3) != null) {
                y = Integer.parseInt(mDate.group(3));
                if (y < 100) y += 2000;
            }
            if (d >= 1 && d <= 31 && m >= 0 && m <= 11) {
                targetDate.set(Calendar.YEAR, y);
                targetDate.set(Calendar.MONTH, m);
                targetDate.set(Calendar.DAY_OF_MONTH, d);
                // If it's passed this year, move to next (only if no year provided)
                if (mDate.group(3) == null && targetDate.before(now)) {
                    targetDate.add(Calendar.YEAR, 1);
                }
                foundDate = true;
            }
        } else {
            // Check yyyy-mm-dd
            Pattern isoDatePattern = Pattern.compile("\\b(\\d{4})[-/.](\\d{1,2})[-/.](\\d{1,2})\\b");
            Matcher mIso = isoDatePattern.matcher(noAccentText);
            if (mIso.find()) {
                int y = Integer.parseInt(mIso.group(1));
                int m = Integer.parseInt(mIso.group(2)) - 1;
                int d = Integer.parseInt(mIso.group(3));
                if (d >= 1 && d <= 31 && m >= 0 && m <= 11) {
                    targetDate.set(Calendar.YEAR, y);
                    targetDate.set(Calendar.MONTH, m);
                    targetDate.set(Calendar.DAY_OF_MONTH, d);
                    foundDate = true;
                }
            }
        }

        // 2. Detect Abs Time
        // Matches 18:30, 18h30, 18h, 7pm, 07:15 AM
        Pattern absTimePattern = Pattern.compile("\\b(\\d{1,2})(?:[:h](\\d{2}))?\\s*(am|pm)?\\b");
        Matcher mTime = absTimePattern.matcher(noAccentText);
        // We need to avoid matching generic numbers like days.
        // Usually time has am/pm or h or :. Let's make it stricter:
        Pattern strictTimePattern = Pattern.compile("\\b(\\d{1,2})(?:[:h](\\d{2})|\\s*([aApP][mM])|h)\\b");
        Matcher msTime = strictTimePattern.matcher(noAccentText);
        String extractedTimeStr = null;
        if (msTime.find()) {
            int h = Integer.parseInt(msTime.group(1));
            int min = 0;
            if (msTime.group(2) != null) {
                min = Integer.parseInt(msTime.group(2));
            }
            String ap = msTime.group(3); // am/pm
            if (ap == null && msTime.groupCount() >= 3) {
                 // The regex matches group 3 differently based on branches, checking raw bounds
                 Matcher m2 = Pattern.compile("\\b(\\d{1,2})(?:[:h](\\d{2}))?\\s*(am|pm)\\b").matcher(noAccentText);
                 if(m2.find()) ap = m2.group(3);
            }

            if (ap != null) {
                if (ap.equals("pm") && h < 12) h += 12;
                if (ap.equals("am") && h == 12) h = 0;
            }
            if (h >= 0 && h < 24 && min >= 0 && min < 60) {
                result.dueTime = String.format(Locale.ROOT, "%02d:%02d", h, min);
                foundTime = true;
            }
        }

        // 2b Check am/pm without space directly, fallback or hh:mm am/pm
        if (!foundTime) {
           // For format like "7pm", "07:15 AM", "18h30"
           Matcher fallbackTime2 = Pattern.compile("\\b(\\d{1,2})(?:[:h](\\d{2}))?\\s*([aApP][mM])\\b").matcher(noAccentText);
           if (fallbackTime2.find()) {
               int h = Integer.parseInt(fallbackTime2.group(1));
               int min = 0;
               if (fallbackTime2.group(2) != null) {
                   min = Integer.parseInt(fallbackTime2.group(2));
               }
               String ap = fallbackTime2.group(3).toLowerCase(Locale.ROOT);
               if (ap.equals("pm") && h < 12) h += 12;
               if (ap.equals("am") && h == 12) h = 0;
               if (h >= 0 && h < 24) {
                   result.dueTime = String.format(Locale.ROOT, "%02d:%02d", h, min);
                   foundTime = true;
               }
           } else {
               Matcher fallbackTime = Pattern.compile("\\b(\\d{1,2})(am|pm|g)\\b").matcher(noAccentText);
               if (fallbackTime.find()) {
                   int h = Integer.parseInt(fallbackTime.group(1));
                   String ap = fallbackTime.group(2).toLowerCase(Locale.ROOT);
                   if (ap.equals("pm") && h < 12) h += 12;
                   if (ap.equals("am") && h == 12) h = 0;
                   if (h >= 0 && h < 24) {
                       result.dueTime = String.format(Locale.ROOT, "%02d:00", h);
                       foundTime = true;
                   }
               }
           }
        }

        // 3. Relative Dates if absolute not found
        if (!foundDate) {
            if (noAccentText.contains("hom nay") || noAccentText.contains("toi nay") || noAccentText.contains("chieu nay") || noAccentText.contains("sang nay") || noAccentText.contains("khuya nay")) {
                foundDate = true;
                // targetDate is already today
            } else if (noAccentText.contains("ngay mai") || noAccentText.contains(" mai ") || noAccentText.matches("^mai .*") || noAccentText.contains("chieu mai") || noAccentText.contains("toi mai") || noAccentText.contains("sang mai") || noAccentText.contains("trua mai")) {
                targetDate.add(Calendar.DAY_OF_YEAR, 1);
                foundDate = true;
            } else if (noAccentText.contains("mot") || noAccentText.contains("ngay kia")) {
                targetDate.add(Calendar.DAY_OF_YEAR, 2);
                foundDate = true;
            } else {
                // Day of week (thứ 2 -> chủ nhật)
                Pattern dowPattern = Pattern.compile("\\bthu (\\d)\\b|\\bchu nhat\\b");
                Matcher mDow = dowPattern.matcher(noAccentText);
                if (mDow.find()) {
                    int targetDow = -1;
                    if (mDow.group(0).equals("chu nhat")) {
                        targetDow = Calendar.SUNDAY;
                    } else {
                        int v = Integer.parseInt(mDow.group(1));
                        if(v >= 2 && v <= 7) targetDow = (v == 8) ? Calendar.SUNDAY : v; // Calendar.MONDAY is 2.
                    }
                    if (targetDow != -1) {
                        int currentDow = now.get(Calendar.DAY_OF_WEEK);
                        int diff = targetDow - currentDow;
                        if (diff <= 0) diff += 7;
                        if (noAccentText.contains("tuan sau")) diff += 7;
                        targetDate.add(Calendar.DAY_OF_YEAR, diff);
                        foundDate = true;
                    }
                }
            }
        }

        // 4. Default Times based on keywords (only if we have a date or deadline context and NO exact time)
        if (!foundTime) {
            // Need to match exact words or roots
            if (noAccentText.matches(".*\\bsang\\b.*")) {
                result.dueTime = "08:00";
            } else if (noAccentText.matches(".*\\btrua\\b.*")) {
                result.dueTime = "12:00";
            } else if (noAccentText.matches(".*\\bchieu\\b.*")) {
                result.dueTime = "15:00";
            } else if (noAccentText.matches(".*\\btoi\\b.*") || noAccentText.matches(".*\\bkhuya\\b.*")) {
                result.dueTime = "20:00";
            } else if (noAccentText.contains("deadline")
                || noAccentText.contains("nop bo")
                || noAccentText.contains("nop bao")
                || noAccentText.contains("han nop")
                || noAccentText.contains("nop bai")
                || noAccentText.matches(".*\\bnop\\b.*")
                || noAccentText.contains("due")) {
                // If it's a deadline, default to 23:59
                result.dueTime = "23:59";
            }
        }

        // Must have at least a date to be valid (or we assume it's valid if we found time and keywords)
        // Usually, to be safe, we only return if date is found.
        if (foundDate) {
            result.dueDate = targetDate.getTimeInMillis();
            return result;
        }

        return null; // Ignore if no reliable date found
    }
}
