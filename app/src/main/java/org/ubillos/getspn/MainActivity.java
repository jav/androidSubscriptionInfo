package org.ubillos.getspn;

import android.content.Context;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

import java.util.List;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    private int mInterval = 2000;
    private Handler mHandlerUpdateSubscriptionInfo;
    TextView mCarrierNameLabel;
    private String TAG = "MainActivity";
    private TextView mDisplayNameLabel;
    private TextView mSubscriptionIdLabel;
    private TextView mSubscriptionToString;

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

        mHandlerUpdateSubscriptionInfo = new Handler();


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


    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                updateSubscriptionInfo(); //this function can change value of mInterval.
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

        Random r = new Random();


        mCarrierNameLabel.setText(activeSubscriptionInfoList.get(0).getCarrierName());
        mDisplayNameLabel.setText(activeSubscriptionInfoList.get(0).getDisplayName());


        mSubscriptionToString.setText(activeSubscriptionInfoList.get(0).toString());

        Log.d(TAG, "Subscriber info Updated");

    }

    void updateIPInfo(){
        // if file dosn't exist
        Utils.getIPAddress(true); // IPv4
        Utils.getIPAddress(false); // IPv6

    }
}
