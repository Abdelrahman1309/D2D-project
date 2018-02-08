package com.android.internal.telephony.activities;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v4.app.FragmentActivity;
import android.util.Log;

import com.android.internal.telephony.R;
import com.android.internal.telephony.fragments.CallProcessFragment;
import com.android.internal.telephony.fragments.IncomeCallFragment;
import com.android.internal.telephony.utils.Constants;

import org.abtollc.sdk.AbtoApplication;
import org.abtollc.sdk.AbtoPhone;

//Todo (1) regarding to received intent display (Income call or outcome call)
//Todo (2) Interact with call service

/**
 * There are two components can fire this activity
 * @link HomeActivity
 * @link SignlingService
 *
 * This activity will manage call events:
 *  - Incomming call
 *  - Outgoing call
 *  - end call
 * At incoming call
 * it will push Incoming call fragment
 * At Accept or reject, This activity must send signal to signaling server
 */
public class CallActivity extends FragmentActivity {

    private static String TAG;

    //Fragments
    private IncomeCallFragment mCallFragment;
    private CallProcessFragment mProcessCallFrag;

    //Call process
    private String mDeviceIP = null;
    private String mDevicePhoneNumber = null;
    private String mIncomePhoneNumber = null;
    private String mCallTech = null;
    //Broadcast receiver
    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            //In case user accept call
            //open call Instance by sending broadcast to calling service
            if(intent.getAction().equals("CALL_ACCEPTED")) {
                Intent i = new Intent();
                i.setAction(Constants.Calling.CALL_SERVICE_ACTION);
                i.putExtra(Constants.Calling.MAKE_CALL_ACTION_PARAM, mDeviceIP);
                sendBroadcast(i);
                try {
                    pushCallProcessFragment(mDevicePhoneNumber);
                }catch (IllegalStateException ex){

                }
            }else if(intent.getAction().equals("CALL_ENDED")) {
                CallActivity.this.finish();
            }
        }
    };

    private Intent mIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_call);

        TAG = this.getClass().getSimpleName();
        //register broadcast receiver
        IntentFilter intentFilter = new IntentFilter("CALL_ACCEPTED");
        intentFilter.addAction("CALL_ENDED");
        registerReceiver(mBroadcastReceiver,intentFilter);

        //Get Intent and put it in global intent var
        mIntent = getIntent();
        //Get call tech if D2D
        mCallTech = mIntent.getStringExtra("CALL_TECH");
        //Get Phone Number
        mDevicePhoneNumber = mIntent.getStringExtra("PHONE_NUM");

        Log.i(TAG,String.format("Phone number is %s ",mDevicePhoneNumber));
        //Process Intent, by get call type
        if(mIntent.getStringExtra("CALL_TYPE").equals("OUTGOING")){
            if(mCallTech.equals("D2D")) {
                //Case D2D outgoing call
                try {
                    mDeviceIP = Constants.getPhoneNumber(mDevicePhoneNumber).second;//Second is device ip
                }catch (Exception ex){
                    Log.d(TAG,"Device not found please handle it");
                    this.finish();
                }
                mCallTech = "D2D";
                Log.i(TAG, String.format("Device IP is: %s", mDeviceIP));
                //send Signal to recipient device
                String signalMsg = "_invite_##" + Constants.getPhoneNumber();
                sendSignal(signalMsg);
                // push Call process fragment
                pushCallProcessFragment(mDevicePhoneNumber);
            }else if(mCallTech.equals("VOIP")){
                AbtoPhone abtoPhone = ((AbtoApplication)getApplication()).getAbtoPhone();
                try {
                    abtoPhone.startCall(mDevicePhoneNumber,abtoPhone.getCurrentAccountId());
                    pushCallProcessFragment(mDevicePhoneNumber);
                } catch (RemoteException e) {
                    e.printStackTrace();
                    this.finish();
                }
            }
        }else if(mIntent.getStringExtra("CALL_TYPE").equals("INCOMING")){
            Log.i(TAG,"Incoming call");
            //check if incoming call tech is d2d
            if(mCallTech!= null && mIntent.getStringExtra("CALL_TECH").equals("D2D")){
                mDeviceIP = mIntent.getStringExtra("PHONE_IP");
                mIncomePhoneNumber = mIntent.getStringExtra("PHONE_NUM");
                Log.i(TAG, String.format("Incoming Phone ip is: %s",mDeviceIP));
            }
            //Case incoming call p ush Call incoming call fragment
            pushIncomingCallFragment(mIncomePhoneNumber);
        }

    }

    public void answerCall(){
        if(mCallTech.equals("D2D")){
            Log.i(TAG,"Answer call invoked");
            //Start Calling Server By sending broadcast to CallService
            Intent i = new Intent();
            i.setAction(Constants.Calling.CALL_SERVICE_ACTION);
            sendBroadcast(i);
            //Send accept to recipient By send broadcast to signaling server to send it
            i = new Intent();
            i.setAction(Constants.Signaling.SIGNALING_SERVICE_ACTION);
            i.putExtra(Constants.Signaling.SIGNALING_SERVICE_ACTION_MESSAGE,"_accept_");
            sendBroadcast(i);
        }else if(mCallTech.equals("VOIP")){
            AbtoPhone abtoPhone = ((AbtoApplication)getApplication()).getAbtoPhone();
            try {
                abtoPhone.answerCall(200);
                abtoPhone.setCallDisconnectedListener((s, i, i1) -> {
                    this.finish();
                });
                pushCallProcessFragment(mDevicePhoneNumber);
            } catch (RemoteException e) {
                e.printStackTrace();
                this.finish();
            }
        }

    }

    public void endCall(){
        if(mCallTech.equals("D2D")){
            Log.i(TAG,"End call invoked");
            //End Calling Server By sending broadcast to CallService
            Intent i = new Intent();
            i.setAction(Constants.Calling.CALL_SERVICE_ACTION);
            i.putExtra("END","END");
            sendBroadcast(i);
            //Send end to recipient By send broadcast to signaling server to send it
            i = new Intent();
            i.setAction(Constants.Signaling.SIGNALING_SERVICE_ACTION);
            i.putExtra(Constants.Signaling.SIGNALING_SERVICE_ACTION_MESSAGE,"_end_");
            i.putExtra(Constants.Signaling.SIGNALING_SERVICE_ACTION_IP_ADDRESS,mDeviceIP);
            sendBroadcast(i);

            //finish this activity
            this.finish();
        }else if(mCallTech.equals("VOIP")){
            AbtoPhone abtoPhone = ((AbtoApplication)getApplication()).getAbtoPhone();
            abtoPhone.setCallDisconnectedListener(null);
            try {
                abtoPhone.hangUp();
                CallActivity.this.finish();
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

    }

    private void sendSignal(String signal){
        Intent i = new Intent();
        i.setAction(Constants.Signaling.SIGNALING_SERVICE_ACTION);
        i.putExtra(Constants.Signaling.SIGNALING_SERVICE_ACTION_MESSAGE,signal);
        if(mDeviceIP != null)
            i.putExtra(Constants.Signaling.SIGNALING_SERVICE_ACTION_IP_ADDRESS,mDeviceIP);
        sendBroadcast(i);
        Log.i(TAG,"BroadCast sent");
    }
    private void pushIncomingCallFragment(String phoneNumber) {
        if(mCallFragment == null) mCallFragment = new IncomeCallFragment();
        mCallFragment.setPhoneNumber(phoneNumber);
        mCallFragment.setArguments(mIntent.getExtras());
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragsContainer,mCallFragment)
                .commit();
    }

    private void pushCallProcessFragment(String phoneNumber) {

        if(mProcessCallFrag == null) mProcessCallFrag = new CallProcessFragment();
        mProcessCallFrag.setPhoneNumber(phoneNumber);
        mProcessCallFrag.setArguments(mIntent.getExtras());

        getSupportFragmentManager().beginTransaction().replace(R.id.fragsContainer,mProcessCallFrag).commit();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(mBroadcastReceiver);
    }

    //Broadcast receiver implementation

}
