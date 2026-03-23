package hcmute.edu.vn.ticktick.contacts.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import hcmute.edu.vn.ticktick.R;
import hcmute.edu.vn.ticktick.contacts.model.ContactItem;

public class ContactsAdapter extends RecyclerView.Adapter<ContactsAdapter.ContactViewHolder> {

    public interface OnContactClickListener {
        void onContactClick(ContactItem item);
    }

    private final List<ContactItem> fullItems = new ArrayList<>();
    private final List<ContactItem> filteredItems = new ArrayList<>();
    private final OnContactClickListener onContactClickListener;

    public ContactsAdapter(OnContactClickListener onContactClickListener) {
        this.onContactClickListener = onContactClickListener;
    }

    public void setItems(List<ContactItem> items) {
        fullItems.clear();
        if (items != null) {
            fullItems.addAll(items);
        }
        filteredItems.clear();
        filteredItems.addAll(fullItems);
        notifyDataSetChanged();
    }

    public void filter(String query) {
        filteredItems.clear();

        String keyword = query == null ? "" : query.trim().toLowerCase(Locale.getDefault());
        if (keyword.isEmpty()) {
            filteredItems.addAll(fullItems);
            notifyDataSetChanged();
            return;
        }

        for (ContactItem item : fullItems) {
            String lowerName = item.getDisplayName().toLowerCase(Locale.getDefault());
            if (lowerName.contains(keyword)) {
                filteredItems.add(item);
                continue;
            }

            for (String number : item.getPhoneNumbers()) {
                if (number.toLowerCase(Locale.getDefault()).contains(keyword)) {
                    filteredItems.add(item);
                    break;
                }
            }
        }

        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ContactViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_contact, parent, false);
        return new ContactViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ContactViewHolder holder, int position) {
        ContactItem item = filteredItems.get(position);
        holder.bind(item);
        holder.itemView.setOnClickListener(v -> onContactClickListener.onContactClick(item));
    }

    @Override
    public int getItemCount() {
        return filteredItems.size();
    }

    static class ContactViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvName;
        private final TextView tvPhone;

        ContactViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName = itemView.findViewById(R.id.tv_contact_name);
            tvPhone = itemView.findViewById(R.id.tv_contact_phone);
        }

        void bind(ContactItem item) {
            String name = item.getDisplayName().isEmpty()
                    ? itemView.getContext().getString(R.string.contacts_unknown_name)
                    : item.getDisplayName();
            tvName.setText(name);

            List<String> phones = item.getPhoneNumbers();
            if (phones.isEmpty()) {
                tvPhone.setText(R.string.contacts_no_phone);
            } else if (phones.size() == 1) {
                tvPhone.setText(phones.get(0));
            } else {
                tvPhone.setText(itemView.getContext().getString(
                        R.string.contacts_phone_count,
                        phones.get(0),
                        phones.size() - 1
                ));
            }
        }
    }
}
