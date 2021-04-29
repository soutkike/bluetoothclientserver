package com.example.alienware.usandobluetooth;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.os.Handler;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;


public class BluetoothService {
    //private static final String TAG = "com.example.alienware.usandobluetooth.BluetoothService";

    private static final String NOMBRE_SEGURO = "bt-prueba-123";
    private static UUID UUID_SEGURO;
    private  final UUID MY_UUID = UUID.fromString("0125bb20-d629-11e3-9c1a-0800200c9a66");
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    public static final int ESTADO_NINGUNO          = 0;
    public static final int ESTADO_CONECTADO        = 1;
    public static final int ESTADO_REALIZANDO_CONEXION  = 2;
    public static final int ESTADO_ATENDIENDO_PETICIONES= 3;

    public static final int MSG_NINGUNO = 10;
    public static final int MSG_LEER = 11;
    public static final int MSG_ESCRIBIR = 12;

    private final Handler handler;
    private BluetoothSocket socket;

    private BluetoothAdapter bAdapter;

    private int estado;

    private HiloServidor hiloServidor;
    private HiloCliente hiloCliente;
    private HiloConexion hiloConexion;

    BluetoothService(BluetoothAdapter bAdapter, Handler handler){
        this.bAdapter = bAdapter;
        this.handler = handler;

        UUID_SEGURO = UUID.randomUUID();
    }



    public void iniciarServidor(){
        (new HiloServidor()).start();
    }

    public void iniciarCliente(BluetoothDevice bluetoothDevice){
        (new HiloCliente(bluetoothDevice)).start();
    }

    // Hilo encargado de mantener la conexion y realizar las lecturas y escrituras
    // de los mensajes intercambiados entre dispositivos.
    private class HiloConexion extends Thread
    {
        private final BluetoothSocket  socket;         // Socket
        private final InputStream inputStream;    // Flujo de entrada (lecturas)
        private final OutputStream outputStream;   // Flujo de salida (escrituras)
        private int NEXT_ACCION;

        public HiloConexion(BluetoothSocket socket, int next_action)
        {
            Log.e("HILO-CONEXION", "HiloConexion.Constructor(): Iniciando metodo");
            this.socket = socket;
            setName(socket.getRemoteDevice().getName() + " [" + socket.getRemoteDevice().getAddress() + "]");

            // Se usan variables temporales debido a que los atributos se declaran como final
            // no seria posible asignarles valor posteriormente si fallara esta llamada
            InputStream tmpInputStream = null;
            OutputStream tmpOutputStream = null;

            // Obtenemos los flujos de entrada y salida del socket. MODIFICADO
            try {
                tmpInputStream = socket.getInputStream();
                tmpOutputStream = socket.getOutputStream();
                Log.e("HILO-CONEXION", "HiloConexion(): Se leyeron los flujos");
            }
            catch(IOException e){
                Log.e("HILO-CONEXION", "HiloConexion(): Error al obtener flujos de E/S", e);
            }
            inputStream = tmpInputStream;
            outputStream = tmpOutputStream;

            this.NEXT_ACCION = next_action;
        }

        // Metodo principal del hilo, encargado de realizar las lecturas
        public void run()
        {
            Log.e("HILO-CONEXION", "HiloConexion.run(): Iniciando metodo");
            byte[] buffer = new byte[1024];
            int bytes;
            setEstado(ESTADO_CONECTADO);
            // Mientras se mantenga la conexion el hilo se mantiene en espera ocupada
            // leyendo del flujo de entrada
            while(true)
            {
                try {

                    switch( this.NEXT_ACCION ){
                        case BluetoothService.MSG_LEER:
                            // Leemos del flujo de entrada del socket
                            bytes = inputStream.read(buffer);
                            // Enviamos la informacion a la actividad a traves del handler.
                            // El metodo handleMessage sera el encargado de recibir el mensaje
                            // y mostrar los datos recibidos en el TextView
                            handler.obtainMessage(MSG_LEER, bytes, -1, buffer).sendToTarget();
                            break;

                        case BluetoothService.MSG_ESCRIBIR:
                            String saludo = "0";
                            byte[] out_buffer = saludo.getBytes();
                            bytes = out_buffer.length;
                            outputStream.write(out_buffer);
                            handler.obtainMessage(MSG_ESCRIBIR, bytes, -1, out_buffer).sendToTarget();
                            this.NEXT_ACCION = MSG_NINGUNO;
                            break;
                    }



                }
                catch(IOException e) {
                    Log.e("HILO-CONEXION", "HiloConexion.run(): Error al realizar la lectura", e);
                } /*catch (InterruptedException e) {
                    e.printStackTrace();
                }*/
            }
        }

        public void escribir(byte[] buffer)
        {
            try {
                // Escribimos en el flujo de salida del socket
                outputStream.write(buffer);

                // Enviamos la informacion a la actividad a traves del handler.
                // El metodo handleMessage sera el encargado de recibir el mensaje
                // y mostrar los datos enviados en el Toast

                handler.obtainMessage(MSG_ESCRIBIR, buffer.length, -1, buffer).sendToTarget();
            }
            catch(IOException e) {
                Log.e("HILO-CONEXION", "HiloConexion.escribir(): Error al realizar la escritura", e);
            }
        }
    }

    // Hilo que hace las veces de servidor, encargado de escuchar conexiones entrantes y
    // crear un hilo que maneje la conexion cuando ello ocurra.
    // La otra parte debera solicitar la conexion mediante un HiloCliente.
    private class HiloServidor extends Thread
    {
        private final BluetoothServerSocket serverSocket;

