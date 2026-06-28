/*
 * *****************************************************************************
 * Copyright (C) 2014-2024 Dennis Sheirer
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>
 * ****************************************************************************
 */
package io.github.dsheirer.source.tuner.usb;

import io.github.dsheirer.buffer.INativeBuffer;
import io.github.dsheirer.buffer.INativeBufferFactory;
import io.github.dsheirer.sample.Listener;
import io.github.dsheirer.source.SourceException;
import io.github.dsheirer.source.tuner.ITunerErrorListener;
import io.github.dsheirer.source.tuner.TunerController;
import io.github.dsheirer.source.tuner.TunerType;
import io.github.dsheirer.source.tuner.manager.TunerManager;
import io.github.dsheirer.util.ThreadPool;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.usb4java.Context;
import org.usb4java.Device;
import org.usb4java.DeviceDescriptor;
import org.usb4java.DeviceHandle;
import org.usb4java.DeviceList;
import org.usb4java.LibUsb;
import org.usb4java.Transfer;
import org.usb4java.TransferCallback;

/**
 * Tuner controller implementation for USB tuners.  Manages general USB operations and incorporates threaded USB
 * Transfer processing.
 */
public abstract class USBTunerController extends TunerController
{
    private Logger mLog = LoggerFactory.getLogger(USBTunerController.class);
    private static final int USB_INTERFACE = 0x0;  //Common value for all currently supported devices
    private static final int USB_CONFIGURATION = 0x1;  //Common value for all currently supported devices
    private static final int USB_BULK_TRANSFER_BUFFER_POOL_SIZE = 8;
    protected static final byte USB_BULK_TRANSFER_ENDPOINT = (byte) 0x81;
    private static final long USB_BULK_TRANSFER_TIMEOUT_MS = 2000l;
    static final int ZERO_LENGTH_TRANSFER_ERROR_THRESHOLD = 50;
    static final long ZERO_LENGTH_TRANSFER_ERROR_MIN_DURATION_MS = 60_000L;

    protected int mBus;
    protected String mPortAddress;
    private Context mDeviceContext = new Context();
    private Device mDevice;
    private DeviceHandle mDeviceHandle;
    private DeviceDescriptor mDeviceDescriptor;
    private TransferManager mTransferManager = new TransferManager();
    private UsbEventProcessor mEventProcessor = new UsbEventProcessor();
    private AtomicBoolean mStreaming = new AtomicBoolean();
    private boolean mRunning = false;

    //Troubleshooting libusb bug: https://github.com/DSheirer/sdrtrunk/issues/1253
    private int mAnomalousTransfersDetected = 0;

    /**
     * Optional hook invoked with each completed (non-empty) raw USB transfer buffer, allowing tuner
     * implementations that know their native sample format to monitor the broadband RF level (e.g.
     * for antenna disconnection detection).  Implementations must be cheap, must not modify the
     * buffer position, and should internally rate-limit their work.  Default: no-op.
     * @param buffer raw transfer buffer (read with absolute indexing only)
     * @param length valid byte count
     */
    protected void monitorRawSignalLevel(java.nio.ByteBuffer buffer, int length)
    {
        //no-op by default
    }

    static boolean shouldRestartForZeroLengthTransfers(int consecutiveZeroLengthTransfers, long elapsedMillis)
    {
        return consecutiveZeroLengthTransfers >= ZERO_LENGTH_TRANSFER_ERROR_THRESHOLD &&
                elapsedMillis >= ZERO_LENGTH_TRANSFER_ERROR_MIN_DURATION_MS;
    }

    /**
     * USB tuner controller class. Provides auto-start and auto-stop function when complex buffer listeners are added
     * or removed from this tuner controller.
     *
     * @param bus number USB
     * @param portAddress address USB
     * @param tunerErrorListener to receive errors from this tuner controller
     */
    public USBTunerController(int bus, String portAddress, ITunerErrorListener tunerErrorListener)
    {
        super(tunerErrorListener);
        mBus = bus;
        mPortAddress = portAddress;
    }

    /**
     * Constructs an instance
     * @param bus usb
     * @param portAddress usb
     * @param minimum tunable frequency in Hertz
     * @param maximum tunable frequency in Hertz
     * @param halfBandwidth that is unusable for DC spike avoidance
     * @param usablePercent bandwith in Hertz
     * @param tunerErrorListener to receive errors from this tuner controller
     */
    public USBTunerController(int bus, String portAddress, long minimum, long maximum, int halfBandwidth, double usablePercent,
                              ITunerErrorListener tunerErrorListener)
    {
        this(bus, portAddress, tunerErrorListener);
        setMinimumFrequency(minimum);
        setMaximumFrequency(maximum);
        setMiddleUnusableHalfBandwidth(halfBandwidth);
        setUsableBandwidthPercentage(usablePercent);
    }

