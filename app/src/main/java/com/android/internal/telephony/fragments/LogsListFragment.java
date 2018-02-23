package com.android.internal.telephony.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import com.android.internal.telephony.R;
import android.support.v4.app.Fragment;
import com.android.internal.telephony.contacts.Logs;
import com.android.internal.telephony.contacts.LogsAdapter;
import com.android.internal.telephony.utils.Constants;

import java.util.ArrayList;


public class LogsListFragment extends Fragment {
    ListView listView;
    ArrayList<Logs> logs = new ArrayList<>();
    public LogsListFragment(){
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_logs_list, container, false);
        v.setBackgroundColor(Color.WHITE);
        listView = v.findViewById(R.id.logs_list_view);
        //logs = Constants.logs;
        logs.add(new Logs("Abdelrahman",R.drawable.forward_call,"D2D","10:43 am"));
        logs.add(new Logs("Hashem",R.drawable.cancel_call,"VOIP","10:43 am"));
        logs.add(new Logs("Kholy",R.drawable.forward_call,"D2D","11:43 am"));
        logs.add(new Logs("Tal3at",R.drawable.income_call,"VOIP","10:00 pm"));
        logs.add(new Logs("Sika",R.drawable.missed_call,"D2D","07:30 pm"));
        listView.setAdapter(new LogsAdapter(getActivity(),logs));
        return v;
    }
}
