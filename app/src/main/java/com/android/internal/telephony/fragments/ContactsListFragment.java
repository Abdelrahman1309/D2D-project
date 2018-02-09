package com.android.internal.telephony.fragments;

import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.os.Bundle;

import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.support.v4.app.Fragment;
import com.android.internal.telephony.R;
import com.android.internal.telephony.contacts.Contacts;
import com.android.internal.telephony.contacts.ContactsAdapter;
import com.android.internal.telephony.utils.Constants;

import java.util.ArrayList;


public class ContactsListFragment extends Fragment {

    public ContactsListFragment(){
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_contact_list, container, false);
        v.setBackgroundColor(Color.WHITE);
        ArrayList<Contacts> contacts;

        contacts = Constants.users;

        if (contacts != null) {
            ListView listView = v.findViewById(R.id.contact_list_view);
            listView.setAdapter(new ContactsAdapter(getActivity(), contacts));
        }
        else {
            AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
            builder.setTitle("Contacts Permission");
            builder.setMessage("Please enable Contacts permission to read contacts");
            builder.setPositiveButton("OK", (dialogInterface, i) -> {});
            Dialog alertDialog = builder.create();
            alertDialog.show();
        }
        return v;
    }

}