    /**
     * Tuner type for this USB controller
     */
    public abstract TunerType getTunerType();

    /**
     * Factory for converting received streaming sample data into native buffers, as provided by sub-class.
     */
    protected abstract INativeBufferFactory getNativeBufferFactory();

    /**
     * Sub-class definition of transfer buffer sizes to use for the tuner.  Note: this should be a power-of-two
     * value for compatibility with downstream (SIMD) operations (e.g. 65,536, 131072, 262144, etc.)
     * @return transfer buffer size.
     */
    protected abstract int getTransferBufferSize();

    /**
     * Sub-class method to perform additional device setup steps after the USB interface has been claimed and before any
     * transfer operations start.
     * @throws SourceException if there is an issue in configuring the device
     */
    protected abstract void deviceStart() throws SourceException;

    /**
     * Sub-class method to perform additional device shutdown steps after transfer processing has stopped and before
     * the USB interface is released.
     */
    protected abstract void deviceStop();

    /**
     * Starts or initializes this tuner.
     *
     * Note: sub-class implementations should override and invoke this method and then perform additional initialization
     * operations for the tuner.
     *
     * @throws SourceException if there is an error and/or this tuner is unusable.
     */
    public final void start() throws SourceException
    {
        if(mDeviceContext == null)
        {
            throw new SourceException("Device cannot be reused once it has been shutdown");
        }

        int status = LibUsb.init(mDeviceContext);

        if(status != LibUsb.SUCCESS)
        {
            throw new SourceException("Can't initialize libusb library - " + LibUsb.errorName(status));
        }

        mDevice = findDevice();

        if(mDevice == null)
        {
            throw new SourceException("Couldn't find USB device at bus [" + mBus + "] port [" + mPortAddress + "]");
        }

        mDeviceDescriptor = new DeviceDescriptor();
        status = LibUsb.getDeviceDescriptor(mDevice, mDeviceDescriptor);

        if(status != LibUsb.SUCCESS)
        {
            releaseAfterFailedStart(false);
            throw new SourceException("Can't obtain tuner's device descriptor - " + LibUsb.errorName(status));
        }

        mDeviceHandle = new DeviceHandle();
        status = LibUsb.open(mDevice, mDeviceHandle);

        //Now that we have opened the device and added an additional reference, remove the original reference placed on
        // the device during the findDevice() operation
        LibUsb.unrefDevice(mDevice);

        if(status == LibUsb.ERROR_ACCESS)
        {
            mLog.error("Access to USB tuner denied [bus:{} port:{} - {}] - (windows) reinstall zadig driver or (linux) blacklist driver and/or check udev rules", mBus, mPortAddress, LibUsb.errorName(status));
            releaseAfterFailedStart(false); //open failed - no handle to close, but release the libusb context
            throw new SourceException("access denied - if using linux, blacklist the default driver and/or install udev rules");
        }
        else if(status != LibUsb.SUCCESS)
        {
            mLog.error("Can't open USB tuner [bus:{} port:{} - {}] - check driver or Linux udev rules", mBus, mPortAddress, LibUsb.errorName(status));
            releaseAfterFailedStart(false); //open failed - no handle to close, but release the libusb context
            throw new SourceException("Can't open USB tuner - reinstall driver? - " + LibUsb.errorName(status));
        }

        //Detach the kernel driver if active and detach is supported.  Otherwise, let the claim interface fail.
        status = LibUsb.kernelDriverActive(mDeviceHandle, USB_INTERFACE);

        if(status == 1) //kernel driver is attached and detach operation is supported
        {
            status = LibUsb.detachKernelDriver(mDeviceHandle, USB_INTERFACE);

            if(status != LibUsb.SUCCESS)
            {
                mLog.error("Unable to detach kernel driver for USB tuner device - bus:" + mBus + " port:" + mPortAddress);
                releaseAfterFailedStart(true); //handle was opened - close it and release the context
                throw new SourceException("Can't detach kernel driver");
            }
        }

        //Set the configuration which also invokes a soft reset on the device
        status = LibUsb.setConfiguration(mDeviceHandle, USB_CONFIGURATION);

        if(status == LibUsb.ERROR_BUSY)
        {
            mLog.error("Unable to set USB configuration on tuner - device is busy (in use by another application)");
            releaseAfterFailedStart(true);
            throw new SourceException("USB tuner is in-use by another application");
        }
        else if(status != LibUsb.SUCCESS)
        {
            releaseAfterFailedStart(true);
            throw new SourceException("Can't set configuration (ie reset) on the USB tuner - " + LibUsb.errorName(status));
        }

        //Claim the interface
        status = LibUsb.claimInterface(mDeviceHandle, USB_INTERFACE);

        if(status == LibUsb.ERROR_BUSY)
        {
            releaseAfterFailedStart(true);
            throw new SourceException("USB tuner is in-use by another application");
        }
        else if(status != LibUsb.SUCCESS)
        {
            releaseAfterFailedStart(true);
            throw new SourceException("Can't claim interface on USB tuner - " + LibUsb.errorName(status));
        }

        //Set running true for deviceStart() operations that require it.
        mRunning = true;

        try
        {
            deviceStart();
        }
        catch(Exception se)
        {
            mRunning = false;
            releaseAfterFailedStart(true);
            throw se;
        }
    }

