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

import java.util.Collection;

import net.commotionwireless.olsrinfo.datatypes.Neighbor;
import android.content.Context;
import android.net.wifi.WifiManager;
import android.util.Log;

/**
 *
 * ProximityServiceHelper
 *
 */
public class ProximityServiceHelper
{
    private static final String TAG = "ProximityServiceHelper";

    private final WifiManager mWifiManager;
    private final OlsrHelper mOlsrHelper;

    /**
     *
     * @param context
     */
    public ProximityServiceHelper(Context context)
    {
        mWifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        mOlsrHelper = new OlsrHelper(context);
    }

    /**
     *
     * @return
     */
    public boolean startRoutingProtocol()
    {
        return mOlsrHelper.startDaemon();
    }

    /**
     *
     * @return
     */
    public boolean stopRoutingProtocol()
    {
        return mOlsrHelper.stopDaemon();
    }

    /**
     *
     * @return
     */
    public Collection<Neighbor> getPeers()
    {
        return mOlsrHelper.getPeers();
    }

    /**
     * Disable the default wifi interface for this device
     */
    public void disableWifi()
    {
        mWifiManager.setWifiEnabled(false);
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

    /**
     * Enable the default wifi interface for this device
     */
    public void enableWifi()
    {
        // Waiting for interface-restart
        mWifiManager.setWifiEnabled(true);
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
}
