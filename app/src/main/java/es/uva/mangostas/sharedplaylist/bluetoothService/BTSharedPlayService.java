package es.uva.mangostas.sharedplaylist.bluetoothService;

/**
 * @author Alberto Amigo Alonso
 * @author Sergio Delgado Álvarez
 * @author Óscar Fernández Angulo
 * @author Santos Ángel Prado
 */

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.UUID;

import es.uva.mangostas.sharedplaylist.Constants;
import es.uva.mangostas.sharedplaylist.R;

/**
 * Esta clase hace el trabajo para las conexiones
 * bluetooth se encarga de conectar los dispositivos,
 *  aceptar peticiones y manejar el envio de mensajes.
 *  Tiene 3 hilos diferentes dependiendo del rol en el caso del servidor
 *  un hilo que acepta las peticiones de conexion, otro que realiza la conexion
 *  y un último hilo que gestiona la recepción de datos. Por parte del cliente
 *  se elimina el hilo para aceptar las peticiones y se añade uno para realizar los envios.
 */

public class BTSharedPlayService {

    //UUID del servicio para identificarlo
    private static final UUID MY_UUID = UUID.fromString("fa87c0d0-afac-11de-8a39-0800200c9a66");

    //Atributos para manejar el bluetooth
    private final BluetoothAdapter btAdapter;
    private final Handler mHandler;
    private AcceptThread mAcceptThread;
    private ConnectThread mConnectThread;
    private ConnectedThread mConnectedThread;
    private SendThread mSendThread;
    private int state;
    private final Context context;
    private final String mtype;
    private ArrayList<ConnectedThread> myConnections;
    private static final String SERVER_TYPE = "Server";
    private static final String CLIENT_TYPE = "Client";

    //Constantes para el estado
    public static final int STATE_NONE = 0;       // Nada
    public static final int STATE_LISTEN = 1;     // Escuchando para conexiones
    public static final int STATE_CONNECTING = 2; // Conectando
    public static final int STATE_CONNECTED_AND_LISTEN = 3;  // Conectado a un dispositivo y escuchando para nuevas conexiones
    public static final int STATE_CONNECTED = 4;

    /**
     * Constructor del servicio
     * @param handler Manejador de mensajes para devolver información a la actividad
     */
    public BTSharedPlayService(Context context, Handler handler, String type) {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        state = STATE_NONE;
        mHandler = handler;
        this.context = context;
        mtype = type;
    }

    /**
     * Setter para cambiar el estado
     * @param state Estado que se asignará al servicio.
     */
    private synchronized void setState(int state) {
        this.state = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Getter para el estado
     * @return Estado actual del servicio
     */
    public synchronized int getState() {return state;}

    /**
     * Realiza las funciones necesarias para poner el servicio
     * en funcionamiento. Inicia los hilos y cancela aquellos
     * que no sean necesarios.
     */
    public synchronized void start() {

        //Cancelamos cualquier conexion en curso
        if(mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        //Cancelar conexiones establecidas
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }
        if (mtype.equals(SERVER_TYPE)) {
            setState(STATE_LISTEN);
        }


        //Lanzamos el hilo que se encarga de escuchar peticiones
        if (mtype.equals(SERVER_TYPE)) {
            if (mAcceptThread == null) {
                mAcceptThread = new AcceptThread();
                mAcceptThread.start();
            }
            myConnections = new ArrayList<>();
        }
    }

    /**
     * Lanzamos el hilo que conecta los dispositivos.
     *
     * @param device Dispositivo al cual vamos a realizar la conexión
     */
    public synchronized void connect(BluetoothDevice device) {
        //Cancelamos los hilos que traten de conectarse
        if (state == STATE_CONNECTING) {
            if (mConnectThread != null) {
                mConnectThread.cancel();
                mConnectThread = null;
            }
        }

        //Cancelamos los que ya estan conectados
        if (mtype.equals(CLIENT_TYPE) && mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        //Lanzamos el hilo de conexion
        mConnectThread = new ConnectThread(device);
        mConnectThread.start();
        setState(STATE_CONNECTING);
    }

    /**
     * Lanza el hilo que gestiona la conexión entre dos dispositivos
     * @param socket Socket de la conexión
     * @param device Dispositivo remoto al que se realiza la conexión
     */
    private synchronized void connected(BluetoothSocket socket,
                                        BluetoothDevice device) {
        //Cancelamos los hilos que estan conectando
        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;

        }

        //lanzamos el hilo que maneja la conexion
        if (mtype.equals(SERVER_TYPE)) {
            myConnections.add(new ConnectedThread(socket));
            myConnections.get(myConnections.size()-1).start();

            setState(STATE_CONNECTED_AND_LISTEN);
        } else if (mtype.equals(CLIENT_TYPE)) {
            mConnectedThread = new ConnectedThread(socket);
            mConnectedThread.start();
            setState(STATE_CONNECTED_AND_LISTEN);
            }
        //Enviamos el nombre del dispositivo que se ha conectado de vuelta a la Actividad
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);


    }