    /**
     * Releases the libusb resources acquired during a failed start() so a failing tuner (e.g. a device that keeps
     * returning LIBUSB_ERROR_ACCESS on every 5-second fast-recovery attempt) does not leak a libusb context - and,
     * on post-open failures, the device handle - on every attempt.  That accumulation of un-exited contexts is a
     * known aggravator of the native libusb Windows poll_windows assertion (assert(fd != NULL)) that aborts the
     * whole process.  The context is exited and the field nulled; a later stop() on this (discarded) controller is
     * null-guarded so it cannot double-exit, and the recovery path always creates a fresh controller for the next
     * attempt.  Safe here because start() failed before any streaming/event-processing thread was started.
     * @param closeHandle true if the device handle was successfully opened and must be closed.
     */
    private void releaseAfterFailedStart(boolean closeHandle)
    {
        if(closeHandle && mDeviceHandle != null)
        {
            try { LibUsb.close(mDeviceHandle); } catch(Exception e) { /* best-effort */ }
        }

        mDeviceHandle = null;
        mDeviceDescriptor = null;

        if(mDeviceContext != null)
        {
            try { LibUsb.exit(mDeviceContext); } catch(Exception e) { /* best-effort */ }
            mDeviceContext = null;
        }
    }

    /**
     * Prepares the tuner for full shutdown by stopping streaming, shutdown the device, and releasing the USB resources.
     */
    public final void stop()
    {
        mRunning = false;

        //Spin the graceful streaming/device shutdown onto a worker thread so that a stuck native libusb call can't
        //hang the calling (UI/recovery) thread indefinitely.
        Thread t = new Thread(() -> {
            stopStreaming();
            mNativeBufferBroadcaster.clear();
            deviceStop();
        });
        t.setName("sdrtrunk USB tuner shutdown - bus [" + mBus + "] port [" + mPortAddress + "]");
        t.setDaemon(true);
        t.start();

        boolean gracefulShutdownCompleted = false;

        try
        {
            //Wait for the graceful shutdown to finish.  This budget must comfortably exceed the internal waits
            //performed by stopStreaming() (the event-processor thread join of up to 1000ms plus a final 50ms
            //handle-events pass) so the normal case always completes cleanly.  The join returns as soon as the
            //worker finishes, so this cap does not slow down a healthy shutdown.
            t.join(3000);
            gracefulShutdownCompleted = !t.isAlive();
        }
        catch(InterruptedException ie)
        {
            Thread.currentThread().interrupt();
        }

        if(!gracefulShutdownCompleted)
        {
            //The worker is still inside a native libusb call.  Freeing transfers, closing the handle, or exiting
            //the context now would race the still-running libusb event loop and corrupt libusb's internal Windows
            //poll-fd list, aborting the entire application with a "poll_windows.c ... assert(fd != NULL)" failure
            //(see issue #1253).  Interrupting a thread blocked in a native JNI call does nothing, so the only safe
            //option is to leak this one device's libusb resources.  The controller instance is discarded by the
            //caller (DiscoveredTuner.stop() nulls its tuner reference) and never reused, and the application keeps
            //running.  Leave the libusb fields untouched so the still-running worker can finish without an NPE.
            mLog.error("USB tuner shutdown did not complete in time [bus:{} port:{}] - leaking libusb resources to " +
                    "avoid a native poll_windows assertion that would abort the application", mBus, mPortAddress);
            return;
        }

        //Graceful shutdown completed - all transfers have been cancelled and returned and the event-processing
        //thread has stopped, so it is now safe to free the transfers and release the libusb handle and context.
        mTransferManager.freeTransfers();

        if(mDeviceHandle != null)
        {
            LibUsb.releaseInterface(mDeviceHandle, USB_INTERFACE);
            LibUsb.close(mDeviceHandle);
            mDeviceHandle = null;
            mDevice = null;
            mDeviceDescriptor = null;
        }

        //Null-guarded: a failed start() may have already exited and nulled the context (releaseAfterFailedStart),
        //and exiting it again would be a double libusb_exit that can itself abort the process.
        if(mDeviceContext != null)
        {
            LibUsb.exit(mDeviceContext);
            mDeviceContext = null;
        }
    }

