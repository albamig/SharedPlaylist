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
import java.util.UUID;

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
    private int state;

    //Constantes para el estado
    public static final int STATE_NONE = 0;       // Nada
    public static final int STATE_LISTEN = 1;     // Escuchando para conexiones
    public static final int STATE_CONNECTING = 2; // Conectando
    public static final int STATE_CONNECTED = 3;  // Conectado a un dispositivo

    /**
     * Constructor del servicio
     * @param context
     * @param handler
     */
    public BTSharedPlayService(Context context, Handler handler) {
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        state = STATE_NONE;
        mHandler = handler;
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

        setState(STATE_LISTEN);

        //Lanzamos el hilo que se encarga de escuchar peticiones
        if (mAcceptThread == null) {
            mAcceptThread = new AcceptThread();
            mAcceptThread.start();
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
        if (mConnectedThread != null) {
            mConnectThread.cancel();
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
        //Cancelamos los que estan conectados
        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
        }

        //lanzamos el hilo que maneja la conexion
        mConnectedThread = new ConnectedThread(socket);
        mConnectedThread.start();

        //Enviamos el nombre del dispositivo que se ha conectado de vuelta a la Actividad
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_DEVICE_NAME);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.DEVICE_NAME, device.getName());
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        setState(STATE_CONNECTED);
    }

    /**
     * Detener todos los hilos
     */
    public synchronized void stop() {

        if (mConnectThread != null) {
            mConnectThread.cancel();
            mConnectThread = null;
        }

        if (mConnectedThread != null) {
            mConnectedThread.cancel();
            mConnectedThread = null;
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

        //Copia temporal del hilo para realizar el envio del mensaje
        ConnectedThread r;
        //Sincronizamos la copia con el original para enviar el mesnaje.
        synchronized (this) {
            if (state != STATE_CONNECTED) return;
            r = mConnectedThread;
        }

        r.write(out);
    }

    /**
     * Notifica a la actividad que la conexion ha fallado
     */
    private void connectionFailed() {
        // Envia mensaje de fallo
        Message msg = mHandler.obtainMessage(Constants.MESSAGE_TOAST);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.TOAST, "Unable to connect device");
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
        bundle.putString(Constants.TOAST, "Device connection was lost");
        msg.setData(bundle);
        mHandler.sendMessage(msg);

        // Reiniciar el servicio
        BTSharedPlayService.this.start();
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
            while (state != STATE_CONNECTED) {
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
                Log.d("EXCEPTION: ", "Socket get streams exception");
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {

            byte[] buffer = new byte[1024];
            int bytes;

            // Keep listening to the InputStream while connected
            while (state == STATE_CONNECTED) {
                try {
                    // Read from the InputStream
                    bytes = mmInStream.read(buffer);

                    // Send the obtained bytes to the UI Activity
                    mHandler.obtainMessage(Constants.MESSAGE_READ, bytes, -1, buffer)
                            .sendToTarget();
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

