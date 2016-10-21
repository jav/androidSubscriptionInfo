package org.ubillos.getspn;

import android.annotation.TargetApi;
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

/**
 * Created by jav on 2016-10-21.
 */
public class IPToASNResolver {
    private final File mFile;
    private String TAG = "IPToASNResolver";

    public IPToASNResolver(File ipToASNFileFinished) {
        mFile = ipToASNFileFinished;
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

    public long resolve(String ip) throws UnknownHostException {
        long ipAsNumber = ipToLong(ip);

        String line;
        try {
            InputStream fis = new FileInputStream(mFile);
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
                    return asn;
                }

            }
        } catch (Exception e) {
            Log.d(TAG, e.toString());
        }

        return 1;
    }
}
