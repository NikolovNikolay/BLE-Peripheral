package advertizer.ibm.com.advertizer.activities;

import android.content.pm.PackageManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import advertizer.ibm.com.advertizer.Constants;
import advertizer.ibm.com.advertizer.utils.PermissionModule;

/**
 * Created by Nikolay Nikolov
 */

public class BaseActivity extends AppCompatActivity
{
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults)
    {
        switch (requestCode)
        {
            case Constants.REQUEST_FEATURE_PERMISSIONS:
            {
                for (int i = 0; i < permissions.length; i++)
                {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED)
                    {
                        Log.d("Permissions", "Permission Granted: " + permissions[i]);
                    }
                    else if (grantResults[i] == PackageManager.PERMISSION_DENIED)
                    {
                        Log.d("Permissions", "Permission Denied: " + permissions[i]);
                    }
                }

                Toast.makeText(this, "Permissions granted", Toast.LENGTH_SHORT).show();
            }
            break;
            default:
            {
                super.onRequestPermissionsResult(requestCode, permissions, grantResults);
                Toast.makeText(this, "Permissions not granted", Toast.LENGTH_SHORT).show();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    protected void onResume()
    {
        super.onPostResume();
        PermissionModule.requestPermissions(this);
    }
}
