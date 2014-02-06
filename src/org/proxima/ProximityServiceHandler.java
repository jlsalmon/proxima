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

import android.os.Handler;
import android.os.Message;
import android.util.Log;

/**
 *
 * ProximityServiceHandler
 *
 * Handler of incoming messages from clients.
 */
class ProximityServiceHandler extends Handler
{
    /**
     * The ID tag of this class for use with logging messages
     */
    private static final String TAG = "ProximityServiceHandler";

    /**
     * Weak reference to the service itself
     */
    private final WeakReference<ProximityService> mService;

    /**
     * Constructor
     *
     * @param service the service object reference
     */
    public ProximityServiceHandler(ProximityService service)
    {
        // Store a weak reference so to avoid memory leaks of this class
        mService = new WeakReference<ProximityService>(service);
    }

    /**
     * Called when a message is delivered to this handler.
     *
     * @see android.os.Handler#handleMessage(android.os.Message)
     */
    @Override
    public void handleMessage(Message message)
    {
        Log.d(TAG, "Received message");

        switch (message.what)
        {
            case ProximityManager.DISCOVER_NEIGHBORS:
                Log.d(TAG, "Received message DISCOVER_NEIGHBORS");
                mService.get().discoverNeighbors(message);
                break;

            case ProximityManager.REQUEST_NEIGHBORS:
                Log.d(TAG, "Received message REQUEST_NEIGHBORS");
                mService.get().requestNeighbors(message);
                break;

            default:
                super.handleMessage(message);
        }
    }
}
