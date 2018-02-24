package com.android.internal.telephony.views;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.android.internal.telephony.R;
import com.android.internal.telephony.activities.CallActivity;
import com.android.internal.telephony.activities.HomeActivity;
import com.android.internal.telephony.activities.SplashActivity;
import com.android.internal.telephony.utils.Constants;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;


public class AskPhoneNumber extends DialogFragment{
    Activity mActivity;
    String number,checkNumber;
    Button mSaveBtn;
    EditText mPhoneNumTxt;

    public AskPhoneNumber() {
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.ask_phone_number,container);
        mSaveBtn = v.findViewById(R.id.dlg_save_btn);
        mPhoneNumTxt = v.findViewById(R.id.dlg_et_phone_num);

        mSaveBtn.setOnClickListener(v1 -> {
            checkNumber = mPhoneNumTxt.getText().toString();

            if(checkNumber.length() == 11 && checkNumber.startsWith("01") ) {
                onSaveBtnClicked();
            }
            else{
                Toast toast = Toast.makeText(v.getContext(),"Invalid number", Toast.LENGTH_SHORT);
                toast.show();
            }
        });

        return v;
    }

    private void onSaveBtnClicked(){
        number = mPhoneNumTxt.getText().toString();
        //Save data to shared preferences

        SharedPreferences.Editor editor = getActivity().getSharedPreferences(Constants.SharedPref.SHARED_PREF, Context.MODE_PRIVATE).edit();
        editor.putString( Constants.SharedPref.SHARED_PREF_PHONE_NUM , number);
        editor.apply();

        Constants.setPhoneNumber(number);
        Intent i = new Intent(this.getContext() , HomeActivity.class);
        startActivity(i);

    }
}
