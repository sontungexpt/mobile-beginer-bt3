package com.bt3;

import android.Manifest;
import android.app.AlertDialog;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.ContactsContract;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;

import androidx.core.app.ActivityCompat;
import androidx.core.app.ComponentActivity;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class MainActivity extends ComponentActivity {
    private static final int PERMISSION_REQUEST_READ_CONTACTS = 100;
    ListView listView;
    List<Contact> contactList = new ArrayList<>();
    ArrayAdapter<String> adapter;
    List<String> contactNames = new ArrayList<>();
    boolean isMultipleDelete = false;
    List<Contact> selectedContacts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        listView = findViewById(R.id.contact_list);

        // Xin quyền truy cập danh bạ nếu chưa có
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.READ_CONTACTS}, PERMISSION_REQUEST_READ_CONTACTS);
        } else {
            loadContacts();
        }

        // Menu thêm và sắp xếp danh bạ
        Button menuButton = findViewById(R.id.menu_button);
        menuButton.setOnClickListener(v -> showContextMenu(v));
    }

    private void showContextMenu(View view) {
        PopupMenu popup = new PopupMenu(this, view);
        popup.getMenuInflater().inflate(R.menu.contact_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.sort_ascending) {
                sortContacts(true);
                return true;
            } else if (item.getItemId() == R.id.sort_descending) {
                sortContacts(false);
                return true;
            } else if (item.getItemId() == R.id.add_contact) {
                addContactDialog();
                return true;
            } else if (item.getItemId() == R.id.delete_multiple) {
                isMultipleDelete = !isMultipleDelete;
                if (isMultipleDelete) {
                    Toast.makeText(this, "Select contacts to delete", Toast.LENGTH_SHORT).show();
                } else {
                    deleteMultipleContacts();
                }
                return true;
            } else {
                return false;
            }
        });
        popup.show();
    }

    // Hiển thị danh bạ
    private void loadContacts() {
        ContentResolver contentResolver = getContentResolver();
        Cursor cursor = contentResolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        contactList.clear();
        contactNames.clear();

        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                String phoneNumber = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                long id = cursor.getLong(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));

                Contact contact = new Contact(name, phoneNumber, id);
                contactList.add(contact);
                contactNames.add(name + " - " + phoneNumber);
            }
            cursor.close();
        }

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, contactNames);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (isMultipleDelete) {
                selectedContacts.add(contactList.get(position));
            } else {
                showContactMenu(contactList.get(position));
            }
        });
    }

    private void showContactMenu(Contact contact) {
        PopupMenu popup = new PopupMenu(this, listView);
        popup.getMenuInflater().inflate(R.menu.single_contact_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.delete_contact) {
                deleteContact(contact);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void sortContacts(boolean ascending) {
        if (ascending) {
            contactList.sort((c1, c2) -> c1.getName().compareTo(c2.getName()));
        } else {
            contactList.sort((c1, c2) -> c2.getName().compareTo(c1.getName()));
        }
        updateContactListView();
    }

    private void updateContactListView() {
        contactNames.clear();
        for (Contact contact : contactList) {
            contactNames.add(contact.getName() + " - " + contact.getPhoneNumber());
        }
        adapter.notifyDataSetChanged();
    }

    // Thêm danh bạ
    private void addContactDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add New Contact");

        // Layout cho dialog
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        EditText nameInput = new EditText(this);
        nameInput.setHint("Name");
        layout.addView(nameInput);

        EditText phoneInput = new EditText(this);
        phoneInput.setHint("Phone Number");
        layout.addView(phoneInput);

        builder.setView(layout);

        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = nameInput.getText().toString().trim();
            String phoneNumber = phoneInput.getText().toString().trim();

            // Kiểm tra đầu vào
            if (!name.isEmpty() && !phoneNumber.isEmpty()) {
                addContactToPhone(name, phoneNumber); // Gọi phương thức để thêm vào danh bạ
                loadContacts();  // Tải lại danh bạ sau khi thêm
            } else {
                Toast.makeText(this, "Please enter valid details.", Toast.LENGTH_SHORT).show();
            }
        });

        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        builder.show();
        //addContact(new Contact("New Contact", "123456789"));
    }
    private void addContactToPhone(String name, String phoneNumber) {
        ContentValues values = new ContentValues();
        values.put(ContactsContract.RawContacts.ACCOUNT_TYPE, true);
        values.put(ContactsContract.RawContacts.ACCOUNT_NAME, true);
        Uri rawContactUri =
                getContentResolver().insert(ContactsContract.RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(rawContactUri);

        // Thêm tên
        values.clear();
        values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        values.put(
                ContactsContract.Data.MIMETYPE,
                ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);
        values.put(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name);
        getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);

        // Thêm số điện thoại
        values.clear();
        values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        values.put(
                ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        values.put(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNumber);
        values.put(
                ContactsContract.CommonDataKinds.Phone.TYPE,
                ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        getContentResolver().insert(ContactsContract.Data.CONTENT_URI, values);

        loadContacts(); // Tải lại danh bạ

    }
    private void addContact(Contact contact) {
        try {
            ArrayList<ContentProviderOperation> ops = new ArrayList<>();
            ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, null)
                    .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, null)
                    .build());

            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, contact.getName())
                    .build());

            ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                    .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                    .withValue(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                    .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, contact.getPhoneNumber())
                    .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                    .build());

            getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            Toast.makeText(this, "Contact added", Toast.LENGTH_SHORT).show();
            loadContacts();
        } catch (Exception e) {
            Log.e("Add Contact", e.getMessage());
        }
    }

    // Xóa danh bạ
    private void deleteContact(Contact contact) {
        ContentResolver contentResolver = getContentResolver();
        String where = ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?";
        String[] params = new String[]{String.valueOf(contact.getId())};
        contentResolver.delete(ContactsContract.RawContacts.CONTENT_URI, where, params);
        Toast.makeText(this, "Contact deleted", Toast.LENGTH_SHORT).show();
        loadContacts();
    }

    // Xóa nhiều danh bạ
    private void deleteMultipleContacts() {
        for (Contact contact : selectedContacts) {
            deleteContact(contact);
        }
        selectedContacts.clear();
        isMultipleDelete = false;
        loadContacts();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == PERMISSION_REQUEST_READ_CONTACTS) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                loadContacts();
            } else {
                Toast.makeText(this, "Permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }
}
