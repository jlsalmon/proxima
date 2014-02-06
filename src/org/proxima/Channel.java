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
import java.util.HashMap;

import org.proxima.ProximityManager.ActionListener;
import org.proxima.ProximityManager.NeighborListListener;

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
    /**
     *
     */
    private static final String TAG = "Channel";

    /**
     *
     */
    private final Context mContext;

    /**
     *
     */
    private ChannelListener mChannelListener;

    /**
     *
     */
    private Messenger mDstMessenger = null;

    /**
     *
     */
    private final Messenger mSrcMessenger;

    /**
    *
    */
    private final Handler mSrcHandler;

    /**
     *
     */
    private boolean mIsServiceConnected;

    /**
     *
     */
    private final static int INVALID_LISTENER_KEY = 0;

    /**
    *
    */
    private final static int CHANNEL_DISCONNECTED = 0;

    /**
     *
     */
    private final HashMap<Integer, Object> mListenerMap = new HashMap<Integer, Object>();

    /**
     *
     */
    private final Object mListenerMapLock = new Object();

    /**
     *
     */
    private int mListenerKey = 0;

    /**
     *
     * @param context
     */
    public Channel(Context context, ChannelListener channelListener)
    {
        mContext = context;
        mChannelListener = channelListener;
        mSrcHandler = new ChannelHandler(this);
        mSrcMessenger = new Messenger(mSrcHandler);
    }

    /**
     *
     */
    public void connect()
    {
        // Start the service (if it hasn't already been)
        mContext.startService(new Intent(mContext, ProximityService.class));
        // Bind to the service, so we can use IPC
        bindService();
    }

    /**
     *
     */
    public void disconnect()
    {
        unbindService();
    }

    /**
     *
     * @param what
     * @param arg1
     * @param arg2 the listener key
     */
    public void sendMessage(int what, int arg1, int arg2)
    {
        if (mIsServiceConnected)
        {
            if (mDstMessenger != null)
            {
                Message message = Message.obtain();
                message.what = what;
                message.arg1 = arg1;
                message.arg2 = arg2;

                try
                {
                    message.replyTo = mSrcMessenger;
                    mDstMessenger.send(message);
                    Log.d(TAG, "Message sent");
                }
                catch (RemoteException e)
                {
                    // We are disconnected.
                    Log.e(TAG, e.toString());

                    Message msg = mSrcHandler
                            .obtainMessage(CHANNEL_DISCONNECTED);
                    msg.obj = this;
                    msg.replyTo = mDstMessenger;
                    mSrcHandler.sendMessage(msg);
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
     * ChannelHandler
     *
     */
    class ChannelHandler extends Handler
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
        public ChannelHandler(Channel channel)
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
            Object listener = mChannel.get().getListener(message.arg2);

            switch (message.what)
            {
                case Channel.CHANNEL_DISCONNECTED:
                    Log.d(TAG, "Received CHANNEL_DISCONNECTED");
                    if (mChannelListener != null)
                    {
                        mChannelListener.onChannelDisconnected();
                        mChannelListener = null;
                    }
                    break;

                case ProximityManager.DISCOVER_NEIGHBORS_FAILED:
                    Log.d(TAG, "Received DISCOVER_NEIGHBORS_FAILED");
                    if (listener != null)
                    {
                        ((ActionListener) listener).onFailure(message.arg1);
                    }
                    break;

                case ProximityManager.DISCOVER_NEIGHBORS_SUCCEEDED:
                    Log.d(TAG, "Received DISCOVER_NEIGHBORS_SUCCEEDED");
                    if (listener != null)
                    {
                        ((ActionListener) listener).onSuccess();
                    }
                    break;

                case ProximityManager.RESPONSE_NEIGHBORS:
                    Log.d(TAG, "Received RESPONSE_NEIGHBORS");
                    Bundle bundle = message.getData();
                    ArrayList<String> neighbors = bundle
                            .getStringArrayList(ProximityManager.EXTRA_NEIGHBOR_LIST);

                    // Call the listener
                    if (listener != null)
                    {
                        ((NeighborListListener) listener)
                                .onNeighborsAvailable(neighbors);
                    }
                    break;

                default:
                    super.handleMessage(message);
            }
        }
    }

    /**
     *
     */
    private final ServiceConnection mServiceConnection = new ServiceConnection()
    {
        /**
         *
         * @see android.content.ServiceConnection
         *      #onServiceConnected(android.content.ComponentName,
         *      android.os.IBinder)
         */
        @Override
        public void onServiceConnected(ComponentName className, IBinder service)
        {
            Log.d(TAG, "Service connected");
            mDstMessenger = new Messenger(service);
            mIsServiceConnected = true;

            // TODO: to notify the client that we have connected to the service,
            // should we send a broadcast intent or call a callback method? And
            // what about "state enabled"?

            // Notify that the proximity functionality is now available
            Intent intent = new Intent();
            intent.setAction(ProximityManager.PROXIMITY_STATE_CHANGED_ACTION);
            intent.putExtra(ProximityManager.EXTRA_PROXIMITY_STATE,
                    ProximityManager.PROXIMITY_STATE_ENABLED);
            mContext.sendBroadcast(intent);

            // Notify the client application
            if (mChannelListener != null)
            {
                mChannelListener.onChannelConnected();
            }
        }

        /**
         *
         * @see android.content.ServiceConnection
         *      #onServiceDisconnected(android.content.ComponentName)
         */
        @Override
        public void onServiceDisconnected(ComponentName className)
        {
            // This is called when the connection with the service has been
            // unexpectedly disconnected - process crashed.
            mDstMessenger = null;
            mIsServiceConnected = false;

            // Notify the client application of the error
            if (mChannelListener != null)
            {
                mChannelListener.onChannelDisconnected();
                mChannelListener = null;
            }

            Log.d(TAG, "Service disconnected");
        }
    };

    /**
     *
     */
    private boolean bindService()
    {
        if (mContext.bindService(new Intent(mContext, ProximityService.class),
                mServiceConnection, Context.BIND_AUTO_CREATE))
        {
            Log.d(TAG, "Bound to service");
            return true;
        }
        else
        {
            Log.d(TAG, "Failed to bind to service");
            return false;
        }
    }

    /**
     *
     */
    private boolean unbindService()
    {
        if (mIsServiceConnected)
        {
            // Detach our existing connection.
            mContext.unbindService(mServiceConnection);
            mIsServiceConnected = false;
            Log.d(TAG, "Unbound from service");
        }

        return true;
    }

    /**
     *
     * ChannelListener
     *
     */
    public interface ChannelListener
    {
        /**
         *
         */
        public void onChannelConnected();

        /**
         *
         */
        public void onChannelDisconnected();
    }

    /**
     *
     * @param listener
     * @return
     */
    protected int putListener(Object listener)
    {
        if (listener == null) return INVALID_LISTENER_KEY;
        int key;
        synchronized (mListenerMapLock)
        {
            do
            {
                key = mListenerKey++;
            }
            while (key == INVALID_LISTENER_KEY);
            mListenerMap.put(key, listener);
        }
        return key;
    }

    /**
     *
     * @param key
     * @return
     */
    protected Object getListener(int key)
    {
        if (key == INVALID_LISTENER_KEY) return null;
        synchronized (mListenerMapLock)
        {
            return mListenerMap.remove(key);
        }
    }
}
