package org.proxima;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;

import android.content.Context;
import android.util.Log;

public class NativeTools
{
    private static final String TAG = "NativeTools";

    // private static final String FILES_DIR = "/data/data/org.proxima";

    public static void unpackResources(Context context)
    {
        String path = context.getFilesDir().getParent();

        if (copyFile(context, path + "/bin/iwconfig", R.raw.iwconfig))
            chmod(context, path + "/bin/iwconfig", "0755");

        if (copyFile(context, path + "/bin/ifconfig", R.raw.ifconfig_old))
            chmod(context, path + "/bin/ifconfig", "0755");

        if (copyFile(context, path + "/bin/iptables", R.raw.iptables))
            chmod(context, path + "/bin/iptables", "0755");

        if (copyFile(context, path + "/bin/wifi", R.raw.wifi))
            chmod(context, path + "/bin/wifi", "0755");

        if (copyFile(context, path + "/bin/tcpdump", R.raw.tcpdump))
            chmod(context, path + "/bin/tcpdump", "0755");

        if (copyFile(context, path + "/bin/dnsmasq", R.raw.dnsmasq))
            chmod(context, path + "/bin/dnsmasq", "0755");

        if (copyFile(context, path + "/bin/tether", R.raw.tether))
            chmod(context, path + "/bin/tether", "0755");

        if (copyFile(context, path + "/conf/tether.edify", R.raw.tether_edify))
            chmod(context, path + "/conf/tether.edify", "0755");

        if (copyFile(context, path + "/bin/olsrd", R.raw.olsrd))
            chmod(context, path + "/bin/olsrd", "0755");

        if (copyFile(context, path + "/conf/olsrd.conf", R.raw.olsrd_conf_in))
            chmod(context, path + "/conf/olsrd.conf", "0644");

        if (copyFile(context, path + "/bin/olsrd_txtinfo.so.0.1",
                R.raw.olsrd_txtinfo_so_0_1))
            chmod(context, path + "/bin/olsrd_txtinfo.so.0.1", "0755");

        if (copyFile(context, path + "/bin/olsrd_jsoninfo.so.0.0",
                R.raw.olsrd_jsoninfo_so_0_0))
            chmod(context, path + "/bin/olsrd_jsoninfo.so.0.0", "0755");

        if (copyFile(context, path + "/bin/olsrd_nameservice.so.0.3",
                R.raw.olsrd_nameservice_so_0_3))
            chmod(context, path + "/bin/olsrd_nameservice.so.0.3", "0755");
    }

    public static InetAddress getIpAddress()
    {
        try
        {

            InetAddress inetAddress = null;
            InetAddress myAddr = null;

            for (Enumeration<NetworkInterface> networkInterface = NetworkInterface
                    .getNetworkInterfaces(); networkInterface.hasMoreElements();)
            {

                NetworkInterface singleInterface = networkInterface
                        .nextElement();

                for (Enumeration<InetAddress> IpAddresses = singleInterface
                        .getInetAddresses(); IpAddresses.hasMoreElements();)
                {
                    inetAddress = IpAddresses.nextElement();

                    if (!inetAddress.isLoopbackAddress()
                            && (singleInterface.getDisplayName().contains(
                                    "wlan0") || singleInterface
                                    .getDisplayName().contains("eth0")))
                    {

                        myAddr = inetAddress;
                    }
                }
            }
            return myAddr;

        }
        catch (SocketException ex)
        {
            Log.e(TAG, ex.toString());
        }
        return null;
    }

    public static InetAddress getBroadcast(InetAddress inetAddr)
    {

        NetworkInterface temp;
        InetAddress iAddr = null;
        try
        {
            temp = NetworkInterface.getByInetAddress(inetAddr);
            List<InterfaceAddress> addresses = temp.getInterfaceAddresses();

            for (InterfaceAddress inetAddress : addresses)

                iAddr = inetAddress.getBroadcast();
            Log.d(TAG, "iAddr=" + iAddr);
            return iAddr;

        }
        catch (SocketException e)
        {

            e.printStackTrace();
            Log.d(TAG, "getBroadcast" + e.getMessage());
        }
        return null;
    }