    /**
     * Forcefully releases and closes the libusb device when a disconnect is detected to avoid lockouts.
     */
    private void forceCloseDevice()
    {
        if(mDeviceHandle != null)
        {
            try {
                LibUsb.releaseInterface(mDeviceHandle, USB_INTERFACE);
                LibUsb.close(mDeviceHandle);
            } catch (Exception e) {
            }
            mDeviceHandle = null;
            mDevice = null;
            mDeviceDescriptor = null;
        }
    }

    /**
     * Starts streaming data from the tuner
     */
    private void startStreaming()
    {
        if(mStreaming.compareAndSet(false, true))
        {
            try
            {
                prepareStreaming();
                List<Transfer> transfers = mTransferManager.getTransfers();
                mEventProcessor.start();
                mTransferManager.setAutoResubmitTransfers(true);
                mTransferManager.submitTransfers(transfers);
            }
            catch(SourceException se)
            {
                mLog.error("Error starting streaming on USB tuner [bus:{} port:{}]", mBus, mPortAddress, se);
            }
        }
    }

    /**
     * Prepares to start streaming.  This method can be overridden by sub-class to implement additional actions
     * need to prepare before start streaming.
     */
    protected void prepareStreaming()
    {
    }

    /**
     * Stop streaming data from the tuner
     */
    private void stopStreaming()
    {
        if(mStreaming.compareAndSet(true, false))
        {
            //Turn off auto-resubmit of USB transfer buffers
            mTransferManager.setAutoResubmitTransfers(false);

            //Stop event processing thread to put all submitted tranfers in a stable state - blocks until stopped
            mEventProcessor.stop();

            //Cancel all currently submitted transfers
            mTransferManager.cancelTransfers();

            //Perform final event processing iteration so LibUsb returns all of our cancelled tranfers
            mEventProcessor.handleFinalEvents();

            streamingCleanup();
        }
    }

    /**
     * Post streaming cleanup actions.  This method can be overridden by sub-class to implement additional actions
     * needed to cleanup after streaming stops.
     */
    protected void streamingCleanup()
    {
    }

    /**
     * Finds the USB device for this tuner at the specified USB bus and port.
     * @return discovered USB device
     * @throws SourceException if there is an error or the device is not discovered.
     */
    private Device findDevice() throws SourceException
    {
        Device foundDevice = null;

        DeviceList deviceList = new DeviceList();
        int count = LibUsb.getDeviceList(mDeviceContext, deviceList);

        if(count >= 0)
        {
            for(Device device: deviceList)
            {
                int bus = LibUsb.getBusNumber(device);
                int port = LibUsb.getPortNumber(device);

                if(port > 0)
                {
                    String portAddress = TunerManager.getPortAddress(device);

                    if(mBus == bus && mPortAddress != null && mPortAddress.equals(portAddress))
                    {
                        foundDevice = device;
                    }
                    else
                    {
                        LibUsb.unrefDevice(device);
                    }
                }
                else
                {
                    LibUsb.unrefDevice(device);
                }
            }
        }

        //Free the device list but don't auto-unref all the devices ... we already did that during iteration
        LibUsb.freeDeviceList(deviceList, false);

        if(foundDevice != null)
        {
            return foundDevice;
        }

        throw new SourceException("LibUsb couldn't discover USB device [" + mBus + ":" + mPortAddress +
                "] from device list" + (count < 0 ? " - error: " + LibUsb.errorName(count) : ""));
    }

    /**
     * Access the discovered USB device.
     */
    protected Device getDevice()
    {
        return mDevice;
    }

    /**
     * LibUsb context for this device.
     */
    protected Context getDeviceContext()
    {
        return mDeviceContext;
    }

    /**
     * LibUsb device descriptor for this device
     */
    protected DeviceDescriptor getDeviceDescriptor()
    {
        return mDeviceDescriptor;
    }

    /**
     * USB Device Handle for the claimed device
     */
    protected DeviceHandle getDeviceHandle()
    {
        return mDeviceHandle;
    }

    /**
     * Indicates if the device handle is non-null
     */
    protected boolean hasDeviceHandle()
    {
        return getDeviceHandle() != null;
    }

    /**
     * Indicates if this device is usable, meaning it has been started and is not yet stopping.
     *
     * Note: this is a general usability flag for controlling all code that touches the USB interface(s)
     */
    protected boolean isRunning()
    {
        return mRunning;
    }

    /**
     * Adds the IQ buffer listener and automatically starts stream buffer transfer processing, if not already started.
     */
    @Override
    public void addBufferListener(Listener<INativeBuffer> listener)
    {
        if(isRunning())
        {
            getLock().lock();

            try
            {
                boolean hasExistingListeners = hasBufferListeners();

                super.addBufferListener(listener);

                if(!hasExistingListeners)
                {
                    startStreaming();
                }
            }
            finally
            {
                getLock().unlock();
            }
        }
    }

