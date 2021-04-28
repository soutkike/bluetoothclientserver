package com.example.alienware.usandobluetooth;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import java.util.ArrayList;

public class BluetoothAdmin {

    public static void registrarEventosBluetooth(Activity act, BroadcastReceiver bReceiver)
    {
        IntentFilter filtro;

        // Registramos el BroadcastReceiver que instanciamos previamente para
        // detectar los distintos eventos que queremos recibir
        filtro = new IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED);
        act.registerReceiver(bReceiver, filtro);

        filtro = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        act.registerReceiver(bReceiver, filtro);

        filtro = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        act.registerReceiver(bReceiver, filtro);
    }

    public static void getPermissions(String[] permissions, Activity activity) {
        if(Build.VERSION.SDK_INT>=23) {
            Log.e("DATA", "API >= 23");
            ArrayList<String> preToDo = new ArrayList<>();
            boolean tip = false;
            for (String pre : permissions) {
                if (activity.checkSelfPermission(pre) != PackageManager.PERMISSION_GRANTED) {
                    preToDo.add(pre);
                    if (activity.shouldShowRequestPermissionRationale(pre)) {
                        tip = true;
                    }
                }
            }
            if (preToDo.size() == 0)
                return;

            if (tip)
                Toast.makeText(activity, "Permission needed [" + permissions[0] + "]", Toast.LENGTH_SHORT).show();

            int permsRequestCode = 200;
            activity.requestPermissions(preToDo.toArray(new String[preToDo.size()]), permsRequestCode);
        }
    }
}
