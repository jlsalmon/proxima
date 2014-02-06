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

import net.commotionwireless.olsrinfo.JsonInfo;
import net.commotionwireless.olsrinfo.datatypes.Neighbor;
import net.commotionwireless.olsrinfo.datatypes.OlsrDataDump;
import android.content.Context;
import android.os.Build;
import android.util.Log;

/**
 *
 * OlsrHelper
 *
 * This class helps with operations related to the olsr routing protocol.
 */
public class OlsrHelper
{
    /**
     *
     */
    private static final String TAG = "OlsrHelper";

    /**
     *
     */
    private final Context mContext;

    /**
     *
     */
    private Process mOlsrProcess;

    /**
     *
     */
    private final JsonInfo mJsonInfo;

    /**
     *
     * @param context
     */
    public OlsrHelper(Context context)
    {
        mContext = context;
        mJsonInfo = new JsonInfo();
    }

    /**
     *
     * @return
     */
    public boolean isDaemonRunning()
    {
        boolean running = false;

        try
        {
            running = NativeTools.isProcessRunning("olsrd");
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        return running;
    }

    /**
     *
     * @return
     */
    public boolean startDaemon()
    {
        String path = mContext.getFilesDir().getParent();

        String iface;
        if (Build.MODEL.equalsIgnoreCase("GT-I9505"))
        {
            iface = "wlan0";
        }
        else
        {
            iface = "eth0";
        }

        // HACK: modify LD_LIBRARY_PATH to olsrd can find plugins
        String command = "LD_LIBRARY_PATH=" + path + "/bin:$LD_LIBRARY_PATH; "
                + path + "/bin/olsrd" + " -f " + path + "/conf/olsrd.conf"
                // + "/data/data/org.span/conf/olsrd.conf"
                + " -i " + iface + " -d 2";

        // /home/jussy/.android/platform.jks

        stopDaemon();
        mOlsrProcess = NativeTools
                .runRootCommandInBackground(mContext, command);

        try
        {
            Thread.sleep(1000);
        }
        catch (InterruptedException e)
        {
            e.printStackTrace();
        }

        return mOlsrProcess != null && isDaemonRunning();
    }

    /**
     *
     * @return
     */
    public boolean stopDaemon()
    {
        try
        {
            if (mOlsrProcess != null)
            {
                mOlsrProcess = null;
            }

            NativeTools.killProcess(mContext, "olsrd");
        }
        catch (Exception e)
        {
            Log.e(TAG, e.toString());
        }

        return true;
    }

    /**
     *
     * @return
     */
    public Collection<Neighbor> requestNeighbors()
    {
        OlsrDataDump dump = null;

        try
        {
            OlsrInfoThread thread = new OlsrInfoThread(
                    "/neighbors/links/interfaces");
            thread.start();
            thread.join();
            dump = thread.dump;
        }
        catch (InterruptedException e)
        {
            Log.e(TAG, e.toString());
        }

        return dump.neighbors;
    }

    /**
     *
     * OlsrInfoThread
     *
     */
    private class OlsrInfoThread extends Thread
    {
        private final String request;
        private OlsrDataDump dump;

        /**
         *
         * @param request
         */
        public OlsrInfoThread(String request)
        {
            this.request = request;

        }

        /**
         *
         * @see java.lang.Thread#run()
         */
        @Override
        public void run()
        {
            dump = mJsonInfo.parseCommand(request);
            Log.d(TAG, dump.toString());
        }
    }
}
