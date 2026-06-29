package io.github.dsheirer.gui;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Enforces a single running instance of the application on the local machine and lets a second launch bring the
 * already-running instance's window to the foreground instead of starting a duplicate.
 *
 * Mechanism: the first instance binds a {@link ServerSocket} on a fixed loopback port (127.0.0.1).  A loopback-only
 * bind does not open a network port to other machines and does not trigger the Windows Defender Firewall prompt.  A
 * second launch fails to bind that port, connects to it, sends a one-line FOCUS command, and exits - the running
 * instance receives the command and restores/raises its window.
 *
 * A loopback socket is used (rather than a lock file) specifically because the operating system releases it the
 * instant the owning process exits, so a crash or the Windows watchdog restart never leaves a stale lock that would
 * block the next start.
 *
 * Configuration (system property, or environment variable in parentheses):
 *   sdrtrunk.singleinstance.enabled (SDRTRUNK_SINGLEINSTANCE_ENABLED) - set to "false" to disable entirely
 *   sdrtrunk.singleinstance.port    (SDRTRUNK_SINGLEINSTANCE_PORT)    - loopback port to use, default 52317
 */
public class SingleInstanceManager
{
    private static final Logger mLog = LoggerFactory.getLogger(SingleInstanceManager.class);
    private static final int DEFAULT_PORT = 52317;
    private static final String FOCUS_COMMAND = "FOCUS";
    private static final String ACK = "OK";
    private static final int CONNECT_TIMEOUT_MS = 1500;
    private static final int BIND_RETRY_COUNT = 3;
    private static final long BIND_RETRY_DELAY_MS = 300;

    private static volatile ServerSocket sLockSocket;
    private static volatile Runnable sFocusHandler;

    private SingleInstanceManager()
    {
    }

    /**
     * Attempts to become the single running instance.  If another instance already holds the lock, this signals it
     * to bring its window forward.
     *
     * @return true if this process acquired the single-instance lock and startup should proceed; false if another
     * instance is already running (the caller should exit immediately without launching the GUI).
     */
    public static boolean acquireLockOrSignalRunningInstance()
    {
        if(isDisabled())
        {
            mLog.info("Single-instance enforcement disabled via configuration");
            return true;
        }

        int port = getConfiguredPort();
        InetAddress loopback = InetAddress.getLoopbackAddress();

        for(int attempt = 1; attempt <= BIND_RETRY_COUNT; attempt++)
        {
            try
            {
                ServerSocket socket = new ServerSocket();
                socket.setReuseAddress(false); //we want bind to FAIL if another instance holds the port
                socket.bind(new InetSocketAddress(loopback, port));
                sLockSocket = socket;
                startListener(socket);
                mLog.info("Single-instance lock acquired on 127.0.0.1:" + port);
                return true;
            }
            catch(IOException bindFailure)
            {
                //Port is in use - most likely another instance.  Try to tell it to come to the foreground.
                if(signalRunningInstance(loopback, port))
                {
                    mLog.info("Another instance is already running - signaled it to come to the foreground; exiting");
                    return false;
                }

                //Could not signal (connection refused / no response): the port is held by something that is not a
                //live instance, or the previous instance is still shutting down.  Briefly retry the bind before
                //giving up so a watchdog restart that overlaps the old process can still take over.
                if(attempt < BIND_RETRY_COUNT)
                {
                    sleep(BIND_RETRY_DELAY_MS);
                }
                else
                {
                    mLog.warn("Could not bind single-instance port " + port + " and no running instance answered - " +
                            "continuing startup without single-instance enforcement", bindFailure);
                    return true;
                }
            }
        }

        return true;
    }

    /**
     * Registers the action used to bring the main window forward when a second launch signals this instance.  Safe
     * to call after the GUI has started; until it is set, incoming focus requests are ignored (the window is in the
     * middle of starting up and will appear on its own).
     */
    public static void setFocusHandler(Runnable focusHandler)
    {
        sFocusHandler = focusHandler;
    }

    /**
     * Releases the single-instance lock.  Called on shutdown; the OS would release it on process exit regardless.
     */
    public static void release()
    {
        ServerSocket socket = sLockSocket;
        sLockSocket = null;

        if(socket != null)
        {
            try
            {
                socket.close();
            }
            catch(IOException e)
            {
                //best-effort
            }
        }
    }

    private static void startListener(ServerSocket socket)
    {
        Thread listener = new Thread(() -> {
            while(!socket.isClosed())
            {
                try(Socket client = socket.accept())
                {
                    client.setSoTimeout(CONNECT_TIMEOUT_MS);
                    BufferedReader reader = new BufferedReader(
                            new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                    String command = reader.readLine();

                    if(FOCUS_COMMAND.equals(command))
                    {
                        OutputStream out = client.getOutputStream();
                        out.write((ACK + "\n").getBytes(StandardCharsets.UTF_8));
                        out.flush();

                        Runnable handler = sFocusHandler;
                        if(handler != null)
                        {
                            mLog.info("Received focus request from a second launch - bringing window to foreground");
                            handler.run();
                        }
                    }
                }
                catch(IOException e)
                {
                    //Socket closed during shutdown, or a malformed/aborted client connection - keep serving.
                    if(socket.isClosed())
                    {
                        break;
                    }
                }
            }
        }, "sdrtrunk single-instance listener");
        listener.setDaemon(true);
        listener.start();
    }

    /**
     * Connects to the already-running instance and asks it to bring its window forward.
     * @return true if a running instance answered the focus request; false if nothing answered.
     */
    private static boolean signalRunningInstance(InetAddress loopback, int port)
    {
        try(Socket socket = new Socket())
        {
            socket.connect(new InetSocketAddress(loopback, port), CONNECT_TIMEOUT_MS);
            socket.setSoTimeout(CONNECT_TIMEOUT_MS);

            OutputStream out = socket.getOutputStream();
            out.write((FOCUS_COMMAND + "\n").getBytes(StandardCharsets.UTF_8));
            out.flush();

            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String response = reader.readLine();
            return ACK.equals(response);
        }
        catch(IOException e)
        {
            return false;
        }
    }

    private static boolean isDisabled()
    {
        String value = System.getProperty("sdrtrunk.singleinstance.enabled",
                System.getenv("SDRTRUNK_SINGLEINSTANCE_ENABLED"));
        return value != null && value.equalsIgnoreCase("false");
    }

    private static int getConfiguredPort()
    {
        String value = System.getProperty("sdrtrunk.singleinstance.port",
                System.getenv("SDRTRUNK_SINGLEINSTANCE_PORT"));

        if(value != null)
        {
            try
            {
                return Integer.parseInt(value.trim());
            }
            catch(NumberFormatException e)
            {
                mLog.warn("Invalid single-instance port [" + value + "] - using default " + DEFAULT_PORT);
            }
        }

        return DEFAULT_PORT;
    }

    private static void sleep(long ms)
    {
        try
        {
            Thread.sleep(ms);
        }
        catch(InterruptedException e)
        {
            Thread.currentThread().interrupt();
        }
    }
}
