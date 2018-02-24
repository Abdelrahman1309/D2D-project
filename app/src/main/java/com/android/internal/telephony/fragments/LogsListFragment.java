package com.android.internal.telephony.fragments;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
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
    ImageView trash;
    public LogsListFragment(){
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_logs_list, container, false);
        v.setBackgroundColor(Color.WHITE);
        listView = v.findViewById(R.id.logs_list_view);
        logs = Constants.getLogs();
        listView.setAdapter(new LogsAdapter(getActivity(),logs));
        trash = v.findViewById(R.id.trash);
        trash.setOnClickListener(v1 -> {
            Constants.deleteLogs();
            logs = Constants.getLogs();
            listView.setAdapter(new LogsAdapter(getActivity(),logs));
        });
        return v;
    }
}
