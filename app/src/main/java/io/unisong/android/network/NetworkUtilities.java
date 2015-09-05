package io.unisong.android.network;

import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.Enumeration;

/**
 * Created by ezturner on 3/2/2015.
 */
public class NetworkUtilities {
    private final static String LOG_TAG = NetworkUtilities.class.getSimpleName();
    public final static String EC2_INSTANCE = "http://ec2-52-25-84-220.us-west-2.compute.amazonaws.com:8000";

    //Returns the IP address of the local interface. Code is from online.
    public static String getLocalIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress()) {
                        return inetAddress.getHostAddress().toString();
                    }
                }
            }
        } catch (SocketException ex) {
            Log.e(LOG_TAG , ex.toString());
        }
        return null;
    }


    //Returns the broadcast IP address for the current network
    //TODO: Implement exception handling
    public static InetAddress getBroadcastAddress(){
        try {
            System.setProperty("java.net.preferIPv4Stack", "true");
            for (Enumeration<NetworkInterface> niEnum = NetworkInterface.getNetworkInterfaces(); niEnum.hasMoreElements(); ) {
                NetworkInterface ni = niEnum.nextElement();
                if (!ni.isLoopback()) {
                    for (InterfaceAddress interfaceAddress : ni.getInterfaceAddresses()) {
                        if (interfaceAddress.getBroadcast() != null) {
                            return Inet4Address.getByName(interfaceAddress.getBroadcast().toString().substring(1));
                        }
                    }
                }
            }
        } catch (SocketException e){
            Log.e(LOG_TAG , e.toString());
        } catch(UnknownHostException e){
            Log.e(LOG_TAG , e.toString());
        }
        return null;
    }

    //Combines two arrays into one, from stackOverflow
    public static byte[] combineArrays(byte[] a, byte[] b){
        int aLen = a.length;
        int bLen = b.length;
        byte[] c= new byte[aLen+bLen];
        System.arraycopy(a, 0, c, 0, aLen);
        System.arraycopy(b, 0, c, aLen, bLen);
        return c;
    }

    public static void readFromStream(InputStream stream , byte[] data) throws IOException{
        int res = 0;

        while(res < data.length){
            res += stream.read(data , res , (data.length - res));
        }
    }

    /**
     * Decodes a string from a byte array
     * @param data the raw data
     * @param index the index at which we start
     */
    public static String decodeString(byte[] data , int index){
        //TODO: implement these methods.
        return "";
    }

    public static Long decodeLong(byte[] data , int index){
        return 21l;
    }

    public static int decodeInt(byte[] data , int index){
        return 2;
    }
}
