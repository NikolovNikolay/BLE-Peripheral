package advertizer.ibm.com.advertizer.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Arrays;
import java.util.HashSet;

import advertizer.ibm.com.advertizer.BLEService;
import advertizer.ibm.com.advertizer.BatteryService;
import advertizer.ibm.com.advertizer.Constants;
import advertizer.ibm.com.advertizer.GattUUIDS;
import advertizer.ibm.com.advertizer.Listener;
import advertizer.ibm.com.advertizer.R;

public class BLEActivity extends BaseActivity implements View.OnClickListener, Listener.Notifiable, BLEService.ServiceFragmentDelegate
{
    private static final String[] PERIPHERALS_NAMES = new String[]{"Battery"};
    public final static String EXTRA_PERIPHERAL_INDEX = "PERIPHERAL_INDEX";
    private static final int REQUEST_ENABLE_BT = 1;
    private static final String TAG = BLEActivity.class.getCanonicalName();

    private ViewContainer views;
    private Resources resources;
    private BLEService batteryService;
    private BluetoothGattService bluetoothGattService;
    private HashSet<BluetoothDevice> mBluetoothDevices;
    private BluetoothAdapter btAdapter;
    private BluetoothManager btManager;
    private AdvertiseData mAdvData;
    private AdvertiseData mAdvScanResponse;
    private AdvertiseSettings mAdvSettings;
    private BluetoothLeAdvertiser btAdvertiser;
    private BluetoothLeScanner btScanner;
    private BluetoothGattServer gattServer;
    private Listener listener;

    private final AdvertiseCallback mAdvCallback = new AdvertiseCallback()
    {
        @Override
        public void onStartFailure(int errorCode)
        {
            super.onStartFailure(errorCode);
            Log.e(TAG, "Not broadcasting: " + errorCode);
            int statusText;
            switch (errorCode)
            {
                case ADVERTISE_FAILED_ALREADY_STARTED:
                    Log.w(TAG, "App was already advertising");
                    break;
                case ADVERTISE_FAILED_DATA_TOO_LARGE:
                    break;
                case ADVERTISE_FAILED_FEATURE_UNSUPPORTED:
                    break;
                case ADVERTISE_FAILED_INTERNAL_ERROR:
                    break;
                case ADVERTISE_FAILED_TOO_MANY_ADVERTISERS:
                    break;
                default:
                    Log.wtf(TAG, "Unhandled error: " + errorCode);
            }
        }

        @Override
        public void onStartSuccess(AdvertiseSettings settingsInEffect)
        {
            super.onStartSuccess(settingsInEffect);
            Log.v(TAG, "Broadcasting");
        }
    };