    /**
     * Genera el hilo que realiza el envio de información por
     * parte del cliente
     * @param out Array de bytes con los datos a enviar
     */
    public void write(byte[] out) {
        if (mtype.equals(CLIENT_TYPE)) {
            if (mSendThread != null) {
                mSendThread = null;
            }
            mSendThread = new SendThread(mConnectedThread.mmOutStream, out);
            mSendThread.start();
        }
    }

    /**
     * Notifica a la actividad que la conexion ha fallado
     */
    private void connectionFailed() {
        // Envia mensaje de fallo
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, context.getString(R.string.imposibleconectdevice));
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Reiniciar el servicio
        BTSharedPlayService.this.start();
    }

    /**
     * Notifica a la actividad que se ha perdido la conexion
     */
    private void connectionLost() {
        // Enviar mensaje de fallo
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, context.getString(R.string.conectionlost));
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // Reiniciar el servicio
        if (mtype.equals(CLIENT_TYPE)) {
            BTSharedPlayService.this.start();
        }
    }

    /**
     * Clase descendiente de Thread la cual mantiene una escucha
     * y acepta las posibles peticiones de conexion
     */
    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private final String mSocketType;

        public AcceptThread() {
            BluetoothServerSocket tmp;
            mSocketType = "Insecure";

            // Creacción de un nuevo socket de escucha para el servidor
            try {
                tmp = btAdapter.listenUsingRfcommWithServiceRecord("Text",
                        MY_UUID);
            } catch (IOException e) {
                tmp = null;
            }
            mmServerSocket = tmp;
        }

        public void run() {
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket;

            //Esuchando posibles peticiones entrantes
            while (true) {
                try {
                    // Llamada bloqueante la cual devuelve la aceptación de la petición o una
                    // excepción
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    this.start();
                    break;
                }

                // Si se ha aceptado la petición
                if (socket != null) {
                    synchronized (BTSharedPlayService.this) {
                        switch (state) {
                            case STATE_CONNECTED_AND_LISTEN:
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situación normal se lanza el hilo para la gestión de la conexión
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                //No disponible en este momento, se cierra el socket
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                    this.start();
                                    break;
                                }
                                break;
                        }
                    }
                }
            }

        }
    }


    /**
     * Clase descendiente de Thread que se inicia cuando hay una conexión
     * saliente con un dispositivo.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp;

            // Obtenemos un socket bluetooth a partir del dispositivo remoto
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);

            } catch (IOException e) {
                tmp = null;
            }
            mmSocket = tmp;
        }

        public void run() {

            setName("ConnectThread");

            // Cancelamos el escaneo de dispositivos en caso de que se estuviera realizando
            btAdapter.cancelDiscovery();

            try {
                // Llamada bloqueante que devuelve una conexión exitosa o una excepción
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {
                    return;
                }
                connectionFailed();
                return;
            }

            // Reiniciamos el hilo de conexion por que ya hemos terminado
            synchronized (BTSharedPlayService.this) {
                mConnectThread = null;
            }

            // Lanzamos el hilo que gestiona la conexión
            connected(mmSocket, mmDevice);
        }

        /**
         * Cancelar el hilo de conexión
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                this.start();
            }
        }
    }

    /**
     * Clase descendiente de Thread que se encarga de realizar
     * el envio de información hacia el dispositivo vinculado
     */
    private class SendThread extends Thread {
        private final OutputStream mmOutStream;
        private final byte[] songToSend;

        /**
         * Constructor principal
         * @param mmOutStream Stream de salida sobre el que se escribira la información
         * @param song Datos de la canción que se envia
         */
        private SendThread(OutputStream mmOutStream, byte[] song) {
            songToSend = new byte[song.length];
            this.mmOutStream = mmOutStream;
            System.arraycopy(song, 0, songToSend, 0, song.length);
        }

        public void run() {
            try {
                mmOutStream.write(songToSend);
                // Devolvemos a la actividad principal un mensaje de confirmación del envio
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, songToSend.length, -1, songToSend)
                        .sendToTarget();
            } catch (IOException e) {
                this.start();
            }
        }
    }

    /**
     * Clase descendiente de Thread la cual se mantiene durante
     * el transcurso de una conexión, gestionando la recepción de información
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        /**
         * Constructor principal
         * @param socket Socket de conexión
         */
        public ConnectedThread(BluetoothSocket socket) {

            mmSocket = socket;
            InputStream tmpIn;
            OutputStream tmpOut;

            // Obetenemos los sockets de entrada y de salida del socket
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("ERROR: ", "Socket get streams exception", e);
                tmpIn = null;
                tmpOut = null;
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {

            byte[] buffer = new byte[1024];
            byte[] fin = null;
            int bytes;
            int size = 0;
            int totalBytes = 0;

            // Espera activa para recibir información
            while (true) {
                    try {
                        if (totalBytes == 0) {
                            //Leemos el tamaño de la canción en los 4 primeros bytes que se han escrito
                            mmInStream.read(buffer, 0, 4);

                            //Pasamos el valor de los bytes a un entero
                            size = (buffer[0] << 24) & 0xff000000 |
                                    (buffer[1] << 16) & 0x00ff0000 |
                                    (buffer[2] << 8) & 0x0000ff00 |
                                    (buffer[3]) & 0x000000ff;

                            //Si la condicion se cumple se trata de un video por lo tanto lo tratamos como tal.
                            if (size == 0) {
                                bytes = mmInStream.read(buffer);
                                mHandler.obtainMessage(Constants.MESSAGE_VIDEO_READ, bytes, -1, buffer)
                                        .sendToTarget();
                                continue;
                            } else {
                                //Definimos el array que almacenara la cancion con el tamaño de esta
                                fin = new byte[size];
                            }

                        }


                        //Pasamos a leer los datos de la canción
                        bytes = mmInStream.read(buffer);

                        //Guardamos en totalBytes el numero de bytes leidos para poder comprobar si ya hemos
                        //terminado de leer los datos
                        totalBytes += bytes;

                        if (totalBytes == size) {
                            //Si se cumple la condicion ya hemos leido los ultimos bytes de infomación
                            // por lo tanto los copiamos al buffer y enviamos los datos a la
                            // actividad del servidor.
                            System.arraycopy(buffer, 0, fin, totalBytes - bytes + 0, bytes);
                            mHandler.obtainMessage(Constants.MESSAGE_SONG_READ, totalBytes, -1, fin)
                                    .sendToTarget();
                            fin = null;
                            totalBytes = 0;
                        } else if (totalBytes > 0) {
                            //Si no se cumple la condición copiamos los bytes leidos en el buffer final.
                            for (int i = 0; i < bytes; i++) {
                                fin[totalBytes - bytes + i] = buffer[i];
                            }
                        }

                    } catch (IOException e) {

                        connectionLost();
                        // Start the service over to restart listening mode
                        BTSharedPlayService.this.start();
                        break;
                    }
            }
        }

        /**
         * Cancelar el hilo de conexión
         */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                this.start();
            }
        }
    }

}

