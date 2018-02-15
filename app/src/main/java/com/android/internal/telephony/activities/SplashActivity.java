package com.android.internal.telephony.activities;

import android.Manifest;
import android.annotation.TargetApi;
import android.app.ActionBar;
import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.location.LocationManager;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.RemoteException;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.android.internal.telephony.R;
import com.android.internal.telephony.contacts.Contacts;
import com.android.internal.telephony.receivers.WifiNetworkReceiver;
import com.android.internal.telephony.services.CallService;
import com.android.internal.telephony.services.IncomingVIOPCallService;
import com.android.internal.telephony.services.SignalingService;
import com.android.internal.telephony.utils.Constants;
import com.android.internal.telephony.utils.NetworkUtils;
import com.android.internal.telephony.utils.PhoneUtils;
import com.android.internal.telephony.views.AskPhoneNumber;

import org.abtollc.sdk.AbtoApplication;
import org.abtollc.sdk.AbtoPhone;
import org.abtollc.sdk.AbtoPhoneCfg;
import org.abtollc.sdk.OnInitializeListener;
import org.abtollc.sdk.OnRegistrationListener;
import org.abtollc.utils.codec.Codec;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;


//Todo (1) Check permissions and guarantee it - done
//Todo (2) Get Phone Number = done
//Todo (3) Register Power Monitors - done
//Todo (4) Start Signaling Server - done
//Todo (5) Initialize SIP connection -done

public class SplashActivity extends AppCompatActivity  implements OnInitializeListener {
    SharedPreferences prefs;
    String devicePhoneNumber;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        requestBasicPermissions();
        requestRecordAudioPermission();
        ContactsAsyncTask task = new ContactsAsyncTask();
        task.execute("Contacts");

        String phoneNumber = PhoneUtils.getPhoneNumber(this);
        if(phoneNumber!= null) {
            phoneNumber = phoneNumber.replace(" ", "");
            Constants.setPhoneNumber(phoneNumber);
            //phoneNumber = null;
        }
        prefs = getSharedPreferences(Constants.SharedPref.SHARED_PREF, MODE_PRIVATE);
        devicePhoneNumber = prefs.getString(Constants.SharedPref.SHARED_PREF_PHONE_NUM,"SHARED_PREF_PHONE_NUM");


        if (devicePhoneNumber.length() != 11 && !devicePhoneNumber.startsWith("01")){
            showDlg(this);
        }


        NetworkUtils.turnOnWifi(this);
        Constants.setDeviceIP(NetworkUtils.getWifiApIpAddress());

/*
        //open location if turned off
        LocationManager lm = (LocationManager) getSystemService(LOCATION_SERVICE);
//        assert lm != null;



        if(lm!=null) {
            if (!lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                    !lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                // Build the alert dialog
                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Location Services Not Active");
                builder.setMessage("Please enable Location Services and GPS");
                builder.setPositiveButton("OK", (dialogInterface, i) -> {
                    // Show location settings when the user acknowledges the alert dialog
                    Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivity(intent);
                });
                Dialog alertDialog = builder.create();
                alertDialog.setCanceledOnTouchOutside(false);
                alertDialog.show();
            }
        }
*/

        //Start signaling service
        startService(new Intent(this, SignalingService.class));
        startService(new Intent(this, CallService.class));
        startService(new Intent(this, IncomingVIOPCallService.class));

        registerReceiver(regis,new IntentFilter(Constants.Signaling.USER_REGISTER_REQUEST_SIGNAL_PARAM));

        WifiManager wifiManager = (WifiManager) getApplicationContext().getSystemService(WIFI_SERVICE);

        //assert wifiManager != null;
        if (wifiManager != null) {
            wifiManager.startScan();
            getApplicationContext().registerReceiver(new WifiNetworkReceiver(), new IntentFilter(WifiManager.SCAN_RESULTS_AVAILABLE_ACTION));
        }
        // Get AbtoPhone instance
        abtoPhone = ((AbtoApplication) getApplication()).getAbtoPhone();
        boolean bCanStartPhoneInitialization = (Build.VERSION.SDK_INT >= 23) ?  askPermissions() : true;

