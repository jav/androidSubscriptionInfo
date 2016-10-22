/**
 *
 * Usage
 *   Init
 *      ASNToNameResolver mASNToNameResolver;
 *      mASNToNameResolver = new ASNToNameResolver(context);
 *      registerReceiver(mASNToNameResolver.getOnDownloadFinishedReceiver(),
 *              new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
 *
 *      // should not run in the UI thread
 *      mASNToNameResolver.downloadDB();
 *
 *   use
 *      // can be slow, don't run it in the ui thread
 *      // TODO: Make a listenerclass for async callback:ing
 *      ASN = mASNToNameResolver.resolve(12345);
 *
 *   reset (optional)
 *      mASNToNameResolver.deleteFiles();
 *
 */

package org.ubillos.getspn;

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
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by jav on 2016-10-21.
 */
public class ASNToNameResolver {
    private final int mRefreshDayLimit;
    private String TAG = "IPToASNResolver";
    private Context mContext;
    DownloadManager downloadManager;
    private File ASNToNameFileInProgress;
    private File ASNToNameFileFinished;
    private final BroadcastReceiver mOnDownloadFinishedReceiver;



    public ASNToNameResolver(Context context) {

        this(context, 30);
    }

    public ASNToNameResolver(Context context, int refreshDayLimit){
        mRefreshDayLimit = refreshDayLimit;

        mContext = context;
        downloadManager = (DownloadManager) mContext.getSystemService(mContext.DOWNLOAD_SERVICE);
        ASNToNameFileInProgress = new File(mContext.getExternalFilesDir("db_data"), "ASNToName.progress");
        ASNToNameFileFinished = new File(mContext.getExternalFilesDir("db_data"), "ASNToName.tsv");

        mOnDownloadFinishedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d(TAG, "onReceive()");
                long extraDownloadId = intent.getExtras().getLong(DownloadManager.EXTRA_DOWNLOAD_ID);
                Uri fileUri = downloadManager.getUriForDownloadedFile(extraDownloadId);
                File downloadedFile = new File(fileUri.getPath());

                if (downloadedFile.getName().equals(ASNToNameFileInProgress.getName())) {
                    Log.d(TAG, "Detected finished download of " + ASNToNameFileInProgress.getName() + " :: renameing to: " + ASNToNameFileFinished.getName());
                    ASNToNameFileInProgress.renameTo(ASNToNameFileFinished);

                } else {
                    Log.d(TAG, "Unmatchable file: " + downloadedFile.getName());
                }
            }
        };
    }

    public String resolve(long asn) throws DownloadNotReadyException {
        String line;
        String prevASNName = "";
        long lineASN = 0;
        if(!ASNToNameFileFinished.exists()) {
            throw new DownloadNotReadyException();
        }
        try {
            InputStream fis = new FileInputStream(ASNToNameFileFinished);
            InputStreamReader isr = new InputStreamReader(fis, Charset.forName("UTF-8"));
            BufferedReader br = new BufferedReader(isr);
            while ((line = br.readLine()) != null) {
                Log.d(TAG, line);
                String[] parts = line.split(" ", 2);
                try {
                    lineASN = Long.parseLong(parts[0]);
                } catch (NumberFormatException e) {
                    continue;
                }
                String ASNName = parts[1];
                if(asn < lineASN) {
                    return prevASNName;
                }
                prevASNName = ASNName;
            }
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }

        return "";
    }

    public void downloadDB(){
        deleteFileIfOlderThan(ASNToNameFileFinished, mRefreshDayLimit);
        if(!ASNToNameFileFinished.exists())
            downloadDB( "http://thyme.apnic.net/current/data-used-autnums", ASNToNameFileInProgress);

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
        ASNToNameFileFinished.delete();
        ASNToNameFileInProgress.delete();
    }

    public BroadcastReceiver getOnDownloadFinishedReceiver() {
        return mOnDownloadFinishedReceiver;
    }
}
