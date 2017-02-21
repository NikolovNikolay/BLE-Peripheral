package advertizer.ibm.com.advertizer;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelUuid;
import android.util.Log;

import advertizer.ibm.com.advertizer.activities.BLEActivity;

/**
 * Created by Nikolay Nikolov
 */

public class BatteryService extends BLEService
{
    private static final String TAG = BatteryService.class.getCanonicalName();

    private static final long CHANGE_CHAR_INTERVAL = 1000;
    private static final String BATTERY_LEVEL_DESCRIPTION = "The current charge level of a " +
            "battery. 100% represents fully charged while 0% represents fully discharged.";

    private BLEService.ServiceFragmentDelegate delegate;
    private BluetoothGattService batteryService;
    private BluetoothGattCharacteristic batteryCharacteristic;
    private Context context;
    private Handler notifyHandler;
    private HandlerThread thread;
    private boolean isStarted;

    public BatteryService(Context context)
    {
        this.context = context;
        this.isStarted = false;
        batteryCharacteristic =
                new BluetoothGattCharacteristic(
                        GattUUIDS.CHAR_BATTERY_LEVEL_UUID,
                        BluetoothGattCharacteristic.PROPERTY_READ | BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                        BluetoothGattCharacteristic.PERMISSION_READ);

        batteryCharacteristic.addDescriptor(BLEActivity.getClientCharacteristicConfigurationDescriptor());
        batteryCharacteristic.addDescriptor(BatteryService.getCharacteristicPresentationFormatDescriptor());

        batteryService = new BluetoothGattService(GattUUIDS.SERVICE_BATTERY_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);
        batteryService.addCharacteristic(batteryCharacteristic);

        delegate = (BLEService.ServiceFragmentDelegate) context;
    }

    private float getBatteryLevel()
    {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        return ((float) level / (float) scale) * 100.0f;
    }

    public void start()
    {
        isStarted = true;
        thread = new HandlerThread(TAG + System.currentTimeMillis());
        thread.start();
        notifyHandler = new Handler(thread.getLooper());
        notifyHandler.postDelayed(notifyRunnable, CHANGE_CHAR_INTERVAL);
    }

    public void stop()
    {
        try
        {
            isStarted = false;
            notifyHandler.removeCallbacks(notifyRunnable);
            notifyHandler = null;
            thread.quit();
        } catch (Exception e)
        {
            Log.e(TAG, "Error while stopping:%n");
            e.printStackTrace();
        }
    }

    private static BluetoothGattDescriptor getCharacteristicPresentationFormatDescriptor()
    {
        return new BluetoothGattDescriptor(
                GattUUIDS.DESC_PRESENTATION_FORMAT_UUID,
                BluetoothGattDescriptor.PERMISSION_READ);
    }


    private Runnable notifyRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            if (isStarted)
            {
                batteryCharacteristic.setValue(
                        (int) getBatteryLevel(),
                        BluetoothGattCharacteristic.FORMAT_UINT8, 0);
                delegate.sendNotificationToDevices(batteryCharacteristic);
            }
        }
    };

    @Override
    public BluetoothGattService getBluetoothGattService()
    {
        return batteryService;
    }

    @Override
    public ParcelUuid getServiceUUID()
    {
        return new ParcelUuid(GattUUIDS.SERVICE_BATTERY_UUID);
    }
}
