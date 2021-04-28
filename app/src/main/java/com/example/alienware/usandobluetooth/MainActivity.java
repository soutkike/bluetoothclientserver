package com.example.alienware.usandobluetooth;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private Button btnBluetooth;
    private BluetoothAdapter bAdapter;

    private static final int    REQUEST_ENABLE_BT   = 1;

    private Button btnBuscarDispositivo;
    private ArrayList<BluetoothDevice> arrayDevices;

    private ListView lvDispositivos;

    private TextView tvMensaje;

    private EditText etSendData;

    BluetoothService obj;
    BluetoothDevice bluetoothDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnBluetooth  = (Button)findViewById(R.id.btnBluetooth);
        btnBuscarDispositivo = (Button)findViewById(R.id.btnBuscarDispositivo);
        lvDispositivos = (ListView)findViewById(R.id.lvDispositivos);
        tvMensaje = (TextView)findViewById(R.id.tvMensaje);
        etSendData = (EditText)findViewById((R.id.etDataSend));

        lvDispositivos.setOnItemClickListener(new ListView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                bluetoothDevice = arrayDevices.get(position);

                Toast.makeText(getApplicationContext(), "" + bluetoothDevice.getName(), Toast.LENGTH_SHORT).show();
            }
        });
        // Obtenemos el adaptador Bluetooth. Si es NULL, significara que el
        // dispositivo no posee Bluetooth, por lo que deshabilitamos el boton
        // encargado de activar/desactivar esta caracteristica.
        bAdapter = BluetoothAdapter.getDefaultAdapter();
        if(bAdapter == null)
        {
            btnBluetooth.setEnabled(false);
            Toast.makeText(this,"Device does not support Bluettoth", Toast.LENGTH_SHORT);
            return;
        }

        // Comprobamos si el Bluetooth esta activo y cambiamos el texto del
        // boton dependiendo del estado.
        if(bAdapter.isEnabled()){
            btnBluetooth.setText("Desactivar");
            btnBuscarDispositivo.setEnabled(true);
        }
        else
            btnBluetooth.setText("Activar");

        String[] perms = {"android.permission.ACCESS_FINE_LOCATION", "android.permission.ACCESS_COARSE_LOCATION", "android.permission.BLUETOOTH", "android.permission.BLUETOOTH_ADMIN"};

        BluetoothAdmin.getPermissions(perms, this);

        BluetoothAdmin.registrarEventosBluetooth(this, bReceiver);

        obj = new BluetoothService(bAdapter, handler);
    }

    // Instanciamos un BroadcastReceiver que se encargara de detectar si el estado
    // del Bluetooth del dispositivo ha cambiado mediante su handler onReceive
    private final BroadcastReceiver bReceiver = new BroadcastReceiver()
    {

        @Override
        public void onReceive(Context context, Intent intent)
        {
            final String action = intent.getAction();

            // Filtramos por la accion. Nos interesa detectar BluetoothAdapter.ACTION_STATE_CHANGED
            if (BluetoothAdapter.ACTION_STATE_CHANGED.equals(action))
            {
                Log.e("ON-RECIEVE", "ACTION_STATE_CHANGED");

                // Solicitamos la informacion extra del intent etiquetada como BluetoothAdapter.EXTRA_STATE
                // El segundo parametro indicara el valor por defecto que se obtendra si el dato extra no existe
                final int estado = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE,
                        BluetoothAdapter.ERROR);

                switch (estado)
                {
                    // Apagado
                    case BluetoothAdapter.STATE_OFF:
                    {
                        ((Button)findViewById(R.id.btnBluetooth)).setText("Activar");
                        break;
                    }

                    // Encendido
                    case BluetoothAdapter.STATE_ON:
                    {
                        ((Button)findViewById(R.id.btnBluetooth)).setText("Desactivar");
                        break;
                    }
                    default:
                        break;
                }
            }
            // Cada vez que se descubra un nuevo dispositivo por Bluetooth, se ejecutara
            // este fragmento de codigo
            else if (BluetoothDevice.ACTION_FOUND.equals(action))
            {
                Log.e("ON-RECIEVE", "ACTION_FOUND");
                // Acciones a realizar al descubrir un nuevo dispositivo

                // Si el array no ha sido aun inicializado, lo instanciamos
                if(arrayDevices == null)
                    arrayDevices = new ArrayList<BluetoothDevice>();

                // Extraemos el dispositivo del intent mediante la clave BluetoothDevice.EXTRA_DEVICE
                BluetoothDevice dispositivo = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);

                // Añadimos el dispositivo al array
                arrayDevices.add(dispositivo);

                // Le asignamos un nombre del estilo NombreDispositivo [00:11:22:33:44]
                String descripcionDispositivo = dispositivo.getName() + " [" + dispositivo.getAddress() + "]";

                // Mostramos que hemos encontrado el dispositivo por el Toast
                Toast.makeText(getBaseContext(), "Dispositivo Detectado: " + descripcionDispositivo, Toast.LENGTH_SHORT).show();
            }
            // Codigo que se ejecutara cuando el Bluetooth finalice la busqueda de dispositivos.
            else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action))
            {
                Log.e("ON-RECIEVE", "ACTION_DISCOVERY_FINISHED");

                // Acciones a realizar al finalizar el proceso de descubrimiento
                // Instanciamos un nuevo adapter para el ListView mediante la clase que acabamos de crear
                ArrayAdapter arrayAdapter = new BluetoothDeviceArrayAdapter(getBaseContext(), android.R.layout.simple_list_item_2, arrayDevices);

                lvDispositivos.setAdapter(arrayAdapter);
                Toast.makeText(getBaseContext(), "Fin de la búsqueda", Toast.LENGTH_SHORT).show();
            }
        }
    };

    public void onClick(View v){
        switch( v.getId() ){
            case R.id.btnBluetooth:
                if( bAdapter.isEnabled() ){
                    bAdapter.disable();
                    btnBuscarDispositivo.setEnabled(false);
                }else
                {
                    // Lanzamos el Intent que mostrara la interfaz de activacion del
                    // Bluetooth. La respuesta de este Intent se manejara en el metodo
                    // onActivityResult
                    Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
                }
                break;

            case R.id.btnBuscarDispositivo:
                if(arrayDevices != null)
                    arrayDevices.clear();

                // Comprobamos si existe un descubrimiento en curso. En caso afirmativo, se cancela.
                if(bAdapter.isDiscovering())
                    bAdapter.cancelDiscovery();

                // Iniciamos la busqueda de dispositivos y mostramos el mensaje de que el proceso ha comenzado
                if(bAdapter.startDiscovery())
                    Toast.makeText(this, "Iniciando búsqueda de dispositivos bluetooth", Toast.LENGTH_SHORT).show();
                else
                    Toast.makeText(this, "Error al iniciar búsqueda de dispositivos bluetooth", Toast.LENGTH_SHORT).show();

                break;

        }

    }

    public void onDataSend(View v){
        String data = etSendData.getText().toString();
        byte[] buffer = data.getBytes();

        obj.escribir(buffer);

        etSendData.setText("" + (1 - Integer.valueOf(data))%2) ;
    }

    /**
     * Handler del evento desencadenado al retornar de una actividad. En este caso, se utiliza
     * para comprobar el valor de retorno al lanzar la actividad que activara el Bluetooth.
     * En caso de que el usuario acepte, resultCode sera RESULT_OK
     * En caso de que el usuario no acepte, resultCode valdra RESULT_CANCELED
     */
    @Override
    protected void onActivityResult (int requestCode, int resultCode, Intent data) {
        switch(requestCode)
        {
            case REQUEST_ENABLE_BT:
            {
                if(resultCode == RESULT_OK)
                {
                    // Acciones adicionales a realizar si el usuario activa el Bluetooth
                    Log.e("ON-ACTIVITY-RESULT", "RESULT_OK");
                    btnBuscarDispositivo.setEnabled(true);
                }
                else
                {
                    // Acciones adicionales a realizar si el usuario no activa el Bluetooth
                    Log.e("ON-ACTIVITY-RESULT", "RESULT_NO_OK");
                    btnBuscarDispositivo.setEnabled(false);
                }
                break;
            }

            default:
                break;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        this.unregisterReceiver(bReceiver);
    }

    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults){
        Log.e("OR-PERMISSIONS", "Respuesta");

        switch(permsRequestCode){
            case 200:
                boolean fineLocationAccepted = grantResults[0]==PackageManager.PERMISSION_GRANTED;
                boolean coarseLocationAccepted = grantResults[1]==PackageManager.PERMISSION_GRANTED;

                Log.e("OR-PERMISSIONS-200", "Respuesta a solicitud FINE: [" + fineLocationAccepted + "] + COARSE: [" + coarseLocationAccepted + "] + ");

                if( fineLocationAccepted && coarseLocationAccepted ){
                }

                break;

        }
    }

    //Looper: https://medium.com/better-programming/a-detailed-story-about-handler-thread-looper-message-queue-ac2cd9be0d78

    // Handler que obtendrá informacion de BluetoothService
    private final Handler handler = new Handler() {

        @SuppressLint("HandlerLeak")
        @Override
        public void handleMessage(Message msg) {
            byte[] buffer = null;
            String mensaje = null;

            // Atendemos al tipo de mensaje
            switch (msg.what) {
                // Mensaje de lectura: se mostrara en un TextView
                case BluetoothService.MSG_LEER: {
                    buffer = (byte[]) msg.obj;
                    mensaje = new String(buffer, 0, msg.arg1);
                    tvMensaje.setText(mensaje);
                    break;
                }

                // Mensaje de escritura: se mostrara en el Toast
                case BluetoothService.MSG_ESCRIBIR: {
                    buffer = (byte[]) msg.obj;
                    mensaje = new String(buffer);
                    mensaje = "Enviando mensaje: " + mensaje;
                    Toast.makeText(getApplicationContext(), mensaje, Toast.LENGTH_LONG).show();
                    break;
                }

                default:
                    break;
            }
        }
    };

    public void onServer(View v) {
        obj.iniciarServidor();
    }

    public void onClient(View v) {
        obj.iniciarCliente(bluetoothDevice);
    }

}