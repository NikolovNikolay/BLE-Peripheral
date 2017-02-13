package advertizer.ibm.com.advertizer;

import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelUuid;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Created by Nikolay Nikolov
 */

public class Listener
{
    private BluetoothLeScanner scanner;
    private Context context;
    private HandlerThread thread;
    private Handler scanHandle;
    private String thName;
    private ScanSettings settings;

    private Runnable scanRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            scanner.startScan(new ArrayList<ScanFilter>(), settings, scanCallback);
        }
    };

    public Listener(Context context, BluetoothLeScanner scanner)
    {
        this.context = context;
        this.scanner = scanner;
        this.thName = System.currentTimeMillis() + "";
        this.settings = new ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build();
    }

    public void start()
    {
        try
        {
            this.thread = new HandlerThread(String.format("BroadCaster_%s", thName));
            this.thread.start();
            scanHandle = new Handler(thread.getLooper());
        } catch (Exception ignored)
        {
        }

        scanHandle.post(scanRunnable);
    }

    public void stop()
    {
        scanHandle.removeCallbacks(scanRunnable);
        scanHandle.removeCallbacksAndMessages(null);
        thread.quit();
    }

    private ScanCallback scanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            super.onScanResult(callbackType, result);
            if (result != null && result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null)
            {
                if (result.getScanRecord().getServiceUuids().indexOf(new ParcelUuid(UUID.fromString(context.getString(R.string.advertise_service_uuid)))) >= 0)
                {
                    ((Notifiable) context).notifyAboutAdvert("Advertisement found");
                }
            }

            try
            {
                scanner.stopScan(scanCallback);
                scanHandle.postDelayed(scanRunnable, 3000);
            } catch (Exception ignore)
            {
            }
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results)
        {
            super.onBatchScanResults(results);
        }

        @Override
        public void onScanFailed(int errorCode)
        {
            super.onScanFailed(errorCode);
        }
    };

    public interface Notifiable
    {
        void notifyAboutAdvert(Object data);
    }
}
