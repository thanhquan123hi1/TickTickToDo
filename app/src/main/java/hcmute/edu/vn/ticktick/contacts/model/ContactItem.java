package hcmute.edu.vn.ticktick.contacts.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ContactItem {

    private final long contactId;
    private final String displayName;
    private final List<String> phoneNumbers;

    public ContactItem(long contactId, String displayName, List<String> phoneNumbers) {
        this.contactId = contactId;
        this.displayName = displayName;
        this.phoneNumbers = Collections.unmodifiableList(new ArrayList<>(phoneNumbers));
    }

    public long getContactId() {
        return contactId;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getPhoneNumbers() {
        return phoneNumbers;
    }
}

