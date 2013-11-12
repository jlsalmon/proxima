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

import org.proxima.Channel.ChannelListener;

import android.content.Context;
import android.util.Log;

/**
 *
 * ProximityManager
 *
 * This API is based heavily on the Android Wi-Fi Peer-to-Peer API:
 * <a>http://developer
 * .android.com/reference/android/net/wifi/p2p/package-summary.html</a>
 *
 */
public class ProximityManager
{
    private static final String TAG = "ProximityManager";

    public static final String PROXIMITY_STATE_CHANGED_ACTION = "org.proxima.STATE_CHANGED";
    public static final String PROXIMITY_PEERS_CHANGED_ACTION = "org.proxima.PEERS_CHANGED";

    public static final String EXTRA_PROXIMITY_STATE = "proximity_state";

    public static final int PROXIMITY_STATE_ENABLED = 0;

    public static final int REQUEST_PEERS = 0;
    public static final int RESPONSE_PEERS = 1;

    /**
     *
     */
    public ProximityManager()
    {

    }

    /**
     * Initialize the proximity manager (connect to the proximity service)
     *
     * Returns a Channel instance that is used for future requests
     *
     * @param context
     * @param channelListener
     * @return
     */
    public Channel initialize(Context context, ChannelListener channelListener)
    {
        Channel channel = new Channel(context, channelListener);
        channel.connect();
        return channel;
    }

    /**
     *
     * @param channel
     * @param listener
     */
    public void getPeers(Channel channel, PeerListListener listener)
    {
        Log.d(TAG, "Sending message REQUEST_PEERS");
        channel.sendMessage(REQUEST_PEERS, listener);
    }

    /**
     *
     * ActionListener
     *
     * @author Justin Lewis Salmon <mccrustin@gmail.com>
     *
     */
    public interface ActionListener
    {
        /**
         *
         */
        public void onSuccess();

        /**
         *
         * @param reason
         */
        public void onFailure(int reason);
    }

    /**
     *
     * PeerListListener
     *
     * @author Justin Lewis Salmon <mccrustin@gmail.com>
     *
     */
    public interface PeerListListener
    {
        /**
         *
         * @param peers
         */
        public void onPeerListAvailable(ArrayList<String> peers);
    }
}
