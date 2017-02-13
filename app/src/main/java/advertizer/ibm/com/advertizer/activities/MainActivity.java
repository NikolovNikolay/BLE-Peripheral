package advertizer.ibm.com.advertizer.activities;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.bluetooth.le.BluetoothLeScanner;
import android.content.Context;
import android.content.res.Resources;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import advertizer.ibm.com.advertizer.BaseActivity;
import advertizer.ibm.com.advertizer.Broadcaster;
import advertizer.ibm.com.advertizer.Constants;
import advertizer.ibm.com.advertizer.Listener;
import advertizer.ibm.com.advertizer.R;

public class MainActivity extends BaseActivity implements View.OnClickListener, Listener.Notifiable
{
    private ViewContainer views;
    private Resources resources;
    private BluetoothAdapter btAdapter;
    private BluetoothManager btManager;
    private BluetoothLeAdvertiser btAdvertiser;
    private Broadcaster broadcaster;
    private Listener listener;
    private BluetoothLeScanner btScanner;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        this.views = new ViewContainer(this);
        this.resources = getResources();
        this.btManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        this.btAdapter = btManager.getAdapter();
        this.btAdvertiser = btAdapter.getBluetoothLeAdvertiser();
        this.btScanner = btAdapter.getBluetoothLeScanner();
        this.listener = new Listener(this, btScanner);

        this.views.advertise.setOnClickListener(this);
        this.views.listen.setOnClickListener(this);
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
                if (broadcaster == null)
                {
                    broadcaster = new Broadcaster(this, btAdvertiser);
                }
                broadcaster.start();
            }
            else
            {
                ((Button) view).setText(resources.getString(R.string.advertise_btn_start));
                broadcaster.stop();
                broadcaster = null;
            }
        }
        else if (view.getId() == views.listen.getId())
        {
            if (proximateActiveState)
            {
                ((Button) view).setText(resources.getString(R.string.receive_btn_stop));
                if (listener == null)
                {
                    listener = new Listener(this, btScanner);
                }
                listener.start();
            }
            else
            {
                ((Button) view).setText(resources.getString(R.string.receive_btn_start));
                listener.stop();
            }
        }
        else
        {
            Log.e(MainActivity.class.getSimpleName(), "Button not recognized in onClick");
        }
    }

    @Override
    protected void onResume()
    {
        super.onResume();
        if (!this.btAdapter.isMultipleAdvertisementSupported())
        {
            Toast.makeText(this, "Multiple advertisement not supported", Toast.LENGTH_SHORT).show();
            this.views.advertise.setEnabled(false);
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
        StringBuilder sb = new StringBuilder();
        sb.append(this.views.scrollFill.getText()).append(System.getProperty("line.separator")).append(data.toString());

        this.views.scrollFill.setText(sb.toString());
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
