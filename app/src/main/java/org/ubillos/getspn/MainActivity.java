package org.ubillos.getspn;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.TextView;

import java.io.File;
import java.util.Calendar;
import java.util.Date;
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
    DownloadManager downloadManager;
    private File inProgressFile;
    private File finishedFile;
    private Context mContext;


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

        inProgressFile = new File(getApplicationContext().getExternalFilesDir("db_data"), "ipToASN.progress");
        finishedFile = new File(getApplicationContext().getExternalFilesDir("db_data"), "ipToASN.tsv");

        mHandlerUpdateSubscriptionInfo = new Handler();

        mContext = getApplicationContext();

        downloadManager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        registerReceiver(onDownloadFinishReceiver,
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
        unregisterReceiver(onDownloadFinishReceiver);

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

    BroadcastReceiver onDownloadFinishReceiver = new BroadcastReceiver() {
        public void onReceive(Context ctxt, Intent intent) {
            intent.getExtras().getLong(DownloadManager.EXTRA_DOWNLOAD_ID);
            File fromFile = inProgressFile;
            File toFile = finishedFile;
            Log.d(TAG, "renaming from: "+ inProgressFile.toString() + ", to: " + finishedFile.toString());
            fromFile.renameTo(toFile);
        }
    };

    private boolean validDownload(long downloadId) {

        Log.d(TAG,"Checking download status for id: " + downloadId);
        //Verify if download is a success
        Cursor c= downloadManager.query(new DownloadManager.Query().setFilterById(downloadId));

        if(c.moveToFirst()){
            int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));

            if(status == DownloadManager.STATUS_SUCCESSFUL){
                Log.d(TAG, "File was downloading properly.");
                return true;
            }else{
                int reason = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_REASON));
                Log.d(TAG, "Download not correct, status [" + status + "] reason [" + reason + "]");
                return false;
            }
        }
        return false;
    }


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

        //Log.d(TAG, "Subscriber info Updated");

    }

    void updateIPInfo(){
        if(finishedFile.exists() ) {
            Calendar time = Calendar.getInstance();
            time.add(Calendar.DAY_OF_YEAR, -7);
            //I store the required attributes here and delete them
            Date lastModified = new Date(finishedFile.lastModified());
            if (lastModified.before(time.getTime())) {
                finishedFile.delete();
            }
        }
        if(!finishedFile.exists() ) {

            Log.d(TAG, finishedFile.toString() + " does not exist.");
            if(!inProgressFile.exists()) {
                Log.d(TAG, inProgressFile.toString() + " does not exist.");
                    downloadIpToASNDB();
            } else {
                Log.d(TAG, inProgressFile.toString() + "  exists.");
            }
        } else {
            Log.d(TAG, inProgressFile.toString() + "  exists.");
        }

        // if file dosn't exist download it
        // fork off that work and show progress
        // return

        Utils.getIPAddress(true); // IPv4
        Utils.getIPAddress(false); // IPv6

    }

    private void downloadIpToASNDB() {
        Log.d(TAG, "downloadIpToASNDB()");
        String url = "http://thyme.apnic.net/current/data-raw-table";
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("Some description");
        request.setTitle("Some title");
// in order for this if to run, you must use the android 3.2 to compile your app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            request.allowScanningByMediaScanner();
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        }
        request.setDestinationInExternalFilesDir(mContext, "db_data", inProgressFile.getName());
        Log.d(TAG, "Dowloading to : " + inProgressFile.getName());

// get download service and enqueue file
        downloadManager.enqueue(request);

    }

}
