package org.ubillos.getspn;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;

/**
 * Created by jav on 2016-10-21.
 */
public class ASNToNameResolver {
    private final File mFile;
    private String TAG = "IPToASNResolver";

    public ASNToNameResolver(File ASNToNameFile) {
        mFile = ASNToNameFile;
    }

    public String resolve(long asn){
        String line;
        String prevASNName = "";
        long lineASN = 0;
        try {
            InputStream fis = new FileInputStream(mFile);
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

}
