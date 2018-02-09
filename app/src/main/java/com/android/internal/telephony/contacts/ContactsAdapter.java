package com.android.internal.telephony.contacts;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.android.internal.telephony.R;
import com.android.internal.telephony.activities.HomeActivity;
import com.android.internal.telephony.fragments.ContactsListFragment;

import java.util.ArrayList;
import java.util.List;


public class ContactsAdapter<C> extends ArrayAdapter<Contacts> {

    public ContactsAdapter (Context context, ArrayList<Contacts> contacts){
        super(context,0,contacts);
    }
    @Override
    public View getView(int position, @Nullable View convertView, ViewGroup parent) {
        View listItemView = convertView;
        if (listItemView == null){
            listItemView = LayoutInflater.from(getContext()).inflate(R.layout.contact_item,parent,false);
        }
        Contacts currentContact = getItem(position);

        TextView name = listItemView.findViewById(R.id.contact_name);
        name.setText(currentContact.getContactName());

        TextView number = listItemView.findViewById(R.id.contact_number);
        number.setText(currentContact.getContactNumber());

        return listItemView;
    }
}
