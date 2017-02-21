package advertizer.ibm.com.advertizer;

import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.os.ParcelUuid;

/**
 * Created by Nikolay Nikolov
 */
public abstract class BLEService
{
    public abstract BluetoothGattService getBluetoothGattService();

    public abstract ParcelUuid getServiceUUID();

    public abstract void start();

    public abstract void stop();

    public int writeCharacteristic(BluetoothGattCharacteristic characteristic, int offset, byte[] value)
    {
        throw new UnsupportedOperationException("Method writeCharacteristic not overriden");
    }

    public interface ServiceFragmentDelegate
    {
        void sendNotificationToDevices(BluetoothGattCharacteristic characteristic);
    }
}
