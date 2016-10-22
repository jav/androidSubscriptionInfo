package org.ubillos.getspn;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.net.UnknownHostException;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private int mInterval = 5000;
    private Handler mHandlerUpdateSubscriptionInfo;
    TextView mCarrierNameLabel;
    private String TAG = "MainActivity";
    private TextView mDisplayNameLabel;
    private TextView mSubscriptionIdLabel;
    private TextView mSubscriptionToString;
    private File ipToASNFileInProgress;
    private File ipToASNFileFinished;
    private File ASNToNameFileInProgress;
    private File ASNToNameFileFinished;
    private Context mContext;
    private Button mResetButton;
    private TextView mIP;
    private TextView mASN;
    private TextView mASNName;
    private IPToASNResolver mIPToASNResolver;
    private ASNToNameResolver mASNToNameResolver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mCarrierNameLabel = (TextView) findViewById(R.id.carrier_name_label);
        mCarrierNameLabel.setText("Carrier name");

        mDisplayNameLabel = (TextView) findViewById(R.id.display_name_label);
        mDisplayNameLabel.setText("Display name");

        mSubscriptionIdLabel = (TextView) findViewById(R.id.subscription_id_label);
        mSubscriptionIdLabel.setText("Display name");

        mSubscriptionToString = (TextView) findViewById(R.id.to_string_label);
        mSubscriptionToString.setText("Display name");

        mIP = (TextView) findViewById(R.id.ip_label);
        mIP.setText("IP");

        mASN = (TextView) findViewById(R.id.ASN_label);
        mASN.setText("ASN");

        mASNName = (TextView) findViewById(R.id.ASN_name_label);
        mASNName.setText("ASNname");

        mResetButton = (Button) findViewById(R.id.reset_button);
        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mIPToASNResolver.deleteFiles();
                mASNToNameResolver.deleteFiles();
            }
        });



        mHandlerUpdateSubscriptionInfo = new Handler();

        mContext = getApplicationContext();

        mIPToASNResolver = new IPToASNResolver(mContext);
        mASNToNameResolver = new ASNToNameResolver(mContext);


        registerReceiver(mIPToASNResolver.getOnDownloadFinishedReceiver(),
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        registerReceiver(mASNToNameResolver.getOnDownloadFinishedReceiver(),
                new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));

    }

    @Override
    protected void onPause(){
        super.onPause();
        stopRepeatingTasks();
    }
    @Override
    protected void onResume(){
        super.onResume();
        startRepeatingTasks();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        unregisterReceiver(mIPToASNResolver.getOnDownloadFinishedReceiver());
        unregisterReceiver(mASNToNameResolver.getOnDownloadFinishedReceiver());

    }

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                updateSubscriptionInfo();
                updateIPInfo();
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandlerUpdateSubscriptionInfo.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    void startRepeatingTasks() {
        mStatusChecker.run();
    }

    void stopRepeatingTasks() {
        mHandlerUpdateSubscriptionInfo.removeCallbacks(mStatusChecker);
    }

    void updateSubscriptionInfo(){
        TelephonyManager telephonyManager = (TelephonyManager) getApplicationContext().getSystemService(Context.TELEPHONY_SERVICE);
        SubscriptionManager subscriptionManager = (SubscriptionManager) getApplicationContext().getSystemService(TELEPHONY_SUBSCRIPTION_SERVICE);
        List<SubscriptionInfo> activeSubscriptionInfoList = subscriptionManager.getActiveSubscriptionInfoList();

        if (activeSubscriptionInfoList == null){
            Log.d(TAG, "subscriptions is null ");
            return;
        }

        final int num = activeSubscriptionInfoList.size();
        if (num <= 0) {
            mCarrierNameLabel.setText("No active subscriptions");
            Log.d(TAG, "subscriptions list size is zero");
            return;
        }

        mCarrierNameLabel.setText(activeSubscriptionInfoList.get(0).getCarrierName());
        mDisplayNameLabel.setText(activeSubscriptionInfoList.get(0).getDisplayName());

        mSubscriptionToString.setText(activeSubscriptionInfoList.get(0).toString());

        //Log.d(TAG, "Subscriber info Updated");

    }

    void updateIPInfo(){
        mIPToASNResolver.downloadDB();
        mASNToNameResolver.downloadDB();

        mIP.setText(Utils.getIPAddress(true)); // IPv4

        // This blocks the UI thread
        // It should be moved to an async task that either chains them
        // or where the asn->name is only triggered if there is a buffered asn

        long ASN=-1;
        try {
            ASN = mIPToASNResolver.resolve(Utils.getIPAddress(true));
            mASN.setText("ASN: " + ASN);
        } catch (UnknownHostException e1) {
            e1.printStackTrace();
            mASN.setText("ASN: Couldn't understand this devices IP address (wirely formatted?).");
        } catch (DownloadNotReadyException e1) {
            mASN.setText("ASN: Database not ready");
        }

        if(ASN != -1) {
            try{
            String ASNName = mASNToNameResolver.resolve(ASN);
            mASNName.setText("ASN name: " + ASNName);
            } catch (DownloadNotReadyException e1){
                // Normal, shit's just not done yet
                mASNName.setText("Database not ready");
            }
        } else {
            mASNName.setText("No ASN to resolve ready.");
        }


    }




}
