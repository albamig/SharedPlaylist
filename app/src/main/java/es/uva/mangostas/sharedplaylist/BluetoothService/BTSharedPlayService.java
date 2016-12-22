package es.uva.mangostas.sharedplaylist.BluetoothService;

/**
 * Created by root on 1/12/16.
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

import es.uva.mangostas.sharedplaylist.R;

/**
 * Esta clase hace el trabajo para las conexiones
 * bluetooth se encarga de conectar los dispositivos,
 *  aceptar peticiones y manejar el envio de mensajes.
 *  Tiene 3 hilos uno que acepta las peticiones, otro
 *  que maneja la conexion y un ultimo hilo que se
 *  encarga de la transmision de informacion.
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
    private String mtype;
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
     * @param context
     * @param handler
     */
    public BTSharedPlayService(Context context, Handler handler, String type) {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        state = STATE_NONE;
        mHandler = handler;
        mtype = type;
    }

    /**
     * Setter para cambiar el estado
     * @param state
     */
    private synchronized void setState(int state) {
        this.state = state;

        // Give the new state to the Handler so the UI Activity can update
        mHandler.obtainMessage(Constants.MESSAGE_STATE_CHANGE, state, -1).sendToTarget();
    }

    /**
     * Getter para el estado
     * @return
     */
    public synchronized int getState() {return state;}

    /**
     * Metodo con el que iniciamos el servicio cancelando cualquier conexion,
     * en curso o ya establecida, despues lanzamos el hilo que espera peticiones.
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
     * Lanzamos el hilo que conecta los dispositivos
     * @param device
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

    public synchronized void connected(BluetoothSocket socket,
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
     * Detener todos los hilos
     */
    public synchronized void stop() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mtype.equals(CLIENT_TYPE) && mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        } else if (mtype.equals(SERVER_TYPE) && myConnections.size() > 0) {
            for (int i = 0; i < myConnections.size(); i++) {
                myConnections.get(i).cancel();
                myConnections.remove(myConnections.get(i));
            }
        }

        if (mAcceptThread != null) {
            mAcceptThread.cancel();
            mAcceptThread = null;
        }

        setState(STATE_NONE);
    }

    /**
     * Metodo para escribir al dispositivo conectado
     * @param out
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
        bundle.putString(Constants.TOAST, String.valueOf(R.string.imposibleconectdevice));
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
        bundle.putString(Constants.TOAST, String.valueOf(R.string.conectionlost));
        msg.setData(bundle);
        mHandler.sendMessage(msg);
        // Reiniciar el servicio
        if (mtype.equals(CLIENT_TYPE)) {
            BTSharedPlayService.this.start();
        }
    }

    private class AcceptThread extends Thread {
        // The local server socket
        private final BluetoothServerSocket mmServerSocket;
        private String mSocketType;

        public AcceptThread() {
            BluetoothServerSocket tmp = null;
            mSocketType = "Insecure";

            // Create a new listening server socket
            try {
                tmp = btAdapter.listenUsingRfcommWithServiceRecord("Text",
                        MY_UUID);
            } catch (IOException e) {

            }
            mmServerSocket = tmp;
        }

        public void run() {
            setName("AcceptThread" + mSocketType);

            BluetoothSocket socket = null;

            // Listen to the server socket if we're not connected
            while (true) {
                try {
                    // This is a blocking call and will only return on a
                    // successful connection or an exception
                    socket = mmServerSocket.accept();
                } catch (IOException e) {
                    break;
                }

                // If a connection was accepted
                if (socket != null) {
                    synchronized (BTSharedPlayService.this) {
                        switch (state) {
                            case STATE_CONNECTED_AND_LISTEN:
                            case STATE_LISTEN:
                            case STATE_CONNECTING:
                                // Situation normal. Start the connected thread.
                                connected(socket, socket.getRemoteDevice());
                                break;
                            case STATE_NONE:
                            case STATE_CONNECTED:
                                // Either not ready or already connected. Terminate new socket.
                                try {
                                    socket.close();
                                } catch (IOException e) {
                                }
                                break;
                        }
                    }
                }
            }

        }

        public void cancel() {
            try {
                mmServerSocket.close();
            } catch (IOException e) {
            }
        }
    }


    /**
     * This thread runs while attempting to make an outgoing connection
     * with a device. It runs straight through; the connection either
     * succeeds or fails.
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {
            mmDevice = device;
            BluetoothSocket tmp = null;

            // Get a BluetoothSocket for a connection with the
            // given BluetoothDevice
            try {
                tmp = device.createRfcommSocketToServiceRecord(MY_UUID);

            } catch (IOException e) {
            }
            mmSocket = tmp;
        }

        public void run() {

            setName("ConnectThread");

            // Always cancel discovery because it will slow down a connection
            btAdapter.cancelDiscovery();

            // Make a connection to the BluetoothSocket
            try {
                // This is a blocking call and will only return on a
                // successful connection or an exception
                mmSocket.connect();
            } catch (IOException e) {
                // Close the socket
                try {
                    mmSocket.close();
                } catch (IOException e2) {

                }
                connectionFailed();
                return;
            }

            // Reset the ConnectThread because we're done
            synchronized (BTSharedPlayService.this) {
                mConnectThread = null;
            }

            // Start the connected thread
            connected(mmSocket, mmDevice);
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {

            }
        }
    }

    private class SendThread extends Thread {
        private OutputStream mmOutStream;
        private byte[] songToSend;

        private SendThread(OutputStream mmOutStream, byte[] song) {
            songToSend = new byte[song.length];
            this.mmOutStream = mmOutStream;
            for (int i = 0; i < song.length; i++) {
                songToSend[i] = song[i];
            }
        }

        public void run() {
            try {
                mmOutStream.write(songToSend);
                // Share the sent message back to the UI Activity
                mHandler.obtainMessage(Constants.MESSAGE_WRITE, songToSend.length, -1, songToSend)
                        .sendToTarget();
            } catch (IOException e) {
            }
        }
    }

    /**
     * This thread runs during a connection with a remote device.
     * It handles all incoming and outgoing transmissions.
     */
    private class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        public ConnectedThread(BluetoothSocket socket) {

            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            // Get the BluetoothSocket input and output streams
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e("ERROR: ", "Socket get streams exception", e);
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
            int listSize = 0;

            // Keep listening to the InputStream while connected
            while (true) {
                    try {
                        if (totalBytes == 0) {
                            //Leemos el tamaño de la canción en los 4 primeros bytes que se han escrito
                            mmInStream.read(buffer, 0, 4);

                            //Pasamos el valor de los bytes a un entero
                            size = (buffer[0] << 24) & 0xff000000 |
                                    (buffer[1] << 16) & 0x00ff0000 |
                                    (buffer[2] << 8) & 0x0000ff00 |
                                    (buffer[3] << 0) & 0x000000ff;

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
                            for (int i = 0; i < bytes; i++) {
                                fin[totalBytes - bytes + i] = buffer[i];
                            }
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
         * Write to the connected OutStream.
         *
         * @param buffer The bytes to write
         */
        public void write(byte[] buffer) {
            try {
                mmOutStream.write(buffer);

                // Share the sent message back to the UI Activity
               mHandler.obtainMessage(Constants.MESSAGE_WRITE, -1, -1, buffer)
                       .sendToTarget();
            } catch (IOException e) {
            }
        }

        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
            }
        }
    }

}

