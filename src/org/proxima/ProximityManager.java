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
 * This is the class that users of this API will interact with to discover
 * neighbors in proximity.
 *
 * This API takes inspiration from the Android Wi-Fi Peer-to-Peer API:
 * <a>http://developer
 * .android.com/reference/android/net/wifi/p2p/package-summary.html</a>
 */
public class ProximityManager
{
    /**
     * The ID tag of this class for use with logging messages
     */
    private static final String TAG = "ProximityManager";

    /**
     * Broadcast intent action to indicate whether Wi-Fi p2p is enabled or
     * disabled.
     */
    public static final String PROXIMITY_STATE_CHANGED_ACTION =
            "org.proxima.STATE_CHANGED";

    /**
     * Broadcast intent action indicating that the available neighbor list has
     * changed.
     */
    public static final String PROXIMITY_NEIGHBORS_CHANGED_ACTION =
            "org.proxima.NEIGHBORS_CHANGED";

    /**
     * The lookup key for an int that indicates whether proximity functionality
     * is enabled or disabled.
     */
    public static final String EXTRA_PROXIMITY_STATE = "proximityState";

    /**
     * The lookup key for the new neighbor list when
     * PROXIMITY_NEIGHBORS_CHANGED_ACTION broadcast is sent.
     */
    public static final String EXTRA_NEIGHBOR_LIST = "neighborList";

    /**
     * Indicates that proximity functionality is enabled.
     */
    public static final int PROXIMITY_STATE_ENABLED = 0;

    /**
     * Indicates that proximity functionality is disabled.
     */
    public static final int PROXIMITY_STATE_DISABLED = 1;

    /**
     * Action key for neighbor discovery
     */
    public static final int DISCOVER_NEIGHBORS = 1;

    /**
     * Indicates that neighbor discovery failed
     */
    public static final int DISCOVER_NEIGHBORS_FAILED = 2;

    /**
     * Indicates that neighbor discovery succeeded
     */
    public static final int DISCOVER_NEIGHBORS_SUCCEEDED = 3;

    /**
     * Action key for neighbor request
     */
    public static final int REQUEST_NEIGHBORS = 4;

    /**
     * Response key for a neighbor request
     */
    public static final int RESPONSE_NEIGHBORS = 5;

    /**
     * The singleton instance that will be returned with getInstance().
     */
    private static final ProximityManager mInstance = new ProximityManager();

    /**
     * Intentionally made private to keep this class a singleton
     */
    private ProximityManager()
    {}

    /**
     * @return the singleton instance of this class
     */
    public static ProximityManager getInstance()
    {
        return mInstance;
    }

    /**
     * Initialize the proximity manager (connect to the proximity service)
     *
     * @param context the client context
     * @param channelListener the callback listener to be notified on channel
     *            connection/disconnection
     *
     * @return a Channel object to be used with future API requests
     */
    public Channel initialize(Context context, ChannelListener channelListener)
    {
        Channel channel = new Channel(context, channelListener);
        channel.connect();
        return channel;
    }

    /**
     * Start the neighbor discovery process
     *
     * @param channel the client channel instance
     * @param listener the client callback listener to be notified on
     *            success/failure
     */
    public void discoverNeighbors(Channel channel, ActionListener listener)
    {
        Log.d(TAG, "Sending message DISCOVER_NEIGHBORS");
        channel.sendMessage(DISCOVER_NEIGHBORS, 0,
                channel.putListener(listener));
    }

    /**
     * Request the current list of neighbors
     *
     * @param channel the client channel instance
     * @param listener the client callback listener to be notified when the list
     *            of neighbors is available
     */
    public void requestNeighbors(Channel channel, NeighborListListener listener)
    {
        Log.d(TAG, "Sending message REQUEST_NEIGHBORS");
        channel.sendMessage(REQUEST_NEIGHBORS, 0, channel.putListener(listener));
    }

    /**
     * ActionListener
     *
     * Callback interface for use with API method calls.
     */
    public interface ActionListener
    {
        /**
         * Called when the requested action completed successfully.
         */
        public void onSuccess();

        /**
         * Called when the requested action failed to complete successfully.
         *
         * @param reason the failure code
         */
        public void onFailure(int reason);
    }

    /**
     * NeighborListListener
     *
     * Callback interface for use with requestNeighbors().
     */
    public interface NeighborListListener
    {
        /**
         * Called when the list of neighbors is available.
         *
         * @param neighbors the list of neighbors
         */
        public void onNeighborsAvailable(ArrayList<String> neighbors);
    }
}
