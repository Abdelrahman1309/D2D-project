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
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.ListView;
import android.support.v4.app.Fragment;
import android.widget.SearchView;

import com.android.internal.telephony.R;
import com.android.internal.telephony.activities.HomeActivity;
import com.android.internal.telephony.contacts.Contacts;
import com.android.internal.telephony.contacts.ContactsAdapter;
import com.android.internal.telephony.utils.Constants;

import java.util.ArrayList;
import java.util.Locale;


public class ContactsListFragment extends Fragment {
    SearchView editsearch;
    ListView listView;
    ArrayList<Contacts> contacts;
    ContactsAdapter adapter = null;
    int numberOfContacts;
    public ContactsListFragment(){
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_contact_list, container, false);
        v.setBackgroundColor(Color.WHITE);
        listView = v.findViewById(R.id.contact_list_view);
        contacts = Constants.users;


        editsearch = getActivity().findViewById(R.id.search);


        if (contacts != null) {
            updateUI(contacts);
            numberOfContacts = contacts.size();
            editsearch.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
                @Override
                public boolean onQueryTextSubmit(String query) {
                    query = query.toLowerCase(Locale.getDefault());
                    ArrayList<Contacts> users= new ArrayList<>();
                    contacts = Constants.users;
                    for (int i = 0 ; i < numberOfContacts ; i++){
                        Contacts user = contacts.get(i);
                        if (user.getContactName().toLowerCase(Locale.getDefault()).contains(query)){
                            try {
                                users.add(user);
                                Log.w("Users",user.getContactName());
                            }catch (NullPointerException ex){}
                        }
                    }
                    Log.w("Users",contacts.toString());
                    if (users != null) updateUI(users);
                    return false;
                }

                @Override
                public boolean onQueryTextChange(String newText) {
                    newText = newText.toLowerCase(Locale.getDefault());
                    ArrayList<Contacts> users= new ArrayList<>();
                    contacts = Constants.users;
                    for (int i = 0 ; i < numberOfContacts ; i++){
                        Contacts user = contacts.get(i);
                        if (user.getContactName().toLowerCase(Locale.getDefault()).contains(newText)){
                            try {
                                users.add(user);
                                Log.w("Users",user.getContactName());
                            }catch (NullPointerException ex){}
                        }
                    }
                    Log.w("Users",contacts.toString());
                    if (users != null) updateUI(users);
                    return false;
                }
            });

            listView.setOnItemClickListener((parent, view, position, id) -> {
                String user = contacts.get(position).getContactName();
                String num = contacts.get(position).getContactNumber();
                sendData(user,num);
            });
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

    private void sendData(String name, String number)
    {
        //INTENT OBJ
        Intent i = new Intent(getActivity().getBaseContext(), HomeActivity.class);

        //PACK DATA
        i.putExtra("name", name);
        i.putExtra("number", number);

        //START ACTIVITY
        getActivity().startActivity(i);
    }
    private  void updateUI (ArrayList<Contacts> contact){
        listView.setAdapter(new ContactsAdapter(getActivity(), contact));
        listView.setOnItemClickListener((parent, view, position, id) -> {
            String user = contact.get(position).getContactName();
            String num = contact.get(position).getContactNumber();
            sendData(user,num);
        });
    }

}