    private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback()
    {
        @Override
        public void onConnectionStateChange(BluetoothDevice device, final int status, int newState)
        {
            super.onConnectionStateChange(device, status, newState);
            if (status == BluetoothGatt.GATT_SUCCESS)
            {
                if (newState == BluetoothGatt.STATE_CONNECTED)
                {
                    mBluetoothDevices.add(device);
                    Log.v(TAG, "Connected to device: " + device.getAddress());
                }
                else if (newState == BluetoothGatt.STATE_DISCONNECTED)
                {
                    mBluetoothDevices.remove(device);
                    Log.v(TAG, "Disconnected from device");
                }
            }
            else
            {
                mBluetoothDevices.remove(device);
                // There are too many gatt errors (some of them not even in the documentation) so we just
                // show the error to the user.
                final String errorMessage = "Error when connecting" + ": " + status;
                runOnUiThread(new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Toast.makeText(BLEActivity.this, errorMessage, Toast.LENGTH_LONG).show();
                    }
                });
                Log.e(TAG, "Error when connecting: " + status);
            }
        }

        @Override
        public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset,
                                                BluetoothGattCharacteristic characteristic)
        {
            super.onCharacteristicReadRequest(device, requestId, offset, characteristic);
            Log.d(TAG, "Device tried to read characteristic: " + characteristic.getUuid());
            Log.d(TAG, "Value: " + Arrays.toString(characteristic.getValue()));
            if (offset != 0)
            {
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
            /* value (optional) */ null);
                return;
            }
            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
                    offset, characteristic.getValue());
        }

        @Override
        public void onNotificationSent(BluetoothDevice device, int status)
        {
            super.onNotificationSent(device, status);
            Log.v(TAG, "Notification sent. Status: " + status);
        }

        @Override
        public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId,
                                                 BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded,
                                                 int offset, byte[] value)
        {
            super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite,
                    responseNeeded, offset, value);
            Log.v(TAG, "Characteristic Write request: " + Arrays.toString(value));
            int status = batteryService.writeCharacteristic(characteristic, offset, value);
            if (responseNeeded)
            {
                gattServer.sendResponse(device, requestId, status,
            /* No need to respond with an offset */ 0,
            /* No need to respond with a value */ null);
            }
        }

        @Override
        public void onDescriptorReadRequest(BluetoothDevice device, int requestId,
                                            int offset, BluetoothGattDescriptor descriptor)
        {
            Log.d(TAG, "Device tried to read descriptor: " + descriptor.getUuid());
            Log.d(TAG, "Value: " + Arrays.toString(descriptor.getValue()));
            super.onDescriptorReadRequest(device, requestId, offset, descriptor);
            if (offset != 0)
            {
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_INVALID_OFFSET, offset,
            /* value (optional) */ null);
                return;
            }
            gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset,
                    descriptor.getValue());
        }

        @Override
        public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
                                             BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
                                             int offset,
                                             byte[] value)
        {
            Log.v(TAG, "Descriptor Write Request " + descriptor.getUuid() + " " + Arrays.toString(value));
            super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded,
                    offset, value);
            descriptor.setValue(value);
            if (responseNeeded)
            {
                gattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,
            /* No need to respond with offset */ 0,
            /* No need to respond with a value */ null);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        mBluetoothDevices = new HashSet<>();

        this.views = new ViewContainer(this);
        this.resources = getResources();
        this.btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        this.btAdapter = btManager.getAdapter();
        this.btAdvertiser = btAdapter.getBluetoothLeAdvertiser();
        this.btScanner = btAdapter.getBluetoothLeScanner();

        this.views.advertise.setOnClickListener(this);
        this.views.listen.setOnClickListener(this);

        listener = new Listener(this, btScanner);
        if (this.btAdapter.isMultipleAdvertisementSupported())
        {
            batteryService = new BatteryService(this);
            bluetoothGattService = batteryService.getBluetoothGattService();

            mAdvSettings = new AdvertiseSettings.Builder()
                    .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_BALANCED)
                    .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                    .setConnectable(true)
                    .build();
            mAdvData = new AdvertiseData.Builder()
                    .setIncludeDeviceName(true)
                    .addServiceUuid(batteryService.getServiceUUID())
                    .build();
        }
    }

    @Override
    public void onClick(View view)
    {
        boolean proximateActiveState = toggleUi(view);
        if (view.getId() == views.advertise.getId())
        {
            if (proximateActiveState)
            {
                ((Button) view).setText(resources.getString(R.string.advertise_btn_stop));
                batteryService.start();
            }
            else
            {
                ((Button) view).setText(resources.getString(R.string.advertise_btn_start));
                if (batteryService != null)
                {
                    batteryService.stop();
                }
            }
        }
        else if (view.getId() == views.listen.getId())
        {
            if (proximateActiveState)
            {
                ((Button) view).setText(resources.getString(R.string.receive_btn_stop));
                if (listener != null)
                {
                    listener.start();
                }
            }
            else
            {
                ((Button) view).setText(resources.getString(R.string.receive_btn_start));
                listener.stop();
            }
        }
        else
        {
            Log.e(BLEActivity.class.getSimpleName(), "Button not recognized in onClick");
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_ENABLE_BT)
        {
            if (resultCode == RESULT_OK)
            {
                onStart();
            }
            else
            {
                Log.e(TAG, "Bluetooth not enabled");
                finish();
            }
        }
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        if (!this.btAdapter.isMultipleAdvertisementSupported())
        {
            Toast.makeText(this, "Multiple advertisement not supported", Toast.LENGTH_SHORT).show();
            this.views.advertise.setEnabled(false);
        }

        if (btAdapter.isMultipleAdvertisementSupported())
        {
            btAdvertiser = btAdapter.getBluetoothLeAdvertiser();
            gattServer = btManager.openGattServer(this, mGattServerCallback);
            if (gattServer != null)
            {
                gattServer.addService(bluetoothGattService);
            }

            btAdvertiser.startAdvertising(mAdvSettings, mAdvData, mAdvScanResponse, mAdvCallback);
        }
    }

    @Override
    protected void onStop()
    {
        super.onStop();
        if (gattServer != null)
        {
            gattServer.close();
        }
        if (btAdapter.isEnabled() && btAdvertiser != null)
        {
            // If stopAdvertising() gets called before close() a null
            // pointer exception is raised.
            btAdvertiser.stopAdvertising(mAdvCallback);
        }
    }

    @Override
    public void sendNotificationToDevices(BluetoothGattCharacteristic characteristic)
    {
        if (mBluetoothDevices.isEmpty())
        {
            Toast.makeText(this, "Bluetooth device is not connected", Toast.LENGTH_SHORT).show();
        }
        else
        {
            boolean indicate = (characteristic.getProperties()
                    & BluetoothGattCharacteristic.PROPERTY_INDICATE)
                    == BluetoothGattCharacteristic.PROPERTY_INDICATE;
            for (BluetoothDevice device : mBluetoothDevices)
            {
                // true for indication (acknowledge) and false for notification (unacknowledge).
                if (gattServer != null)
                {
                    gattServer.notifyCharacteristicChanged(device, characteristic, indicate);
                }
            }
        }
    }

    private boolean toggleUi(View view)
    {
        boolean isActive = view.getTag() != null && ((String) view.getTag()).equals(resources.getString(R.string.btn_active_tag));

        if (isActive)
        {
            view.setBackground(ContextCompat.getDrawable(this, R.drawable.btn_border_primary_color));
            ((Button) view).setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            view.setTag(Constants.EMPTY_STRING);
        }
        else
        {
            view.setBackground(ContextCompat.getDrawable(this, R.drawable.btn_border_app_primary_fill));
            ((Button) view).setTextColor(ContextCompat.getColor(this, android.R.color.white));
            view.setTag(resources.getString(R.string.btn_active_tag));
        }

        return !isActive;
    }

    @Override
    public void notifyAboutAdvert(Object data)
    {
        this.views.scrollFill.setText(
                String.valueOf(this.views.scrollFill.getText()) + System.getProperty("line.separator") + data.toString());
    }

    public static BluetoothGattDescriptor getClientCharacteristicConfigurationDescriptor()
    {
        BluetoothGattDescriptor descriptor = new BluetoothGattDescriptor(
                GattUUIDS.CHAR_CLIENT_CONFIG_UUID,
                (BluetoothGattDescriptor.PERMISSION_READ | BluetoothGattDescriptor.PERMISSION_WRITE));
        descriptor.setValue(new byte[]{0, 0});
        return descriptor;
    }

    private class ViewContainer
    {
        Button advertise;
        Button listen;
        ScrollView scroll;
        TextView scrollFill;

        ViewContainer(Activity activity)
        {
            this.advertise = (Button) activity.findViewById(R.id.advertise);
            this.listen = (Button) activity.findViewById(R.id.receive);
            this.scroll = (ScrollView) activity.findViewById(R.id.scroll);
            this.scrollFill = (TextView) activity.findViewById(R.id.scrollFill);
        }
    }
}
