package hcmute.edu.vn.ticktick.contacts.data;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.provider.ContactsContract;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import hcmute.edu.vn.ticktick.contacts.model.ContactItem;

public class ContactsRepository {

    public List<ContactItem> loadContacts(Context context) {
        ContentResolver resolver = context.getContentResolver();

        String[] projection = new String[] {
                ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
        };

        Map<Long, ContactAccumulator> buckets = new LinkedHashMap<>();

        try (Cursor cursor = resolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                projection,
                null,
                null,
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " COLLATE NOCASE ASC"
        )) {
            if (cursor == null) {
                return new ArrayList<>();
            }

            int idIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID);
            int nameIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME);
            int numberIndex = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);

            while (cursor.moveToNext()) {
                if (idIndex < 0 || nameIndex < 0 || numberIndex < 0) {
                    continue;
                }

                long contactId = cursor.getLong(idIndex);
                String displayName = safe(cursor.getString(nameIndex));
                String number = safe(cursor.getString(numberIndex));
                if (number.isEmpty()) {
                    continue;
                }

                ContactAccumulator bucket = buckets.get(contactId);
                if (bucket == null) {
                    bucket = new ContactAccumulator(displayName);
                    buckets.put(contactId, bucket);
                }

                bucket.phoneNumbers.add(number);
            }
        }

        List<ContactItem> result = new ArrayList<>(buckets.size());
        for (Map.Entry<Long, ContactAccumulator> entry : buckets.entrySet()) {
            ContactAccumulator bucket = entry.getValue();
            result.add(new ContactItem(entry.getKey(), bucket.displayName, new ArrayList<>(bucket.phoneNumbers)));
        }
        return result;
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static class ContactAccumulator {
        final String displayName;
        final LinkedHashSet<String> phoneNumbers = new LinkedHashSet<>();

        ContactAccumulator(String displayName) {
            this.displayName = displayName;
        }
    }
}

