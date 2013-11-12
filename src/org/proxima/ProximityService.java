/**
 * Author: Justin Lewis Salmon <justin2.salmon@live.uwe.ac.uk>
 *
 * This file is part of the Proxima framework.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.proxima;

import java.util.ArrayList;
import java.util.Collection;

import net.commotionwireless.olsrinfo.datatypes.Neighbor;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;
import android.widget.Toast;

/**
 *
 * ProximityService
 *
 */
public class ProximityService extends Service
{
    private static final String TAG = "ProximityService";

    public static final int MSG_TEST = 1;
    public static final int MSG_REGISTER_CLIENT = 2;
    public static final int MSG_UNREGISTER_CLIENT = 3;
    public static final int MSG_GET_PEERS = 4;

    // Keeps track of all current registered clients.
    private final ArrayList<Messenger> mClients = new ArrayList<Messenger>();

    // Target we publish for clients to send messages to IncomingHandler.
    private final Messenger mMessenger = new Messenger(new IncomingHandler());

    private ProximityServiceHelper helper;

    /**
     *
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "Service started");
        android.os.Debug.waitForDebugger();

        helper = new ProximityServiceHelper(this);

        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                NativeTools.unpackResources(getApplicationContext());

//                // TODO refactor this out

                disableWifi();

                String path = getFilesDir().getParent();

                if (Build.MODEL.equalsIgnoreCase("GT-I9505"))
                {
                    NativeTools.runRootCommandGetOutput(getApplicationContext(),
                            path + "/bin/tether start");
                }
                else
                {
                    String iface;
                    String ip;

                    Log.d(TAG, Build.MODEL + " ------------------------------");

                    if (Build.MODEL.equalsIgnoreCase("GT-P7510"))
                    {
                        iface = "eth0";
                        ip = "192.168.2.101";
                        NativeTools.runRootCommandGetOutput(
                                getApplicationContext(), path + "/bin/wifi load");
                    }
                    else
                    {
                        iface = "eth0";
                        ip = "192.168.2.102";
                        NativeTools.runRootCommandGetOutput(
                                getApplicationContext(), path + "/bin/wifi load");
                    }

                    NativeTools.runRootCommandGetOutput(getApplicationContext(),
                            path + "/bin/ifconfig " + iface + " " + ip
                                    + " netmask 255.255.255.0");
                    NativeTools.runRootCommandGetOutput(getApplicationContext(),
                            path + "/bin/ifconfig " + iface + " up");

                    NativeTools.runRootCommandGetOutput(getApplicationContext(),
                            path + "/bin/iwconfig " + iface + " mode ad-hoc");

                    // NativeTools.runRootCommandGetOutput(getApplicationContext(),
                    // path + "/bin/ifconfig " + iface + " down");


                    NativeTools.runRootCommandGetOutput(getApplicationContext(),
                            path + "/bin/iwconfig " + iface + " essid wildfire2");
                    NativeTools.runRootCommandGetOutput(getApplicationContext(),
                            path + "/bin/iwconfig " + iface + " channel 1");
                    NativeTools.runRootCommandGetOutput(getApplicationContext(),
                            path + "/bin/iwconfig " + iface + " commit");
//                    NativeTools.runRootCommandGetOutput(getApplicationContext(),
//                            path + "/bin/ifconfig " + iface + " " + ip
//                                    + " netmask 255.255.255.0");

                    NativeTools.runRootCommandGetOutput(getApplicationContext(),
                            "echo 1 > /proc/sys/net/ipv4/ip_forward");

                    // TODO refactor this out
                }


                try
                {
                    Thread.sleep(3000);
                }
                catch (InterruptedException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

                if (!helper.startRoutingProtocol())
                {
                    Log.e(TAG, "Could not start routing protocol");
                }
            }
        };
        thread.start();

    }

    // TODO refactor this out
    // disable the default wifi interface for this device
    /**
     *
     */
    private void disableWifi()
    {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        wifiManager.setWifiEnabled(false);
        Log.d(TAG, "Wifi disabled!");
        // Waiting for interface-shutdown
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            // nothing
        }

    }

    // TODO refactor this out
    // enable the default wifi interface for this device
    /**
     *
     */
    private void enableWifi()
    {
        WifiManager wifiManager = (WifiManager) getSystemService(Context.WIFI_SERVICE);
        // Waiting for interface-restart
        wifiManager.setWifiEnabled(true);
        try
        {
            Thread.sleep(5000);
        }
        catch (InterruptedException e)
        {
            // nothing
        }
        Log.d(TAG, "Wifi started!");

    }

    /**
     *
     * @see android.app.Service#onStartCommand(android.content.Intent, int, int)
     */
    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        Log.d(TAG, "Received start id " + startId + ": " + intent);
        return Service.START_STICKY;
    }

    /**
     *
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent arg0)
    {
        return mMessenger.getBinder();
    }

    /**
     *
     * IncomingHandler
     *
     */
    class IncomingHandler extends Handler
    { // Handler of incoming messages from clients.
        @Override
        public void handleMessage(Message msg)
        {
            Log.d(TAG, "Received message");

            switch (msg.what)
            {
                case MSG_REGISTER_CLIENT:
                    Log.d(TAG, "Received message MSG_REGISTER_CLIENT");
                    mClients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    Log.d(TAG, "Received message MSG_UNREGISTER_CLIENT");
                    mClients.remove(msg.replyTo);
                    break;
                case MSG_TEST:
                    Log.d(TAG, "Received message MSG_TEST");
                    Toast.makeText(getApplicationContext(),
                            "Test message received", Toast.LENGTH_LONG).show();
                    break;
                case ProximityManager.REQUEST_PEERS:
                    Log.d(TAG, "Received message REQUEST_PEERS");
                    getPeers();
                    break;
                default:
                    super.handleMessage(msg);
            }
        }
    }

    /**
     *
     */
    private void getPeers()
    {
        for (int i = mClients.size() - 1; i >= 0; i--)
        {
            try
            {
                // Send data
                // ArrayList<String> peers = new ArrayList<String>();
                // peers.add("192.168.1.x");
                // peers.add("192.168.1.x");

                Collection<Neighbor> neighbors = helper.getPeers();
                ArrayList<String> peerList = new ArrayList<String>();

                for (Neighbor neighbor : neighbors)
                {
                    peerList.add(neighbor.ipv4Address);
                }

                Bundle bundle = new Bundle();
                bundle.putStringArrayList("peerList", peerList);
                Message message = Message.obtain(null,
                        ProximityManager.RESPONSE_PEERS);
                message.setData(bundle);
                mClients.get(i).send(message);

            }
            catch (RemoteException e)
            {
                // The client is dead. Remove it from the list; we are going
                // through the list from back to front so this is safe to do
                // inside the loop.
                mClients.remove(i);
            }
        }
    }

    /**
     *
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        NativeTools.runRootCommandGetOutput(getApplicationContext(),
                getFilesDir().getParent() + "/bin/tether stop");
        Log.d(TAG, "Service stopped");
    }

}
