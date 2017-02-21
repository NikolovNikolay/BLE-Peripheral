package advertizer.ibm.com.advertizer;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;

import java.util.HashSet;
import java.util.List;

/**
 * Created by Nikolay Nikolov
 */

public class Listener
{
    private static final String TAG = Listener.class.getCanonicalName();
    private BluetoothLeScanner scanner;
    private Context context;
    private HandlerThread thread;
    private Handler scanHandle;
    private String thName;
    private ScanSettings settings;
    private HashSet<String> connectingDevices;
    private HashSet<String> btDevices;

    private Runnable stopScanRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            scanner.stopScan(scanCallback);
//            scanHandle.postDelayed(startScanRunnable, 4000);
        }
    };

    private Runnable startScanRunnable = new Runnable()
    {
        @Override
        public void run()
        {
            scanner.startScan(scanCallback);
            scanHandle.postDelayed(stopScanRunnable, 10000);
        }
    };

    public Listener(Context context, BluetoothLeScanner scanner)
    {
        this.context = context;
        this.scanner = scanner;
        this.btDevices = new HashSet<>();
        connectingDevices = new HashSet<>();
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

            scanner.startScan(scanCallback);
            scanHandle.postDelayed(stopScanRunnable, 10000);
        } catch (Exception ignored)
        {
        }
    }

    public void stop()
    {
        scanHandle.removeCallbacks(startScanRunnable);
        scanHandle.removeCallbacks(stopScanRunnable);
        scanHandle.removeCallbacksAndMessages(null);
        scanner.stopScan(scanCallback);
        this.btDevices.clear();
        this.connectingDevices.clear();
        thread.quit();
    }

    private ScanCallback scanCallback = new ScanCallback()
    {
        @Override
        public void onScanResult(int callbackType, ScanResult result)
        {
            super.onScanResult(callbackType, result);
            BluetoothDevice device = result.getDevice();

//            if ((!connectingDevices.contains(device.getAddress()) && !btDevices.contains(device.getAddress())) && (device.getName() != null && device.getName().equals("Samsung Galaxy S7 edge")))
//            {
//                device.connectGatt(context, true, btGattCallback);
//                connectingDevices.add(device.getAddress());
//            }

            device.connectGatt(context, true, btGattCallback);
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

    private BluetoothGattCallback btGattCallback = new BluetoothGattCallback()
    {
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
            super.onDescriptorWrite(gatt, descriptor, status);
            BluetoothGattCharacteristic ch =
                    gatt.getService(GattUUIDS.SERVICE_BATTERY_UUID)
                            .getCharacteristic(GattUUIDS.CHAR_BATTERY_LEVEL_UUID);

            gatt.setCharacteristicNotification(ch, true);
        }

        @Override
        public void onDescriptorRead(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status)
        {
            super.onDescriptorRead(gatt, descriptor, status);
        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic)
        {
            super.onCharacteristicChanged(gatt, characteristic);
            Log.d(TAG, "Characteristic changing");
            if (GattUUIDS.CHAR_BATTERY_LEVEL_UUID.equals(characteristic.getUuid()))
            {
                int flag = characteristic.getProperties();
                int format;
                if ((flag & 0x01) != 0)
                {
                    format = BluetoothGattCharacteristic.FORMAT_UINT16;
                    Log.d(TAG, "Heart rate format UINT16.");
                }
                else
                {
                    format = BluetoothGattCharacteristic.FORMAT_UINT8;
                    Log.d(TAG, "Heart rate format UINT8.");
                }
                final float value = characteristic.getIntValue(format, 1);
                Log.d(TAG, "Received heart rate: " + value);


            }
        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            super.onCharacteristicWrite(gatt, characteristic, status);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status)
        {
            super.onCharacteristicRead(gatt, characteristic, status);
        }

        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status)
        {
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                try
                {
                    BluetoothGattService s = gatt.getService(GattUUIDS.SERVICE_BATTERY_UUID);
                    if (s != null)
                    {
                        BluetoothGattCharacteristic ch = s.getCharacteristic(GattUUIDS.CHAR_BATTERY_LEVEL_UUID);
                        BluetoothGattDescriptor descriptor = ch.getDescriptor(GattUUIDS.CHAR_CLIENT_CONFIG_UUID);
                        Log.d(TAG, "Setting descriptor value " + descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE));
                        btDevices.add(gatt.getDevice().getAddress());
                    }
                } catch (Exception e)
                {
                    connectingDevices.remove(gatt.getDevice().getAddress());
                    btDevices.add(gatt.getDevice().getAddress());
                }
//                write(descriptor);
//                mhSendValue.schedule(provideSendHRValTimerTask(), 1000);
            }
            else
            {
                connectingDevices.remove(gatt.getDevice().getAddress());
                btDevices.add(gatt.getDevice().getAddress());
            }
        }

        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState)
        {
            if (status != BluetoothGatt.GATT_SUCCESS)
            {
                Log.w(TAG, "Disconnected " + gatt.getDevice().getAddress());
            }

            if (newState == BluetoothProfile.STATE_CONNECTED)
            {
                Log.w(TAG, "Connected " + gatt.getDevice().getAddress());
                gatt.discoverServices();
            }
            else if (newState == BluetoothProfile.STATE_DISCONNECTED)
            {
                Log.w(TAG, "Disconnected " + gatt.getDevice().getAddress());
                connectingDevices.remove(gatt.getDevice().getAddress());
                btDevices.add(gatt.getDevice().getAddress());
            }
            else
            {
                Log.d(TAG, "Connection State Unknown");
            }
        }
    };

    public interface Notifiable
    {
        void notifyAboutAdvert(Object data);
    }
}