    /**
     * Removes the IQ buffer listener and stops stream buffer transfer processing if there are no more listeners.
     */
    @Override
    public void removeBufferListener(Listener<INativeBuffer> listener)
    {
        getLock().lock();

        try
        {
            super.removeBufferListener(listener);

            if(!hasBufferListeners())
            {
                stopStreaming();
            }
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * Manages USB transfer (ie zero-copy) buffer processing
     */
    class TransferManager implements TransferCallback
    {
        //A transient USB I/O burst can momentarily push every transfer buffer into the error queue.  Rather than
        //killing the tuner instantly (which drops every channel using it), we try to drain the error queue with
        //exponential backoff over a short window and only shut down if the buffers stay exhausted.
        private static final int MAX_BUFFER_RECOVERY_ATTEMPTS = 5;
        private static final long BUFFER_RECOVERY_INITIAL_BACKOFF_MS = 100;
        private static final long BUFFER_RECOVERY_MAX_BACKOFF_MS = 1000;

        private List<Transfer> mAvailableTransfers;
        private LinkedTransferQueue<Transfer> mInProgressTransfers = new LinkedTransferQueue<>();
        private boolean mAutoResubmitTransfers = false;
        private int mTransferErrorCount = 0;
        private List<Transfer> mErrorTransfers = new ArrayList<>();
        private int mResubmitFailureLogCount = 0;
        private int mConsecutiveZeroLengthTransfers = 0;
        private long mFirstZeroLengthTransferTimestamp = 0;
        private final java.util.concurrent.atomic.AtomicBoolean mBufferRecoveryInProgress =
                new java.util.concurrent.atomic.AtomicBoolean(false);

        /**
         * Creates USB Transfers to carry the streaming sample data.  Transfer buffers are backed by native memory
         * byte buffers outside the JVM.
         *
         * @return list of transfers
         * @throws SourceException if there is an error creating transfers
         */
        private List<Transfer> getTransfers() throws SourceException
        {
            if(mAvailableTransfers == null)
            {
                mAvailableTransfers = new ArrayList<>();

                for(int x = 0; x < USB_BULK_TRANSFER_BUFFER_POOL_SIZE; x++)
                {
                    Transfer transfer = LibUsb.allocTransfer();

                    if(transfer == null)
                    {
                        throw new SourceException("Couldn't allocate USB transfer buffer - out of memory");
                    }

                    final ByteBuffer buffer = ByteBuffer.allocateDirect(getTransferBufferSize());

                    LibUsb.fillBulkTransfer(transfer, mDeviceHandle, USB_BULK_TRANSFER_ENDPOINT, buffer,
                            TransferManager.this, "Transfer Buffer " + x, USB_BULK_TRANSFER_TIMEOUT_MS);

                    mAvailableTransfers.add(transfer);
                }
            }

            return mAvailableTransfers;
        }

        /**
         * Prepare to stop processing transfers when stopping streaming of data.
         */
        private void setAutoResubmitTransfers(boolean resubmit)
        {
            mAutoResubmitTransfers = resubmit;

            if(!resubmit)
            {
                mConsecutiveZeroLengthTransfers = 0;
                mFirstZeroLengthTransferTimestamp = 0;
            }
        }

        /**
         * Submits the transfers to start sample stream processing
         * @param transfers to submit
         */
        private void submitTransfers(List<Transfer> transfers)
        {
            for(Transfer transfer: transfers)
            {
                submitTransfer(transfer);
            }
        }

        /**
         * (Re)Submits the transfer for stream processing
         *
         * Note: synchronized used here because there can be multiple threads can invoke LibUsb.handleTimeoutEvents
         * (scheduled thread pool and a dedicated shutdown thread) during tuner shutdown and this has caused transfer
         * tracking issues.
         *
         * @param transfer to (re)submit
         */
        private synchronized void submitTransfer(Transfer transfer)
        {
            int status = LibUsb.submitTransfer(transfer);

            if(status == LibUsb.SUCCESS)
            {
                mInProgressTransfers.add(transfer);

                //Attempt to resubmit any previous transfers that failed on submit
                if(!mErrorTransfers.isEmpty())
                {
                    Transfer toResubmit = mErrorTransfers.remove(0);
                    int resubmitStatus = LibUsb.submitTransfer(toResubmit);

                    if(resubmitStatus == LibUsb.SUCCESS)
                    {
                        mInProgressTransfers.add(toResubmit);

                        //Only log this if more than half of the total transfer buffers are in error-holding
                        if(mErrorTransfers.size() >= (mAvailableTransfers.size() / 2))
                        {
                            mLog.info("Successfully resubmitted previous error USB transfer buffer.  Current transfer buffer" +
                                    " status (error queue/total available) [" + mErrorTransfers.size() + "/" +
                                    mAvailableTransfers.size() + "]");
                        }
                    }
                    else if(resubmitStatus == LibUsb.ERROR_BUSY)
                    {
                        //Ignore - this indicates the transfer was previously submitted and libusb is still working it.
                    }
                    else
                    {
                        //Add it back to the queue to try again later.
                        mErrorTransfers.add(toResubmit);
                        mTransferErrorCount++;
                    }
                }
            }
            else if(status == LibUsb.ERROR_BUSY)
            {
                //Ignore - this indicates the transfer was previously submitted and libusb is still working it.  I'm not
                //sure how this happens because we give libusb a transfer and it hands it back when it's full.  If
                //libusb is still working it, then why did it indicate the transfer was completed?  So, we simply
                //ignore this error code.  Other libraries simply ignore the submit status code altogether.
            }
            else if(status == LibUsb.ERROR_NO_DEVICE || status == LibUsb.ERROR_PIPE)
            {
                mLog.error("USB device physically disconnected during submit - error [" + LibUsb.errorName(status) + "]");
                ThreadPool.CACHED.submit(() -> { forceCloseDevice(); setErrorMessage("USB Error - Device Disconnected"); });
            }
            else
            {
                mLog.error("USB transfer [" + transfer + "] submit attempt failed with error [" + LibUsb.errorName(status) +
                        "] - adding to error queue to resubmit later - this may be a temporary USB issue and has happened [" +
                        mTransferErrorCount + "] time(s) so far.  Current transfer error queue (error/total) [" +
                        mErrorTransfers.size() + "/" + mAvailableTransfers.size() + "]");

                mErrorTransfers.add(transfer);
                mTransferErrorCount++;
            }

            if(mErrorTransfers.size() >= mAvailableTransfers.size())
            {
                //Don't shut down on a momentary exhaustion (a transient USB I/O burst can fill the queue in a few ms
                //before any resubmit succeeds).  Try to recover with backoff first; only shut down if it persists.
                scheduleBufferRecovery();
            }
        }

        /**
         * Launches a one-shot background recovery that repeatedly attempts to resubmit the queued error transfers with
         * exponential backoff.  Only if the transfer buffers remain fully exhausted after the recovery window does the
         * tuner shut down (allowing the channel restart/recovery logic to re-acquire it).  This prevents a transient
         * USB I/O burst from needlessly killing the tuner and dropping all of its channels.
         */
        private void scheduleBufferRecovery()
        {
            if(!mAutoResubmitTransfers)
            {
                return; //already shutting down
            }

            if(!mBufferRecoveryInProgress.compareAndSet(false, true))
            {
                return; //a recovery pass is already running
            }

            mLog.warn("All USB transfer buffers are temporarily in the error queue - attempting recovery with backoff " +
                    "before shutting down the tuner");

            ThreadPool.CACHED.submit(() -> {
                try
                {
                    long backoff = BUFFER_RECOVERY_INITIAL_BACKOFF_MS;

                    for(int attempt = 1; attempt <= MAX_BUFFER_RECOVERY_ATTEMPTS; attempt++)
                    {
                        try
                        {
                            Thread.sleep(backoff);
                        }
                        catch(InterruptedException ie)
                        {
                            Thread.currentThread().interrupt();
                            return;
                        }

                        if(!mAutoResubmitTransfers)
                        {
                            return; //tuner is shutting down for another reason
                        }

                        if(drainErrorTransfers())
                        {
                            mLog.info("USB transfer buffers recovered after [" + attempt + "] attempt(s) - tuner " +
                                    "continues running");
                            return;
                        }

                        backoff = Math.min(backoff * 2, BUFFER_RECOVERY_MAX_BACKOFF_MS);
                    }

                    mLog.error("USB transfer buffers remained exhausted after [" + MAX_BUFFER_RECOVERY_ATTEMPTS +
                            "] recovery attempts - shutting down USB tuner");
                    setErrorMessage("USB Error - Transfer Buffers Exhausted");
                }
                finally
                {
                    mBufferRecoveryInProgress.set(false);
                }
            });
        }

        /**
         * Attempts to resubmit every queued error transfer.
         * @return true if at least one buffer is back in service (queue no longer fully exhausted), false if it
         * remains fully exhausted or the device is gone.
         */
        private synchronized boolean drainErrorTransfers()
        {
            if(mAvailableTransfers == null || mErrorTransfers.isEmpty())
            {
                return mAvailableTransfers != null;
            }

            java.util.Iterator<Transfer> it = mErrorTransfers.iterator();

            while(it.hasNext())
            {
                Transfer transfer = it.next();
                int status = LibUsb.submitTransfer(transfer);

                if(status == LibUsb.SUCCESS)
                {
                    it.remove();
                    mInProgressTransfers.add(transfer);
                }
                else if(status == LibUsb.ERROR_NO_DEVICE || status == LibUsb.ERROR_PIPE)
                {
                    //Device physically gone - let the disconnect path handle it; recovery can't help.
                    return false;
                }
                //else: leave it queued and try again on the next recovery attempt
            }

            return mErrorTransfers.size() < mAvailableTransfers.size();
        }

        /**
         * Cancels any in-progress transfers to prepare for shutdown.
         *
         * Note: this should only be invoked after the LibUsb event processing thread has been stopped so that the
         * transfer buffers are in a stable (submitted vs callback) state and we can then flip their cancel state and
         * then finish processing the timeout events under the control of a single (shutdown) thread.
         *
         * Synchronized on the same monitor as submitTransfer()/drainErrorTransfers() so a still-running buffer
         * recovery pass cannot iterate or resubmit the transfer collections while shutdown is cancelling them
         * (which would otherwise throw ConcurrentModificationException and could submit a transfer that is about
         * to be freed - a native use-after-free that aborts the JVM).
         */
        private synchronized void cancelTransfers()
        {
            for(Transfer transfer: mInProgressTransfers)
            {
                LibUsb.cancelTransfer(transfer);
            }
            for(Transfer transfer: mErrorTransfers)
            {
                LibUsb.cancelTransfer(transfer);
            }
        }

        /**
         * Frees/disposes allocated USB transfer buffers.
         *
         * Synchronized on the same monitor as submitTransfer()/drainErrorTransfers() so the native transfer
         * buffers can never be freed while a buffer recovery pass is still resubmitting them.  Once this method
         * sets mAvailableTransfers to null, drainErrorTransfers() (which re-checks under the same lock) treats the
         * device as gone and stops touching the freed transfers.
         */
        private synchronized void freeTransfers()
        {
            if(mAvailableTransfers != null)
            {
                for(Transfer transfer: mAvailableTransfers)
                {
                    try
                    {
                        LibUsb.freeTransfer(transfer);
                    }
                    catch(Exception e)
                    {
                        mLog.error("Error releasing allocated USB transfer buffer during tuner shutdown: " +
                                e.getLocalizedMessage());
                    }
                }

                mAvailableTransfers.clear();
                mAvailableTransfers = null;
            }
        }

        @Override
        public void processTransfer(Transfer transfer)
        {
            mInProgressTransfers.remove(transfer);

            //Remove under the TransferManager monitor: a concurrent buffer recovery pass (drainErrorTransfers)
            //or a submit may be iterating/mutating mErrorTransfers on another thread, so an unsynchronized
            //contains()/remove() here risks ConcurrentModificationException and list corruption.
            boolean wasErrorTransfer;

            synchronized(this)
            {
                wasErrorTransfer = mErrorTransfers.remove(transfer);
            }

            if(wasErrorTransfer)
            {
                mLog.warn("USB transfer [" + transfer + "] that was being tracked as an error transfer, has just been " +
                        "delivered as completed with transfer status [" + LibUsb.errorName(transfer.status()) +
                        "] - removing it from the transfer error queue");
            }

            switch(transfer.status())
            {
                case LibUsb.TRANSFER_COMPLETED:
                case LibUsb.TRANSFER_STALL:
                case LibUsb.TRANSFER_TIMED_OUT:
                case LibUsb.TRANSFER_ERROR:
                //Note: cancel flag can be set by libusb, independent of commanded cancel of transfers - we simply
                //resubmit the transfer for continued use.
                case LibUsb.TRANSFER_CANCELLED:
                    int transferLength = transfer.actualLength();

                    if(transferLength > 0)
                    {
                        dispatchTransfer(transfer);
                        mConsecutiveZeroLengthTransfers = 0;
                        mFirstZeroLengthTransferTimestamp = 0;
                        monitorRawSignalLevel(transfer.buffer(), transferLength);
                    }
                    else if(mAutoResubmitTransfers)
                    {
                        //Detect a stalled/locked-up tuner that keeps completing transfers with no sample data.
                        //Without this, channels continue running with zero signal and reception silently dies.  The
                        //condition must persist for a minimum wall-clock duration so a burst of rapid empty transfer
                        //completions during startup/recovery doesn't churn every channel through tuner recovery.
                        if(mConsecutiveZeroLengthTransfers == 0)
                        {
                            mFirstZeroLengthTransferTimestamp = System.currentTimeMillis();
                        }

                        mConsecutiveZeroLengthTransfers++;

                        long elapsedMillis = System.currentTimeMillis() - mFirstZeroLengthTransferTimestamp;

                        if(shouldRestartForZeroLengthTransfers(mConsecutiveZeroLengthTransfers, elapsedMillis))
                        {
                            mConsecutiveZeroLengthTransfers = 0;
                            mFirstZeroLengthTransferTimestamp = 0;
                            mLog.error("USB tuner has completed [" + ZERO_LENGTH_TRANSFER_ERROR_THRESHOLD +
                                    "] consecutive transfers with no sample data over [" + elapsedMillis +
                                    "ms] - tuner appears stalled - restarting");
                            ThreadPool.CACHED.submit(() -> setErrorMessage("USB Error - No Sample Data - Tuner Stalled"));
                        }
                    }

                    transfer.buffer().rewind();

                    if(mAutoResubmitTransfers)
                    {
                        submitTransfer(transfer);
                    }
                    break;
                case LibUsb.TRANSFER_NO_DEVICE:
                    mLog.error("USB device physically disconnected during transfer callback");
                    ThreadPool.CACHED.submit(() -> { forceCloseDevice(); setErrorMessage("USB Error - Device Disconnected"); });
                    break;
                default:
                    //Unexpected transfer error - shutdown the tuner
                    transfer.buffer().rewind();

                    //Only set an error if we're not shutting down
                    if(mAutoResubmitTransfers)
                    {
                        //spin this off onto the thread pool, so it doesn't impact the usb processor thread.
                        if (transfer.status() == LibUsb.ERROR_IO)
                        {
                            ThreadPool.CACHED.submit(() -> setErrorMessage("USB Error - Transfer Buffers Exhausted"));
                        }
                        else
                        {
                            ThreadPool.CACHED.submit(() -> setErrorMessage("LibUsb Transfer Error - stopping device - " +
                                    "status [" + transfer.status() + "] - " + LibUsb.errorName(transfer.status())));
                        }
                    }
                    break;
            }
        }

        /**
         * Makes a copy of the transfer's native memory byte array payload so that the transfer can be reused.
         * Dispatches the native buffer to registered listeners.
         * @param transfer to copy and dispatch
         */
        private void dispatchTransfer(Transfer transfer)
        {
            //Pass the transfer's byte buffer so the native buffer factory can make a copy of the byte array contents
            //and package it as a native buffer.
            INativeBuffer nativeBuffer = getNativeBufferFactory().getBuffer(transfer.buffer(), System.currentTimeMillis());
            mNativeBufferBroadcaster.broadcast(nativeBuffer);
        }
    }

    /**
     * Threaded LibUsb event processor - continuously polls LibUsb to process events exclusively for this USB tuner
     * device using the device context.
     */
    class UsbEventProcessor implements Runnable
    {
        private Thread mThread;
        private boolean mProcessing = false;

        /**
         * Start the event processing thread
         */
        public void start()
        {
            if(mThread == null)
            {
                mProcessing = true;
                mThread = new Thread(this);
                mThread.setName("sdrtrunk USB tuner - bus [" + mBus + "] port [" + mPortAddress + "]");
                mThread.setDaemon(true);
                mThread.setPriority(Thread.MAX_PRIORITY);
                mThread.start();
            }
        }

        /**
         * Set the stop processing flag and block until the thread stops, blocking up to 1000 ms.
         */
        public void stop()
        {
            mProcessing = false;

            try
            {
                //Give the thread a second to stop - it should happen quickly because it's only checking transfers
                //for completed status and returning them to us to dispatch.
                mThread.join(1000);
            }
            catch(Exception e)
            {
                mLog.error("Error stopping LibUsb event processing thread - " + e.getLocalizedMessage());
            }

            mThread = null;
        }

        /**
         * This performs a final handle-events invocation after the event processing thread has been shutdown and
         * transfers have been flagged as cancelled.  This should cause LibUsb to return all in-progress and now
         * canceled transfers back to us via the TransferManager.processTransfer() method.
         */
        public void handleFinalEvents()
        {
            try
            {
                //Use a short timeout since this is a shutdown operation
                LibUsb.handleEventsTimeout(mDeviceContext, 50);
            }
            catch(Throwable throwable)
            {
                mLog.error("Error while processing stop-streaming LibUsb timeout events", throwable);
            }
        }

        /**
         * LibUsb event/timeout processing loop
         */
        @Override
        public void run()
        {
            mProcessing = true;

            while(mProcessing)
            {
                try
                {
                    int status = LibUsb.handleEventsTimeout(mDeviceContext, 250);
                    if(status == LibUsb.ERROR_NO_DEVICE || status == LibUsb.ERROR_PIPE)
                    {
                        mProcessing = false;
                        mLog.error("USB device physically disconnected during handle events - error [" + LibUsb.errorName(status) + "]");
                        ThreadPool.CACHED.submit(() -> { forceCloseDevice(); setErrorMessage("USB Error - Device Disconnected"); });
                    }
                }
                catch(Throwable throwable)
                {
                    mLog.error("Error while processing LibUsb timeout events", throwable);
                }
            }
        }
    }
}
