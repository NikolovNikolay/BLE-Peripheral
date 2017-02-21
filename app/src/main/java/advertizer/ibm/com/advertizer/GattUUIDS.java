package advertizer.ibm.com.advertizer;

import java.util.UUID;

/**
 * Created by Nikolay Nikolov
 */

public class GattUUIDS
{
    public static final UUID DESC_PRESENTATION_FORMAT_UUID = UUID
            .fromString("00002904-0000-1000-8000-00805f9b34fb");

    public static final UUID CHAR_USER_DESCRIPTION_UUID = UUID
            .fromString("00002901-0000-1000-8000-00805f9b34fb");

    public static final UUID CHAR_CLIENT_CONFIG_UUID = UUID
            .fromString("00002902-0000-1000-8000-00805f9b34fb");

    public static final UUID SERVICE_BATTERY_UUID =
            UUID.fromString("0000180F-0000-1000-8000-00805f9b34fb");

    public static final UUID CHAR_BATTERY_LEVEL_UUID =
            UUID.fromString("00002a19-0000-1000-8000-00805f9b34fb");
}
