package advertizer.ibm.com.advertizer.utils;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;

import java.util.ArrayList;
import java.util.List;

import advertizer.ibm.com.advertizer.Constants;

/**
 * Created by Nikolay Nikolov
 */

public class PermissionModule
{
    public static void requestPermissions(Activity context)
    {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
        {
            int hasLocationPermission = context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION);
            int hasBlPermission = context.checkSelfPermission(Manifest.permission.BLUETOOTH);
            int hasBlAdminPermission = context.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN);
            List<String> permissions = new ArrayList<String>();

            if (hasLocationPermission != PackageManager.PERMISSION_GRANTED)
            {
                permissions.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }

            if (hasBlPermission != PackageManager.PERMISSION_GRANTED)
            {
                permissions.add(Manifest.permission.BLUETOOTH);
            }

            if (hasBlAdminPermission != PackageManager.PERMISSION_GRANTED)
            {
                permissions.add(Manifest.permission.BLUETOOTH_ADMIN);
            }

            if (!permissions.isEmpty())
            {
                context.requestPermissions(permissions.toArray(new String[permissions.size()]), Constants.REQUEST_FEATURE_PERMISSIONS);
            }
        }
    }
}
