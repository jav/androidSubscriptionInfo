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
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private int mInterval = 5000;
    private Handler mHandlerUpdateSubscriptionInfo;
    TextView mCarrierNameLabel;
    private String TAG = "MainActivity";
    private TextView mDisplayNameLabel;
    private TextView mSubscriptionIdLabel;
    private TextView mSubscriptionToString;
    DownloadManager downloadManager;
    private File ipToASNFileInProgress;
    private File ipToASNFileFinished;
    private File ASNToNameFileInProgress;
    private File ASNToNameFileFinished;
    private Context mContext;
    private Button mResetButton;
    private TextView mIP;
    private TextView mASN;
    private TextView mASNname;
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

        mASNname = (TextView) findViewById(R.id.ASN_name_label);
        mASNname.setText("ASNname");

        mResetButton = (Button) findViewById(R.id.reset_button);
        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ipToASNFileInProgress.delete();
                ipToASNFileFinished.delete();
            }
        });

        ipToASNFileInProgress = new File(getApplicationContext().getExternalFilesDir("db_data"), "ipToASN.progress");
        ipToASNFileFinished = new File(getApplicationContext().getExternalFilesDir("db_data"), "ipToASN.tsv");
        ASNToNameFileInProgress = new File(getApplicationContext().getExternalFilesDir("db_data"), "ASNToName.progress");
        ASNToNameFileFinished = new File(getApplicationContext().getExternalFilesDir("db_data"), "ASNToName.tsv");


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

        if(ipToASNFileFinished.exists()) {
            mIPToASNResolver = new IPToASNResolver(ipToASNFileFinished);
        }
        if(ASNToNameFileFinished.exists()) {
            mASNToNameResolver = new ASNToNameResolver(ASNToNameFileFinished);
        }

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
            Log.d(TAG, "onReceive()");
            long extraDownloadId = intent.getExtras().getLong(DownloadManager.EXTRA_DOWNLOAD_ID);
            Uri fileUri = downloadManager.getUriForDownloadedFile(extraDownloadId);
            File downloadedFile = new File(fileUri.getPath());

            if(downloadedFile.getName().equals(ipToASNFileInProgress.getName())) {
                Log.d(TAG, "Detected finished download of " + ipToASNFileInProgress.getName() + " :: renameing to: " + ipToASNFileFinished.getName());
                ipToASNFileInProgress.renameTo(ipToASNFileFinished);
                mIPToASNResolver = new IPToASNResolver(ipToASNFileFinished);
            } else  if(downloadedFile.getName().equals(ASNToNameFileInProgress.getName())) {
                Log.d(TAG, "Detected finished download of " + ASNToNameFileInProgress.getName() + " :: renameing to: " + ASNToNameFileFinished.getName());
                ASNToNameFileInProgress.renameTo(ASNToNameFileFinished);
                mASNToNameResolver = new ASNToNameResolver(ASNToNameFileFinished);

            } else {
                Log.d(TAG, "Unmatchable file: " + downloadedFile.getName());
            }
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

        mCarrierNameLabel.setText(activeSubscriptionInfoList.get(0).getCarrierName());
        mDisplayNameLabel.setText(activeSubscriptionInfoList.get(0).getDisplayName());

        mSubscriptionToString.setText(activeSubscriptionInfoList.get(0).toString());

        //Log.d(TAG, "Subscriber info Updated");

    }

    void updateIPInfo(){
        deleteFileIfOlderThan(ipToASNFileFinished, 30);
        deleteFileIfOlderThan(ASNToNameFileFinished, 30);


        if(!ipToASNFileFinished.exists())
            downloadDB( "http://thyme.apnic.net/current/data-raw-table", ipToASNFileInProgress);

        if(!ASNToNameFileFinished.exists())
            downloadDB( "http://thyme.apnic.net/current/data-used-autnums", ASNToNameFileInProgress);

        // if file dosn't exist download it
        // fork off that work and show progress
        // return

        mIP.setText(Utils.getIPAddress(true)); // IPv4

        // This blocks the UI thread
        // It should be moved to an async task that either chains them
        // or where the asn->name is only triggered if there is a buffered asn
        if(mIPToASNResolver != null) {
            long ASN=0;
            try {
                ASN = mIPToASNResolver.resolve(Utils.getIPAddress(true));
                mASN.setText("ASN: " + ASN);

                if(mASNToNameResolver != null) {
                    mASNname.setText("ASN name: " + mASNToNameResolver.resolve(ASN));
                } else {
                    mASNname.setText("Database not yet initialized");
                }
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        } else {
            mASN.setText("Database not yet initialized");
        }


    }

    private void deleteFileIfOlderThan(File file, int days) {
        if(file.exists() ) {
            Calendar time = Calendar.getInstance();
            time.add(Calendar.DAY_OF_YEAR, -1 * days);
            //I store the required attributes here and delete them
            Date lastModified = new Date(file.lastModified());
            if (lastModified.before(time.getTime())) {
                file.delete();
            }
        }
    }

    private void downloadDB(String url, File inProgressFile) {
        Log.d(TAG, "downloadIpToASNDB()");
        if(inProgressFile.exists())
            return;

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("Dowloading "+inProgressFile.getName());
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
