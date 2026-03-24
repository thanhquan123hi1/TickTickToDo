package hcmute.edu.vn.ticktick.schoolcalendar;

import android.text.TextUtils;

import androidx.annotation.Nullable;

import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.ticktick.database.Category;
import hcmute.edu.vn.ticktick.database.SchoolCalendarEventEntity;

public class SchoolCalendarExportUseCase {

    private final SchoolTaskLinkRepository linkRepository;

    public SchoolCalendarExportUseCase(SchoolTaskLinkRepository linkRepository) {
        this.linkRepository = linkRepository;
    }

    public SchoolTaskLinkRepository.ExportResult exportEvent(SchoolCalendarEventEntity event, List<Category> categories) {
        Integer defaultCategoryId = findDefaultCategoryId(event, categories);
        return linkRepository.exportToTask(event, defaultCategoryId);
    }

    @Nullable
    public Integer findDefaultCategoryId(SchoolCalendarEventEntity event, List<Category> categories) {
        if (categories == null || categories.isEmpty()) {
            return null;
        }

        int bestScore = Integer.MIN_VALUE;
        Integer bestCategoryId = null;

        for (Category category : categories) {
            String name = category.getName();
            if (TextUtils.isEmpty(name)) {
                continue;
            }

            String lower = name.toLowerCase(Locale.ROOT);
            int score = 0;
            if (lower.contains("study") || lower.contains("học") || lower.contains("school")) {
                score += 10;
            }

            String eventCategory = event.getCategory();
            if (!TextUtils.isEmpty(eventCategory) && lower.contains(eventCategory.toLowerCase(Locale.ROOT))) {
                score += 4;
            }

            if (score > bestScore) {
                bestScore = score;
                bestCategoryId = category.getId();
            }
        }

        return bestCategoryId;
    }
}

