package hcmute.edu.vn.ticktick.contacts.ui;

import android.Manifest;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import hcmute.edu.vn.ticktick.BaseActivity;
import hcmute.edu.vn.ticktick.R;
import hcmute.edu.vn.ticktick.contacts.data.ContactsRepository;
import hcmute.edu.vn.ticktick.contacts.model.ContactItem;

public class ContactsActivity extends BaseActivity {

    private MaterialToolbar toolbar;
    private EditText etSearch;
    private RecyclerView recyclerContacts;
    private TextView tvEmpty;
    private LinearLayout layoutPermissionState;
    private TextView tvPermissionMessage;
    private MaterialButton btnRequestPermission;

    private ContactsAdapter adapter;
    private final ContactsRepository contactsRepository = new ContactsRepository();
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private final ActivityResultLauncher<String> contactsPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) {
                    showContentState();
                    loadContacts();
                } else {
                    showPermissionState(true);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_contacts);

        bindViews();
        setupToolbar();
        setupRecyclerView();
        setupSearch();
        setupPermissionActions();

        if (hasContactsPermission()) {
            showContentState();
            loadContacts();
        } else {
            showPermissionState(false);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        ioExecutor.shutdownNow();
    }

    private void bindViews() {
        toolbar = findViewById(R.id.toolbar_contacts);
        etSearch = findViewById(R.id.et_contact_search);
        recyclerContacts = findViewById(R.id.recycler_contacts);
        tvEmpty = findViewById(R.id.tv_contacts_empty);
        layoutPermissionState = findViewById(R.id.layout_contacts_permission);
        tvPermissionMessage = findViewById(R.id.tv_contacts_permission_message);
        btnRequestPermission = findViewById(R.id.btn_contacts_request_permission);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupRecyclerView() {
        adapter = new ContactsAdapter(this::showContactActions);
        recyclerContacts.setLayoutManager(new LinearLayoutManager(this));
        recyclerContacts.setAdapter(adapter);
    }

    private void setupSearch() {
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s == null ? "" : s.toString());
                updateEmptyState();
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });
    }

    private void setupPermissionActions() {
        btnRequestPermission.setOnClickListener(v -> contactsPermissionLauncher.launch(Manifest.permission.READ_CONTACTS));
    }

    private boolean hasContactsPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void showPermissionState(boolean isAfterDenied) {
        layoutPermissionState.setVisibility(View.VISIBLE);
        recyclerContacts.setVisibility(View.GONE);
        tvEmpty.setVisibility(View.GONE);

        if (isAfterDenied && shouldShowRequestPermissionRationale(Manifest.permission.READ_CONTACTS)) {
            tvPermissionMessage.setText(R.string.contacts_permission_message);
        } else if (isAfterDenied) {
            tvPermissionMessage.setText(R.string.contacts_permission_denied);
        } else {
            tvPermissionMessage.setText(R.string.contacts_permission_message);
        }
    }

    private void showContentState() {
        layoutPermissionState.setVisibility(View.GONE);
        recyclerContacts.setVisibility(View.VISIBLE);
    }

    private void loadContacts() {
        ioExecutor.execute(() -> {
            try {
                List<ContactItem> contacts = contactsRepository.loadContacts(this);
                runOnUiThread(() -> {
                    adapter.setItems(contacts);
                    String query = etSearch.getText() == null ? "" : etSearch.getText().toString();
                    adapter.filter(query);
                    updateEmptyState();
                });
            } catch (Exception ex) {
                runOnUiThread(() -> {
                    adapter.setItems(null);
                    updateEmptyState();
                    Toast.makeText(this, R.string.contacts_loading_failed, Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void updateEmptyState() {
        if (adapter.getItemCount() == 0) {
            tvEmpty.setVisibility(View.VISIBLE);
            recyclerContacts.setVisibility(View.GONE);
        } else {
            tvEmpty.setVisibility(View.GONE);
            recyclerContacts.setVisibility(View.VISIBLE);
        }
    }

    private void showContactActions(ContactItem item) {
        String title = item.getDisplayName().isEmpty()
                ? getString(R.string.contacts_unknown_name)
                : item.getDisplayName();

        String[] actions = new String[] {
                getString(R.string.contact_action_call),
                getString(R.string.contact_action_sms)
        };

        new MaterialAlertDialogBuilder(this)
                .setTitle(title)
                .setItems(actions, (dialog, which) -> {
                    if (which == 0) {
                        pickNumberThenRun(item, this::openDialer);
                    } else if (which == 1) {
                        pickNumberThenRun(item, this::openSmsApp);
                    }
                })
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void pickNumberThenRun(ContactItem item, NumberAction action) {
        List<String> phoneNumbers = item.getPhoneNumbers();
        if (phoneNumbers == null || phoneNumbers.isEmpty()) {
            Toast.makeText(this, R.string.contacts_no_phone, Toast.LENGTH_SHORT).show();
            return;
        }

        if (phoneNumbers.size() == 1) {
            action.run(phoneNumbers.get(0));
            return;
        }

        CharSequence[] choices = phoneNumbers.toArray(new CharSequence[0]);
        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.contacts_choose_number)
                .setItems(choices, (dialog, which) -> action.run(phoneNumbers.get(which)))
                .setNegativeButton(R.string.btn_cancel, null)
                .show();
    }

    private void openDialer(String phoneNumber) {
        String normalized = phoneNumber == null ? "" : phoneNumber.trim();
        Intent intent = new Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", normalized, null));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, R.string.contacts_call_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private void openSmsApp(String phoneNumber) {
        String normalized = phoneNumber == null ? "" : phoneNumber.trim();
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", normalized, null));
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException ex) {
            Toast.makeText(this, R.string.contacts_sms_unavailable, Toast.LENGTH_SHORT).show();
        }
    }

    private interface NumberAction {
        void run(String phoneNumber);
    }
}
