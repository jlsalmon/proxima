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

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;

import net.commotionwireless.olsrinfo.datatypes.Neighbor;
import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/**
 *
 * ProximityService
 *
 */
public class ProximityService extends Service
{
    private static final String TAG = "ProximityService";

    public static final int MSG_REGISTER_CLIENT = 2;
    public static final int MSG_UNREGISTER_CLIENT = 3;
    public static final int MSG_GET_PEERS = 4;

    // Keeps track of all current registered clients.
    private ArrayList<Messenger> mClients;

    // Target we publish for clients to send messages to IncomingHandler.
    private Messenger mMessenger;

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
        mClients = new ArrayList<Messenger>();
        mMessenger = new Messenger(new IncomingHandler(this));

        Thread thread = new Thread()
        {
            @Override
            public void run()
            {
                NativeTools.unpackResources(getApplicationContext());

                // // TODO refactor this out

                helper.disableWifi();

                String path = getFilesDir().getParent();

                if (Build.MODEL.equalsIgnoreCase("GT-I9505"))
                {
                    NativeTools
                            .runRootCommandGetOutput(getApplicationContext(),
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
                                getApplicationContext(), path
                                        + "/bin/wifi load");
                    }
                    else
                    {
                        iface = "eth0";
                        ip = "192.168.2.102";
                        NativeTools.runRootCommandGetOutput(
                                getApplicationContext(), path
                                        + "/bin/wifi load");
                    }

                    NativeTools.runRootCommandGetOutput(
                            getApplicationContext(), path + "/bin/ifconfig "
                                    + iface + " " + ip
                                    + " netmask 255.255.255.0");
                    NativeTools.runRootCommandGetOutput(
                            getApplicationContext(), path + "/bin/ifconfig "
                                    + iface + " up");

                    NativeTools.runRootCommandGetOutput(
                            getApplicationContext(), path + "/bin/iwconfig "
                                    + iface + " mode ad-hoc");

                    // NativeTools.runRootCommandGetOutput(getApplicationContext(),
                    // path + "/bin/ifconfig " + iface + " down");

                    NativeTools.runRootCommandGetOutput(
                            getApplicationContext(), path + "/bin/iwconfig "
                                    + iface + " essid wildfire2");
                    NativeTools.runRootCommandGetOutput(
                            getApplicationContext(), path + "/bin/iwconfig "
                                    + iface + " channel 1");
                    NativeTools.runRootCommandGetOutput(
                            getApplicationContext(), path + "/bin/iwconfig "
                                    + iface + " commit");
                    // NativeTools.runRootCommandGetOutput(getApplicationContext(),
                    // path + "/bin/ifconfig " + iface + " " + ip
                    // + " netmask 255.255.255.0");

                    NativeTools.runRootCommandGetOutput(
                            getApplicationContext(),
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
     * Handler of incoming messages from clients.
     */
    static class IncomingHandler extends Handler
    {
        private final WeakReference<ProximityService> mService;

        /**
         *
         * @param channel
         */
        public IncomingHandler(ProximityService service)
        {
            mService = new WeakReference<ProximityService>(service);
        }

        @Override
        public void handleMessage(Message msg)
        {
            Log.d(TAG, "Received message");
            ArrayList<Messenger> clients = mService.get().getClients();

            switch (msg.what)
            {
                case MSG_REGISTER_CLIENT:
                    Log.d(TAG, "Received message MSG_REGISTER_CLIENT");
                    clients.add(msg.replyTo);
                    break;
                case MSG_UNREGISTER_CLIENT:
                    Log.d(TAG, "Received message MSG_UNREGISTER_CLIENT");
                    clients.remove(msg.replyTo);
                    break;
                case ProximityManager.REQUEST_PEERS:
                    Log.d(TAG, "Received message REQUEST_PEERS");
                    mService.get().getPeers();
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
     * @return
     */
    protected ArrayList<Messenger> getClients()
    {
        return mClients;
    }
}
