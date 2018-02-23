package com.android.internal.telephony.contacts;


public class AvailableContacts {
    private String mName;

    public AvailableContacts (String name){
        mName = name;
    }

    public String getContactName(){
        return mName;
    }
}
