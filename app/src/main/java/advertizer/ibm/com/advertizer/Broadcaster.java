package advertizer.ibm.com.advertizer;

import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelUuid;
import android.util.Log;

import java.util.UUID;

/**
 * Created by Nikolay Nikolov
 */

public class Broadcaster
{
    private Context context;
    private HandlerThread thread;
    private Handler broadcastHandle;
    private String thName;
    private ParcelUuid bleUuid;
    private BluetoothLeAdvertiser advertiser;
    private AdvertiseSettings advertiseSettings;
    private AdvertiseData data;

    private Runnable sendMessage = new Runnable()
    {
        @Override
        public void run()
        {
            advertiser.startAdvertising(advertiseSettings, data, advertisingCallback);
            broadcastHandle.postDelayed(sendMessage, 1000);
        }
    };

    public Broadcaster(Context context, BluetoothLeAdvertiser advertiser)
    {
        this.context = context;
        this.advertiser = advertiser;
        this.thName = System.currentTimeMillis() + "";
        this.thread = new HandlerThread(String.format("BroadCaster_%s", thName));
        this.bleUuid = new ParcelUuid(UUID.fromString(context.getString(R.string.advertise_service_uuid)));
        this.advertiseSettings = new AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH)
                .setConnectable(true)
                .build();
        this.data = new AdvertiseData.Builder()
                .setIncludeDeviceName(false)
                .addServiceUuid(bleUuid)
                .build();
    }

    public void start()
    {
//        thread.start();
//        broadcastHandle = new Handler(thread.getLooper());
//        this.broadcastHandle.post(sendMessage);
        advertiser.startAdvertising(advertiseSettings, data, advertisingCallback);
    }

    public void stop()
    {
//        broadcastHandle.removeCallbacks(sendMessage);
//        thread.quit();
        advertiser.stopAdvertising(advertisingCallback);
    }

    private float getBatteryLevel()
    {
        Intent batteryIntent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int level = batteryIntent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
        int scale = batteryIntent.getIntExtra(BatteryManager.EXTRA_SCALE, -1);

        return ((float) level / (float) scale) * 100.0f;
    }

    private AdvertiseCallback advertisingCallback = new AdvertiseCallback()
    {
        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect)
        {
            super.onStartSuccess(settingsInEffect);
        }

        @Override
        public void onStartFailure(int errorCode)
        {
            Log.e("BLE", "Advertising onStartFailure: " + errorCode);
            super.onStartFailure(errorCode);
        }
    };

}