    public static int runCommand(String command)
    {
        try
        {
            Process process = Runtime.getRuntime().exec(command);
            return process.waitFor();
        }
        catch (Exception e)
        {
            Log.e(TAG, e.toString());
            return -1;
        }
    }

    public static int runRootCommand(Context context, String command)
    {
        return runCommand(prepareRootCommandScript(context, command));
    }

    public static String chmod(Context context, String path, String mode)
    {
        return runCommandGetOutput("chmod " + mode + " " + path);
    }

    /**
     *
     * @param context
     * @param filename
     * @param resource
     * @return true if the file was copied, false otherwise
     */
    private static boolean copyFile(Context context, String filename,
            int resource)
    {
        File outFile = new File(filename);
        outFile.getParentFile().mkdirs();

        InputStream is = context.getResources().openRawResource(resource);
        int inFileLength = 0;

        try
        {
            inFileLength = is.available();
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        Log.d(TAG, "in: " + inFileLength + " out: " + outFile.length());

        // Don't overwrite existing files unless they have changed
        if (outFile.exists() && (outFile.length() == inFileLength))
        {
            return false;
        }

        Log.d(TAG, "Copying file '" + filename + "' ...");
        byte buf[] = new byte[1024];
        int len;
        try
        {
            OutputStream out = new FileOutputStream(outFile);
            while ((len = is.read(buf)) > 0)
            {
                out.write(buf, 0, len);
            }
            out.close();
            is.close();
        }
        catch (IOException e)
        {
            Log.e(TAG,
                    "Couldn't install file - " + filename + "! " + e.toString());
            return false;
        }

        return true;
    }

    private static String prepareRootCommandScript(Context context,
            String command)
    {
        try
        {
            Log.d(TAG, "Root command ==> " + command);

            // create a dummy script so that the user doesn't have to constantly
            // accept the SuperUser prompt
            File scriptFile = new File(context.getFilesDir().getParent()
                    + "/tmp/command.sh");
            scriptFile.delete(); // clear out old content

            scriptFile = new File(scriptFile.getAbsolutePath());
            scriptFile.getParentFile().mkdirs();
            scriptFile.createNewFile();

            BufferedWriter writer = new BufferedWriter(new FileWriter(
                    scriptFile));
            // TODO: will this always be here?
            writer.append("#!/system/bin/sh\n");
            writer.append(command);
            writer.close();

            // set executable permissions
            runCommand("chmod 0755 " + scriptFile.getAbsolutePath());

            return "su -c \"" + scriptFile.getAbsolutePath() + "\"";

        }
        catch (Exception e)
        {
            e.printStackTrace();
            return null;
        }
    }

    public static String runRootCommandGetOutput(Context context, String command)
    {
        return runCommandGetOutput(prepareRootCommandScript(context, command));
    }

    public static String runCommandGetOutput(String command)
    {
        String output = "";

        try
        {
            Log.d(TAG, "command is: " + command);
            Process process = Runtime.getRuntime().exec(command);

            int result = process.waitFor();
            Log.d(TAG, "result was: " + result);

            BufferedReader stdInput = new BufferedReader(new InputStreamReader(
                    process.getInputStream()));

            BufferedReader stdError = new BufferedReader(new InputStreamReader(
                    process.getErrorStream()));

            // read the output from the command
            String s;
            System.out.println("Here is the standard output of the command:\n");
            while ((s = stdInput.readLine()) != null)
            {
                System.out.println(s);
            }

            // read any errors from the attempted command
            System.out
                    .println("Here is the standard error of the command (if any):\n");
            while ((s = stdError.readLine()) != null)
            {
                System.out.println(s);
            }

        }
        catch (Exception e)
        {
            Log.d(TAG, e.toString());
        }

        return output;
    }

    private static synchronized Hashtable<String, String> getRunningProcesses()
    {
        File procDir = new File("/proc");
        FilenameFilter filter = new FilenameFilter()
        {
            @Override
            public boolean accept(File dir, String name)
            {
                try
                {
                    Integer.parseInt(name);
                }
                catch (NumberFormatException ex)
                {
                    return false;
                }
                return true;
            }
        };
        File[] processes = procDir.listFiles(filter);

        Hashtable<String, String> tmpRunningProcesses = new Hashtable<String, String>();
        for (File process : processes)
        {
            String cmdLine = "";
            ArrayList<String> cmdlineContent = readLinesFromFile(process
                    .getAbsoluteFile() + "/cmdline");
            if (cmdlineContent != null && cmdlineContent.size() > 0)
            {
                cmdLine = cmdlineContent.get(0);
            }
            // Adding to tmp-Hashtable
            tmpRunningProcesses.put(process.getAbsoluteFile().toString(),
                    cmdLine);
        }
        return tmpRunningProcesses;
    }

    private static HashSet<String> getPids(String processName) throws Exception
    {

        String pid = null;
        Hashtable<String, String> tmpRunningProcesses = getRunningProcesses();
        HashSet<String> pids = new HashSet<String>();
        String cmdLine = null;
        for (String fileName : tmpRunningProcesses.keySet())
        {
            cmdLine = tmpRunningProcesses.get(fileName);

            // Checking if processName matches
            if (cmdLine.contains(processName))
            { // equals() / contains()
                pid = fileName.substring(fileName
                        .lastIndexOf(File.separatorChar) + 1);
                pids.add(pid);
            }
        }
        return pids;
    }

    public static boolean isProcessRunning(String processName) throws Exception
    {
        return !getPids(processName).isEmpty();
    }

    // TODO
    public static boolean killProcess(Context context, String processName)
            throws Exception
    {
        // runRootCommand("killall " + processName); // requires busybox
        HashSet<String> pids = getPids(processName);
        for (String pid : pids)
        {
            runRootCommand(context, "kill -9 " + pid);
        }
        return true;
    }

    public static ArrayList<String> readLinesFromFile(String filename)
    {
        String line = null;
        BufferedReader br = null;
        InputStream ins = null;
        ArrayList<String> lines = new ArrayList<String>();
        File file = new File(filename);
        if (file.canRead() == false) return lines;
        try
        {
            ins = new FileInputStream(file);
            br = new BufferedReader(new InputStreamReader(ins), 8192);
            while ((line = br.readLine()) != null)
            {
                lines.add(line.trim());
            }
        }
        catch (Exception e)
        {
            Log.d(TAG,
                    "Unexpected error - Here is what I know: " + e.getMessage());
        }
        finally
        {
            try
            {
                ins.close();
                br.close();
            }
            catch (Exception e)
            {
                // Nothing.
            }
        }
        return lines;
    }

    public static Process runRootCommandInBackground(Context context,
            String command)
    {
        return runCommandInBackground(prepareRootCommandScript(context, command));
    }

    public static Process runCommandInBackground(String command)
    {
        Process process = null;
        try
        {
            process = Runtime.getRuntime().exec(command);

            // we must empty the output and error stream to end the process
            EmptyStreamThread emptyInputStreamThread = new EmptyStreamThread(
                    process.getInputStream());
            EmptyStreamThread emptyErrorStreamThread = new EmptyStreamThread(
                    process.getErrorStream());
            emptyInputStreamThread.start();
            emptyErrorStreamThread.start();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
        return process;
    }

    private static class EmptyStreamThread extends Thread
    {
        private InputStream istream = null;

        public EmptyStreamThread(InputStream istream)
        {
            this.istream = istream;

        }

        @Override
        public void run()
        {
            try
            {
                BufferedReader reader = new BufferedReader(
                        new InputStreamReader(istream));
                String line = null;
//                while ((line = reader.readLine()) != null)
//                {
////                    Log.d(TAG, "OUTPUT: " + line);
//                }
                while (reader.readLine() != null)
                {

                }
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }
            finally
            {
                try
                {
                    istream.close();
                }
                catch (IOException e)
                {
                    e.printStackTrace();
                }
            }
        }
    }
}
