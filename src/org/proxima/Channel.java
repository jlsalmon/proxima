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

import org.proxima.ProximityManager.PeerListListener;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Messenger;
import android.os.RemoteException;
import android.util.Log;

/**
 *
 * Channel
 *
 */
public class Channel
{
    private static final String TAG = "Channel";

    private final Context mContext;
    private final ChannelListener mChannelListener;
    private Messenger mService = null;
    private final Messenger mMessenger;

    private boolean mIsServiceBound;

    /*
     * TODO: allow multiple callback listeners
     */
    private PeerListListener mPeerListListener;

    /**
     *
     * @param context
     */
    public Channel(Context context, ChannelListener channelListener)
    {
        mContext = context;
        mChannelListener = channelListener;
        mMessenger = new Messenger(new IncomingHandler(this));
    }

    /**
     *
     */
    public void connect()
    {
        doBindService();
    }

    /**
     *
     * @param what
     * @param mPeerListListener
     */
    public void sendMessage(int what, PeerListListener listener)
    {
        if (mIsServiceBound)
        {
            if (mService != null)
            {
                this.mPeerListListener = listener;

                try
                {
                    Message message = Message.obtain(null, what, 0, 0);
                    message.replyTo = mMessenger;
                    mService.send(message);
                    Log.d(TAG, "Message sent");
                }
                catch (RemoteException e)
                {
                    Log.e(TAG, e.toString());
                }
            }
        }
        else
        {
            Log.e(TAG, "Service not bound");
        }
    }

    /**
     *
     * IncomingHandler
     *
     */
    static class IncomingHandler extends Handler
    {
        /**
         * Why the WeakReference? See
         * <a>http://stackoverflow.com/questions/11407943
         * /this-handler-class-should-
         * be-static-or-leaks-might-occur-incominghandler</a>
         */
        private final WeakReference<Channel> mChannel;

        /**
         *
         * @param channel
         */
        public IncomingHandler(Channel channel)
        {
            mChannel = new WeakReference<Channel>(channel);
        }

        /**
         *
         * @see android.os.Handler#handleMessage(android.os.Message)
         */
        @Override
        public void handleMessage(Message message)
        {
            switch (message.what)
            {
                case ProximityManager.RESPONSE_PEERS:
                    Log.d(TAG, "Received RESPONSE_PEERS");
                    Bundle bundle = message.getData();
                    ArrayList<String> peers = bundle
                            .getStringArrayList("peerList");
                    mChannel.get().getPeerListListener()
                            .onPeerListAvailable(peers);
                    break;
                default:
                    super.handleMessage(message);
            }
        }
    }

    /**
     *
     */
    private final ServiceConnection mConnection = new ServiceConnection()
    {
        /**
         *
         * @see android.content.ServiceConnection#onServiceConnected(android.content.ComponentName,
         *      android.os.IBinder)
         */
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            mService = new Messenger(service);
            Log.d(TAG, "Service connected");

            try
            {
                Message message = Message.obtain(null,
                        ProximityService.MSG_REGISTER_CLIENT);
                message.replyTo = mMessenger;
                mService.send(message);

                Intent intent = new Intent();
                intent.setAction(ProximityManager.PROXIMITY_STATE_CHANGED_ACTION);
                intent.putExtra(ProximityManager.EXTRA_PROXIMITY_STATE,
                        ProximityManager.PROXIMITY_STATE_ENABLED);
                mContext.sendBroadcast(intent);
            }
            catch (RemoteException e)
            {
                // In this case the service has crashed before we could even do
                // anything with it
                Log.e(TAG, e.toString());
                mChannelListener.onChannelDisconnected();
            }
        }

        /**
         *
         * @see android.content.ServiceConnection#onServiceDisconnected(android.content.ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName className)
        {
            // This is called when the connection with the service has been
            // unexpectedly disconnected - process crashed.
            mService = null;
            mChannelListener.onChannelDisconnected();
            Log.d(TAG, "Service disconnected");
        }
    };

    /**
     *
     */
    void doBindService()
    {
        mContext.bindService(new Intent(mContext, ProximityService.class),
                mConnection, Context.BIND_AUTO_CREATE);
        mIsServiceBound = true;
        Log.d(TAG, "Binding to service");
    }

    /**
     *
     */
    void doUnbindService()
    {
        if (mIsServiceBound)
        {
            // If we have received the service, and hence registered with it,
            // then now is the time to unregister.
            if (mService != null)
            {
                try
                {
                    Message message = Message.obtain(null,
                            ProximityService.MSG_UNREGISTER_CLIENT);

                    message.replyTo = mMessenger;
                    mService.send(message);
                }
                catch (RemoteException e)
                {
                    // There is nothing special we need to do if the service has
                    // crashed.
                }
            }

            // Detach our existing connection.
            mContext.unbindService(mConnection);
            mIsServiceBound = false;
            Log.d(TAG, "Unbinding from service");
        }
    }

    /**
     *
     * ChannelListener
     *
     */
    public interface ChannelListener
    {
        public void onChannelDisconnected();
    }

    /**
     *
     * @return
     */
    public PeerListListener getPeerListListener()
    {
        return mPeerListListener;
    }
}