        public HiloServidor()
        {
            BluetoothServerSocket tmpServerSocket = null;

            // Creamos un socket para escuchar las peticiones de conexion
            try {
                tmpServerSocket = bAdapter.listenUsingRfcommWithServiceRecord(NOMBRE_SEGURO, BTMODULEUUID);
                Log.e("HILO-SERVIDOR", "HiloServidor(): Creado");

            } catch(IOException e) {
                Log.e("HILO-SERVIDOR", "HiloServidor(): Error al abrir el socket servidor", e);
            }

            serverSocket = tmpServerSocket;
        }

        public void run()
        {
            Log.e("HILO-SERVIDOR", "HiloServidor.run(): Iniciando metodo");

            BluetoothSocket socket = null;

            setName("HiloServidor");
            setEstado(ESTADO_ATENDIENDO_PETICIONES);
            // El hilo se mantendra en estado de espera ocupada aceptando conexiones
            // entrantes siempre y cuando no exista una conexion activa.
            // En el momento en el que entre una nueva conexion,
            while(estado != ESTADO_CONECTADO)
            {
                Log.e("HILO-SERVIDOR","BUSCANDO CLIENTE");
                try {
                    // Cuando un cliente solicite la conexion se abrirá el socket.
                    socket = serverSocket.accept();
                    Log.e("HILO-SERVIDOR", "Accept: Se conecto cliente");
                }
                catch(IOException e) {
                    Log.e("HILO-SERVIDOR", "HiloServidor.run(): Error al aceptar conexiones entrantes", e);
                    break;
                }
                // Si el socket tiene valor sera porque un cliente ha solicitado la conexion
                if(socket != null)
                {
                    // Realizamos un lock del objeto
                    synchronized(BluetoothService.this)
                    {
                        switch(estado)
                        {
                            case ESTADO_ATENDIENDO_PETICIONES:
                            case ESTADO_REALIZANDO_CONEXION:
                            {
                                // Estado esperado, se crea el hilo de conexion que recibira
                                // y enviara los mensajes
                                hiloConexion = new HiloConexion(socket, MSG_LEER);
                                hiloConexion.start();
                                break;
                            }
                            case ESTADO_NINGUNO:
                            case ESTADO_CONECTADO:
                            {
                                // No preparado o conexion ya realizada. Se cierra el nuevo socket.
                                try {
                                    socket.close();
                                }
                                catch(IOException e) {
                                    Log.e("HILO-SERVIDOR", "HiloServidor.run(): socket.close(). Error al cerrar el socket.", e);
                                }
                                break;
                            }
                            default:
                                break;
                        }
                    }
                }

            } // End while
        }
        public void cancelarConexion()
        {
            try {
                serverSocket.close();
            }
            catch(IOException e) {
                Log.e("HILO-SERVIDOR", "HiloServidor.cancelarConexion(): Error al cerrar el socket", e);
            }
        }
    }

    // Hilo encargado de solicitar una conexion a un dispositivo que este corriendo un
// HiloServidor.
    private class HiloCliente extends Thread {
        private final BluetoothDevice dispositivo;
        private final BluetoothSocket socket;

        public HiloCliente(BluetoothDevice dispositivo) {
            BluetoothSocket tmpSocket = null;
            this.dispositivo = dispositivo;

            // Obtenemos un socket para el dispositivo con el que se quiere conectar
            try {
                //tmpSocket = dispositivo.createRfcommSocketToServiceRecord(MY_UUID);
                tmpSocket = dispositivo.createRfcommSocketToServiceRecord(BTMODULEUUID);
            } catch (IOException e) {
                Log.e("HILO-CLIENTE", "HiloCliente.HiloCliente(): Error al abrir el socket", e);
            }

            socket = tmpSocket;
        }

        public void run()
        {
            Log.e("HILO-CLIENTE", "HiloCliente.run(): Iniciando metodo");
            setName("HiloCliente");
            if(bAdapter.isDiscovering())
                bAdapter.cancelDiscovery();

            try {
                socket.connect();
                setEstado(ESTADO_REALIZANDO_CONEXION);
            }
            catch(IOException e) {
                Log.e("HILO-CLIENTE", "HiloCliente.run(): socket.connect(): Error realizando la conexion", e);
                try {
                    socket.close();
                }
                catch(IOException inner) {
                    Log.e("HILO-CLIENTE", "HiloCliente.run(): Error cerrando el socket", inner);
                }
                setEstado(ESTADO_NINGUNO);
            }

            // Reiniciamos el hilo cliente, ya que no lo necesitaremos mas
            synchronized(BluetoothService.this)
            {
                hiloCliente = null;
            }

            // Realizamos la conexion
            hiloConexion = new HiloConexion(socket, MSG_ESCRIBIR);
            hiloConexion.start();
        }

        public void cancelarConexion()
        {
            Log.e("HILO-CLIENTE", "cancelarConexion():Iniciando metodo");
            try {
                socket.close();
            }
            catch(IOException e) {
                Log.e("HILO-CLIENTE", "HiloCliente.cancelarConexion(): Error al cerrar el socket", e);
            }
            setEstado(ESTADO_NINGUNO);
        }
    }

    public void setEstado(int estado){
        this.estado = estado;
    }


    public void escribir(byte[] buffer){
        if( hiloConexion != null ) {
            hiloConexion.escribir(buffer);
        }
    }
}