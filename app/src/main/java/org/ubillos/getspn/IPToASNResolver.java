package org.ubillos.getspn;

import android.annotation.TargetApi;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by jav on 2016-10-21.
 */
public class IPToASNResolver {
    private final int mRefreshDayLimit;
    private final Context mContext;
    DownloadManager downloadManager;
    private String TAG = "IPToASNResolver";
    private File ipToASNFileInProgress;
    private File ipToASNFileFinished;
    private BroadcastReceiver mOnDownloadFinishedReceiver;

    public IPToASNResolver(Context context) {
        this(context, 30);
    }
    public IPToASNResolver(Context context, int refreshDayLimit){
        mRefreshDayLimit = refreshDayLimit;
        mContext = context;
        downloadManager = (DownloadManager) mContext.getSystemService(mContext.DOWNLOAD_SERVICE);
        ipToASNFileInProgress = new File(mContext.getExternalFilesDir("db_data"), "ipToASN.progress");
        ipToASNFileFinished = new File(mContext.getExternalFilesDir("db_data"), "ipToASN.tsv");

        mOnDownloadFinishedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive()");
                long extraDownloadId = intent.getExtras().getLong(DownloadManager.EXTRA_DOWNLOAD_ID);
                Uri fileUri = downloadManager.getUriForDownloadedFile(extraDownloadId);
                File downloadedFile = new File(fileUri.getPath());

                if (downloadedFile.getName().equals(ipToASNFileInProgress.getName())) {
                    Log.d(TAG, "Detected finished download of " + ipToASNFileInProgress.getName() + " :: renameing to: " + ipToASNFileFinished.getName());
                    ipToASNFileInProgress.renameTo(ipToASNFileFinished);
                } else {
                    Log.d(TAG, "Unmatchable file: " + downloadedFile.getName());
                }
            }
        };
    }


    private static long ipToLong(String ipString) throws UnknownHostException {
        InetAddress ip = InetAddress.getByName(ipString);
        byte[] octets = ip.getAddress();
        long result = 0;
        for (byte octet : octets) {
            result <<= 8;
            result |= octet & 0xff;
        }
        return result;
    }

    public long resolve(String ip) throws DownloadNotReadyException, UnknownHostException {
        long ipAsNumber = ipToLong(ip);
        long prevASN = 0;
        String line;
        if(!ipToASNFileFinished.exists()) {
            throw new DownloadNotReadyException();
        }
        try {
            InputStream fis = new FileInputStream(ipToASNFileFinished);
            InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
            BufferedReader br = new BufferedReader(isr);
            while ((line = br.readLine()) != null) {
                Log.d(TAG, line);
                String[] parts = line.split("/");
                String networkIp = parts[0];
                parts  = parts[1].split("\t");
                long asn = Long.parseLong(parts[1]); // unecessary, could be done on return, but this is easier to read
                Log.d(TAG, "network ip:" + networkIp + ", asn: "+ asn);

                if(ipToLong(networkIp)>ipAsNumber) {
                    return prevASN;
                }
                prevASN = asn;
            }
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }

        return 0;
    }

    public void downloadDB(){
        deleteFileIfOlderThan(ipToASNFileFinished, mRefreshDayLimit);
        if(!ipToASNFileFinished.exists())
            downloadDB( "http://thyme.apnic.net/current/data-raw-table", ipToASNFileInProgress);

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

    public void deleteFiles(){
        ipToASNFileInProgress.delete();
        ipToASNFileFinished.delete();
    }

    public BroadcastReceiver getOnDownloadFinishedReceiver() {
        return mOnDownloadFinishedReceiver;
    }
}

