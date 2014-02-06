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
import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/**
 *
 * ProximityService
 *
 * The main background service that responds to client requests for neighbor
 * discovery and retrieval (via Channel object).
 *
 * TODO: This service should be a state machine.
 */
public class ProximityService extends Service
{
    /**
     * The ID tag of this class for use with logging messages
     */
    private static final String TAG = "ProximityService";

    /**
     * Target we publish for clients to send messages to ChannelHandler.
     */
    private Messenger mMessenger;

    /**
     * Simple helper object to simplify this class
     */
    private ProximityServiceHelper mHelper;

    /**
     * Keep track of whether neighbor discovery has been started.
     */
    private boolean mNeighborDiscoveryStarted;

    /**
     * Called when the service is first created
     *
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.d(TAG, "Service started");

        mHelper = new ProximityServiceHelper(this);
        mMessenger = new Messenger(new ProximityServiceHandler(this));
        mNeighborDiscoveryStarted = false;
    }

    /**
     * Called when the service is finishing or being destroyed by the system
     *
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy()
    {
        super.onDestroy();
        Log.d(TAG, "Service stopped");
    }

    /**
     * Called by the system every time a client explicitly starts the service.
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
     * Return the communication channel to this service.
     *
     * @see android.app.Service#onBind(android.content.Intent)
     */
    @Override
    public IBinder onBind(Intent intent)
    {
        Log.d(TAG, "onBind()");
        return mMessenger.getBinder();
    }

    /**
     * Begin the neighbor discovery process. This involves unpacking the
     * necessary binary files and config files, configuring the wireless
     * interface (setting ad-hoc mode) and starting the routing protocol daemon.
     *
     * If the neighbor discovery process started correctly (i.e. we performed
     * the aforementioned steps successfully) then we respond to the client with
     * a DISCOVER_NEIGHBORS_SUCCEEDED message, and additionally broadcast a
     * PROXIMITY_NEIGHBORS_CHANGED_ACTION intent. Otherwise, we respond to the
     * client with a DISCOVER_NEIGHBORS_FAILED message.
     *
     * @param message the DISCOVER_NEIGHBORS message received from the client
     */
    protected void discoverNeighbors(Message message)
    {
        // Neighbor discovery should only be done once
        if (mNeighborDiscoveryStarted) return;

        // Unpack the binaries and config files
        NativeTools.unpackResources(getApplicationContext());

        // Configure the wireless interface
        if (!mHelper.isInterfaceConfigured())
        {
            mHelper.configureWirelessInterface();
        }

        // We want to keep the routing protocol running, if possible
        if (mHelper.isRoutingProtocolStarted())
        {
            replyToMessage(message,
                    ProximityManager.DISCOVER_NEIGHBORS_SUCCEEDED, null);

            // Broadcast that the neighbors have changed
            sendNeighboursChangedBroadcast();
        }
        else
        {
            // Try to start the routing protocol
            if (!mHelper.startRoutingProtocol())
            {
                Log.e(TAG, "Could not start routing protocol");
                replyToMessage(message,
                        ProximityManager.DISCOVER_NEIGHBORS_FAILED, null);
            }
            else
            {
                Log.e(TAG, "Successfully started routing protocol");
                replyToMessage(message,
                        ProximityManager.DISCOVER_NEIGHBORS_SUCCEEDED, null);

                // Broadcast that the neighbors have changed
                sendNeighboursChangedBroadcast();
            }
        }

    }

    /**
     * Obtain a list of neighbors from the routing protocol daemon interface and
     * return it back to the client.
     *
     * @param message the REQUEST_NEIGHBORS message received from the client
     */
    protected void requestNeighbors(Message message)
    {
        Collection<Neighbor> neighbors = mHelper.requestNeighbors();
        ArrayList<String> neighborList = new ArrayList<String>();

        for (Neighbor neighbor : neighbors)
        {
            neighborList.add(neighbor.ipv4Address);
        }

        Bundle data = new Bundle();
        data.putStringArrayList(ProximityManager.EXTRA_NEIGHBOR_LIST,
                neighborList);

        Log.d(TAG, "Sending message RESPONSE_NEIGHBORS");
        replyToMessage(message, ProximityManager.RESPONSE_NEIGHBORS, data);
    }

    /**
     * Reply to a message received from a client. There will be a callback
     * listener in the arg2 parameter, which is provided by the client, so we
     * pass this back.
     *
     * @param message the client message
     * @param what the message subject
     * @param data optional data bundle to deliver to the client
     */
    private void replyToMessage(Message message, int what, Bundle data)
    {
        if (message.replyTo == null) return;

        Message dstMsg = Message.obtain();
        dstMsg.what = what;

        // The callback listener is in arg2
        dstMsg.arg2 = message.arg2;

        // Maybe set some data
        if (data != null) dstMsg.setData(data);

        try
        {
            dstMsg.replyTo = mMessenger;
            message.replyTo.send(dstMsg);
        }
        catch (RemoteException e)
        {
            Log.e(TAG, "TODO: handle RemoteException" + e);
        }
    }

    /**
     * Notify that the neighbors have changed (or are at least requestable)
     */
    public void sendNeighboursChangedBroadcast()
    {
        Intent intent = new Intent();
        intent.setAction(ProximityManager.PROXIMITY_NEIGHBORS_CHANGED_ACTION);
        sendBroadcast(intent);
    }
}
