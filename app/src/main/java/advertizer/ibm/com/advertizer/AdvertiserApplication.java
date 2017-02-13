package advertizer.ibm.com.advertizer;

import android.app.Application;
import android.util.Log;

/**
 * Created by Nikolay Nikolov
 */

public class AdvertiserApplication extends Application
{
    @Override
    public void onCreate()
    {
        super.onCreate();
        Log.i(AdvertiserApplication.class.getSimpleName(), "App created");
    }
}
