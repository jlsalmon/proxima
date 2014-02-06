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
import android.os.Build;
import android.util.Log;

/**
 *
 * ProximityServiceHelper
 *
 * Simple helper class to perform basic tasks related to the ProximityService,
 * such as enabling/disabling wifi and starting/stopping the routing protocol
 * daemon.
 */
public class ProximityServiceHelper
{
    /**
     * The ID tag of this class for use with logging messages
     */
    private static final String TAG = "ProximityServiceHelper";

    /**
     * Used to enable/disable wifi.
     */
    private final WifiManager mWifiManager;

    /**
     * Helper class for olsr tasks such as starting/stopping the daemon and
     * retrieving the current list of neighbors
     */
    private final OlsrHelper mOlsrHelper;

    /**
     * Reference to the parent context
     */
    private final Context mContext;

    /**
     * Keep track of whether we have already configured the wireless interface
     * or not.
     */
    private boolean mInterfaceConfigured;

    /**
     * Constructor
     *
     * @param context the parent context reference
     */
    public ProximityServiceHelper(Context context)
    {
        mWifiManager = (WifiManager) context
                .getSystemService(Context.WIFI_SERVICE);
        mOlsrHelper = new OlsrHelper(context);
        mContext = context;
        mInterfaceConfigured = false;
    }

    /**
     * Start the routing protocol daemon (currently olsrd)
     *
     * @return true if the routing protocol was successfully started, false
     *         otherwise
     */
    public boolean startRoutingProtocol()
    {
        return mOlsrHelper.startDaemon();
    }

    /**
     * Stop the routing protocol
     *
     * @return true if the routing protocol was successfully stopped, false
     *         otherwise
     */
    public boolean stopRoutingProtocol()
    {
        return mOlsrHelper.stopDaemon();
    }

    /**
     * Query the running status of the routing protocol
     *
     * @return true if the routing protocol is currently running, false
     *         otherwise
     */
    public boolean isRoutingProtocolStarted()
    {
        return mOlsrHelper.isDaemonRunning();
    }

    /**
     * Request the current list of neighbors from the routing protocol interface
     *
     * @return the current neighbor list
     */
    public Collection<Neighbor> requestNeighbors()
    {
        return mOlsrHelper.requestNeighbors();
    }

    /**
     * Disable the default wifi interface for this device
     */
    public void disableWifi()
    {
        mWifiManager.setWifiEnabled(false);
        Log.d(TAG, "Wifi disabled!");

        // Wait for interface-shutdown
        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
        }

    }

    /**
     * Enable the default wifi interface for this device
     */
    public void enableWifi()
    {
        // Wait for interface-restart
        mWifiManager.setWifiEnabled(true);
        try
        {
            Thread.sleep(5000);
        }
        catch (InterruptedException e)
        {
        }
        Log.d(TAG, "Wifi enabled!");
    }

    /**
     * Configure the wireless interface to ad-hoc mode.
     *
     * TODO: don't set fixed IP address TODO: use edify script for speed
     */
    public void configureWirelessInterface()
    {
        disableWifi();

        String path = mContext.getFilesDir().getParent();

        if (Build.MODEL.equalsIgnoreCase("GT-I9505"))
        {
            NativeTools.runRootCommandGetOutput(mContext, path
                    + "/bin/tether start");
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
                NativeTools.runRootCommandGetOutput(mContext, path
                        + "/bin/wifi load");
            }
            else
            {
                iface = "wlan0";
                ip = "192.168.2.102";
                NativeTools.runRootCommandGetOutput(mContext, path
                        + "/bin/wifi load");
            }

            NativeTools.runRootCommandGetOutput(mContext, path
                    + "/bin/ifconfig " + iface + " " + ip
                    + " netmask 255.255.255.0");
            NativeTools.runRootCommandGetOutput(mContext, path
                    + "/bin/ifconfig " + iface + " up");
            NativeTools.runRootCommandGetOutput(mContext, path
                    + "/bin/iwconfig " + iface + " mode ad-hoc");

            // NativeTools.runRootCommandGetOutput(mContext,
            // path + "/bin/ifconfig " + iface + " down");

            NativeTools.runRootCommandGetOutput(mContext, path
                    + "/bin/iwconfig " + iface + " essid AndroidTether");
            NativeTools.runRootCommandGetOutput(mContext, path
                    + "/bin/iwconfig " + iface + " channel 1");
            NativeTools.runRootCommandGetOutput(mContext, path
                    + "/bin/iwconfig " + iface + " commit");

            // NativeTools.runRootCommandGetOutput(mContext,
            // path + "/bin/ifconfig " + iface + " " + ip
            // + " netmask 255.255.255.0");

            NativeTools.runRootCommandGetOutput(mContext,
                    "echo 1 > /proc/sys/net/ipv4/ip_forward");

        }

        mInterfaceConfigured = true;
    }

    /**
     * Query the configuration state of the wireless interface.
     *
     * @return true if the interface is configured, false otherwise
     */
    public boolean isInterfaceConfigured()
    {
        return mInterfaceConfigured;
    }
}