        if(bCanStartPhoneInitialization)    initPhone();

        prefs = getSharedPreferences(Constants.SharedPref.SHARED_PREF, MODE_PRIVATE);
        devicePhoneNumber = prefs.getString(Constants.SharedPref.SHARED_PREF_PHONE_NUM,"SHARED_PREF_PHONE_NUM");

        if (devicePhoneNumber.length() == 11 && devicePhoneNumber.startsWith("01")){
            final Handler handler = new Handler();
            handler.postDelayed(() -> {
                Constants.setPhoneNumber(devicePhoneNumber);
                openHomeActivity();
            }, 5000);

        }

    }

    BroadcastReceiver regis = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(Constants.TAG,"Incomming message from Splash Activity: "+intent.getStringExtra
                    (Constants.Signaling.SIGNALING_MESSAGE).substring(0,40));
        }
    };

    private void openHomeActivity(){
        Intent i = new Intent(this,HomeActivity.class);
        startActivity(i);
    }

    private void requestBasicPermissions(){
        List<String> permissionsNeeded = new ArrayList<>();
        final List<String> permissionsList = new ArrayList<>();
        if (!addPermission(permissionsList, Manifest.permission.READ_PHONE_STATE))       permissionsNeeded.add("READ PHONE STATE");
        if (!addPermission(permissionsList, Manifest.permission.ACCESS_COARSE_LOCATION)) permissionsNeeded.add("ACCESS COARSE LOCATION");
        if (!addPermission(permissionsList, Manifest.permission.ACCESS_WIFI_STATE))      permissionsNeeded.add("ACCESS WIFI STATE");
        if (!addPermission(permissionsList, Manifest.permission.CHANGE_WIFI_STATE))      permissionsNeeded.add("CHANGE WIFI STATE");
        if (!addPermission(permissionsList, Manifest.permission.CALL_PHONE))             permissionsNeeded.add("CALL PHONE");
        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                // Need Rationale
                //String message = "You need to grant access to " + permissionsNeeded.get(0);
                //for (int i = 1; i < permissionsNeeded.size(); i++) message = message + ", " + permissionsNeeded.get(i);


                ActivityCompat.requestPermissions(this,
                        permissionsList.toArray(new String[permissionsList.size()]),
                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            }

            ActivityCompat.requestPermissions(this,
                    permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
        }
    }

    //region VOIP APP
    AbtoPhone abtoPhone;

    final private int REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS = 124;

    private boolean askPermissions() {
        List<String> permissionsNeeded = new ArrayList<>();

        final List<String> permissionsList = new ArrayList<>();
        if (!addPermission(permissionsList, Manifest.permission.RECORD_AUDIO))           permissionsNeeded.add("Record audio");
        if (!addPermission(permissionsList, Manifest.permission.WRITE_EXTERNAL_STORAGE)) permissionsNeeded.add("Write logs to sd card");
        if (!addPermission(permissionsList, Manifest.permission.USE_SIP))                permissionsNeeded.add("Use SIP protocol");


        if (permissionsList.size() > 0) {
            if (permissionsNeeded.size() > 0) {
                // Need Rationale
                //String message = "You need to grant access to " + permissionsNeeded.get(0);
                //for (int i = 1; i < permissionsNeeded.size(); i++) message = message + ", " + permissionsNeeded.get(i);


                ActivityCompat.requestPermissions(this,
                        permissionsList.toArray(new String[permissionsList.size()]),
                        REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);

                return false;
            }

            ActivityCompat.requestPermissions(this,
                    permissionsList.toArray(new String[permissionsList.size()]),
                    REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS);
            return false;
        }

        return true;
    }

    private boolean addPermission(List<String> permissionsList, String permission) {
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            permissionsList.add(permission);
            if (!ActivityCompat.shouldShowRequestPermissionRationale(this, permission))     return false;
        }


        return true;
    }

    @TargetApi(23)
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case REQUEST_CODE_ASK_MULTIPLE_PERMISSIONS:
            {
                Map<String, Integer> perms = new HashMap<>();
                //Initial
                perms.put(Manifest.permission.RECORD_AUDIO,           PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.WRITE_EXTERNAL_STORAGE, PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.CAMERA,                 PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.USE_SIP,                PackageManager.PERMISSION_GRANTED);
                perms.put(Manifest.permission.READ_CONTACTS,          PackageManager.PERMISSION_GRANTED);

                //Fill with results
                for (int i = 0; i < permissions.length; i++) perms.put(permissions[i], grantResults[i]);

                //Check for ACCESS_FINE_LOCATION
                if (perms.get(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.USE_SIP) == PackageManager.PERMISSION_GRANTED
                        && perms.get(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
                    // All Permissions Granted
                    initPhone();
                } else {
                    // Permission Denied
                    Toast.makeText(SplashActivity.this, "Some permissions were denied", Toast.LENGTH_SHORT).show();
                }
            }
            break;
            default:
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        }
    }

    @Override
    protected void onResume() {

        super.onResume();
    }

    protected void initPhone() {
        Log.i("VOIP ","Start Initialize voip");
        abtoPhone.setInitializeListener(this);

        //configure phone instance
        AbtoPhoneCfg config = abtoPhone.getConfig();
        config.setCodecPriority(Codec.G729, (short)250);
        config.setCodecPriority(Codec.PCMU, (short)200);
        config.setCodecPriority(Codec.GSM, (short)150);
        config.setCodecPriority(Codec.PCMA, (short)100);
        config.setCodecPriority(Codec.speex_16000, (short)50);

        //Set port
        config.setSipPort(5060);

        //Set timeouts
        config.setRegisterTimeout(5000);
        config.setHangupTimeout(3000);

        config.setSignallingTransport(AbtoPhoneCfg.SignalingTransportType.UDP);

        //to establish secure call set this option to true
        config.setUseSRTP(false);

        // Start initializing - !has to be invoked only once, when  app started!
        abtoPhone.initialize();

        Log.i("VOIP ","Done initialize");
    }

    public void onDestroy() {
        abtoPhone.setInitializeListener(null);
        super.onDestroy();
        unregisterReceiver(regis);

    }//onDestroy

    @Override
    public void onInitializeState(OnInitializeListener.InitializeState state, String message) {
        Log.i("VOIP__","onInitializeState");
        switch (state) {
            case START:
            case INFO:
            case WARNING: break;
            case FAIL:

                new AlertDialog.Builder(SplashActivity.this)
                        .setTitle("Error")
                        .setMessage(message)
                        .setPositiveButton("Ok", (dlg, which) -> dlg.dismiss()).create().show();
                    Log.i("VOIP__","Initialize Fail");
                break;
            case SUCCESS:
                Log.i("VOIP__","Initialize success");
                registerToServer();
                break;

            default:
                break;
        }
    }

    private void registerToServer() {
        Log.i("VOIP ","registerToServer");
        prefs = getSharedPreferences(Constants.SharedPref.SHARED_PREF, MODE_PRIVATE);
        devicePhoneNumber = prefs.getString(Constants.SharedPref.SHARED_PREF_PHONE_NUM,"SHARED_PREF_PHONE_NUM");
        String user = devicePhoneNumber;
        String pass = "ab" + devicePhoneNumber;
        String domain = "192.168.1.4";
        abtoPhone.getConfig().addAccount(domain, null, user, pass, null, "", 300, false);
        try{
            abtoPhone.register();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        // Set registration event
        abtoPhone.setRegistrationStateListener(new OnRegistrationListener() {

            public void onRegistrationFailed(long accId, int statusCode, String statusText) {
                Log.i(Constants.TAG,"SIP RegistrationFailed");
//                openHomeActivity();
            }

            public void onRegistered(long accId) {
                Log.i(Constants.TAG,"SIP Registered");
//                openHomeActivity();
            }

            @Override
            public void onUnRegistered(long arg0) {
            }
        }); //registration listener
    }
    //endregion

    private static void showDlg(Activity activity){
        AskPhoneNumber dlg = new AskPhoneNumber();
        WindowManager.LayoutParams params = new WindowManager.LayoutParams();
        dlg.setCancelable(false);
        dlg.show(activity.getFragmentManager(),Constants.SharedPref.ASK_NUMBER_DLG);

    }

    private void requestRecordAudioPermission() {
        //check API version, do nothing if API version < 23!
        int currentapiVersion = android.os.Build.VERSION.SDK_INT;
        if (currentapiVersion > android.os.Build.VERSION_CODES.LOLLIPOP){

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {

                // Should we show an explanation?
                if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.RECORD_AUDIO)) {

                    // Show an expanation to the user *asynchronously* -- don't block
                    // this thread waiting for the user's response! After the user
                    // sees the explanation, try again to request the permission.

                } else {

                    // No explanation needed, we can request the permission.

                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 1);
                }
            }
        }
    }

    private ArrayList<Contacts> getContactList() {
        final String DISPLAY_NAME = Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB ?
                ContactsContract.Contacts.DISPLAY_NAME_PRIMARY : ContactsContract.Contacts.DISPLAY_NAME;

        final String FILTER = DISPLAY_NAME + " NOT LIKE '%@%'";

        final String ORDER = String.format("%1$s COLLATE NOCASE", DISPLAY_NAME);

        final String[] PROJECTION = {
                ContactsContract.Contacts._ID,
                DISPLAY_NAME,
                ContactsContract.Contacts.HAS_PHONE_NUMBER
        };

        ArrayList<Contacts> contacts = new ArrayList<>();

        ContentResolver cr = getContentResolver();
        Cursor cursor = cr.query(ContactsContract.Contacts.CONTENT_URI, PROJECTION, FILTER, null, ORDER);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                // get the contact's information
                String id = cursor.getString(cursor.getColumnIndex(ContactsContract.Contacts._ID));
                String name = cursor.getString(cursor.getColumnIndex(DISPLAY_NAME));
                Integer hasPhone = cursor.getInt(cursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER));

                // get the user's phone number
                String phone = null;
                if (hasPhone > 0) {
                    Cursor cp = cr.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null,
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?", new String[]{id}, null);
                    if (cp != null && cp.moveToFirst()) {
                        phone = cp.getString(cp.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        cp.close();
                    }
                }// if the user user has an email or phone then add it to contacts
                if (!TextUtils.isEmpty(phone)) {
                    phone = phone.replaceAll("[^0-9]", "");
                    if (phone.startsWith("2"))phone = phone.substring(1);
                    if (phone.startsWith("00"))phone = phone.substring(1);
                    Contacts contact = new Contacts(name,phone);
                    contacts.add(contact);
                }
            } while (cursor.moveToNext());

            // clean up cursor
            cursor.close();
        }
        return contacts;
    }

    private class ContactsAsyncTask extends AsyncTask<String,Void,ArrayList<Contacts>> {
        @Override
        protected ArrayList<Contacts> doInBackground(String... users) {
            // Perform the HTTP request for earthquake data and process the response.
            try {
                return getContactList();
            }catch (SecurityException ex){
                return null;
            }
        }

        @Override
        protected void onPostExecute(ArrayList<Contacts> contacts) {
            super.onPostExecute(contacts);
            // If there is no result, do nothing.
            if (contacts != null) Constants.users = contacts;
            else return;
        }
    }}
