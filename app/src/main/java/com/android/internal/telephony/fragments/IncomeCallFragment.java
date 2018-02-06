package com.android.internal.telephony.fragments;


import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.internal.telephony.R;
import com.android.internal.telephony.activities.CallActivity;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

//Todo (1) Display call from and accept and reject call
//Todo (2) Interact with Service
public class IncomeCallFragment extends Fragment {
    String incomePhoneNumber;

    public IncomeCallFragment() {
        // Required empty public constructor
    }
    public void setPhoneNumber(String number){
        incomePhoneNumber = number;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View v = inflater.inflate(R.layout.fragment_income_call, container, false);
        ImageView mAnswer = v.findViewById(R.id.answerCallFrag);
        ImageView mReject = v.findViewById(R.id.rejectCallFrag);
        TextView displayIncomePhoneNum = v.findViewById(R.id.income_phone_num);

        displayIncomePhoneNum.setText(incomePhoneNumber);

        Uri ringTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        Ringtone r = RingtoneManager.getRingtone(getContext(), ringTone);
        r.play();

        mAnswer.setOnClickListener(v1 -> {
            onAnswerClicked();
            r.stop();
        });

        mReject.setOnClickListener(v1 -> {
            onRejectClicked();
            r.stop();
        });

        return v;
    }


    private void onAnswerClicked(){
        Log.i("Incomming Call","Answer Btn Clicked");
        ((CallActivity)getActivity()).answerCall();
    }

    private void onRejectClicked(){
        Log.i("Incomming Call","Reject Btn Clicked");
        ((CallActivity)getActivity()).endCall();
    }

}
