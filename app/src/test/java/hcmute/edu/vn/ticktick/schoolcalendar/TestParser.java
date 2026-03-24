package hcmute.edu.vn.ticktick.schoolcalendar;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import hcmute.edu.vn.ticktick.database.SchoolCalendarEventEntity;

public class TestParser {
    public static void main(String[] args) throws Exception {
        String content = new String(Files.readAllBytes(Paths.get("test.ics")));
        IcsParser parser = new IcsParser();
        List<SchoolCalendarEventEntity> list = parser.parse(content, "URL", System.currentTimeMillis());
        System.out.println("Parsed " + list.size() + " events");
        if (!list.isEmpty()) {
            System.out.println("First Event: " + list.get(0).getTitle() + " at " + list.get(0).getStartTimeMillis());
        }
    }
}

