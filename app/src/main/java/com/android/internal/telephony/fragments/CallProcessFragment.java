package com.android.internal.telephony.fragments;


import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.telephony.R;
import com.android.internal.telephony.activities.CallActivity;
import com.android.internal.telephony.utils.Constants;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static android.content.Context.SENSOR_SERVICE;

//Todo (1) Start Call Time Counter
//Todo (2) End calls
public class CallProcessFragment extends Fragment {
    String phoneNumber;
    TextView displayNumber;
    public CallProcessFragment() {
        // Required empty public constructor
    }

    public void setPhoneNumber(String number){
        phoneNumber = number;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_call_process, container, false);
        ImageView endCall = v.findViewById(R.id.endCallFragBtn);
        ImageView speaker = v.findViewById(R.id.speaker);
        ImageView keypad  = v.findViewById(R.id.keypad);
        ImageView mute    = v.findViewById(R.id.mute);
        displayNumber = v.findViewById(R.id.display_phone_num);
        TextView displayName = v.findViewById(R.id.display_name);
        TextView timer = v.findViewById(R.id.timer);

        displayNumber.setText(phoneNumber);
        endCall.setOnClickListener(v1 -> {
            endCall();
        });

        speaker.setOnClickListener(v1 -> {

        });
        keypad.setOnClickListener(v1 -> {

        });
        mute.setOnClickListener(v1 -> {

        });

        return v;
    }

    private  void endCall(){
        ((CallActivity)getActivity()).endCall();
        displayNumber.clearComposingText();
    }
}
