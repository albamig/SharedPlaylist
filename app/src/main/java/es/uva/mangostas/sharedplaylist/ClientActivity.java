package es.uva.mangostas.sharedplaylist;

import android.app.Activity;
import android.app.DialogFragment;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import es.uva.mangostas.sharedplaylist.BluetoothService.BTSharedPlayService;
import es.uva.mangostas.sharedplaylist.BluetoothService.Constants;
import es.uva.mangostas.sharedplaylist.BluetoothService.DeviceListActivity;
import es.uva.mangostas.sharedplaylist.Model.ShpMediaObject;
import es.uva.mangostas.sharedplaylist.Model.ShpSong;
import es.uva.mangostas.sharedplaylist.Model.ShpVideo;

public class ClientActivity extends AppCompatActivity {


    //Codigos de los Intent
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final int REQUEST_ENABLE_BT = 3;
    private static final String TYPE = "Client";
    private ListView listView;
    private ArrayList<ShpMediaObject> playList;
    private FloatingActionButton addVideoButton;
    private ArrayAdapter<ShpMediaObject> adapter;
    private BluetoothAdapter btAdapter;
    private BTSharedPlayService mService;
    private BluetoothDevice device;


    //Manejador para devolver información al servicio
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BTSharedPlayService.STATE_CONNECTED:
                            break;
                        case BTSharedPlayService.STATE_CONNECTING:
                            break;
                        case BTSharedPlayService.STATE_LISTEN:
                        case BTSharedPlayService.STATE_NONE:
                            break;
                    }
                    break;
                case Constants.MESSAGE_WRITE:
                    byte[] writeBuf = (byte[]) msg.obj;
                    // construct a string from the buffer
                    String writeMessage = new String(writeBuf);
                    adapter.add(new ShpVideo(writeMessage));
                    break;
                case Constants.MESSAGE_VIDEO_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    Toast.makeText(getApplicationContext(), "Cancion Añadida", Toast.LENGTH_LONG).show();
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    adapter.add(new ShpVideo(readMessage));
                    break;
                case Constants.MESSAGE_DEVICE_NAME:

                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != getApplicationContext()) {
                        Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
            }
        }
    };

    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.client);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        addVideoButton =(FloatingActionButton)findViewById(R.id.addVideo);
        addVideoButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                File song = new File("/storage/emulated/0/Music/C. Tangana - 10_15 (2015)/1 C.H.I.T.O..mp3");
                byte[] songArray = new byte[(int) song.length()+4];
                int tam = (int) song.length();
                Log.d("Tamaño", ""+tam);
                songArray[0] = (byte) (tam >> 24);
                songArray[1] = (byte) (tam >> 16);
                songArray[2] = (byte) (tam >> 8);
                songArray[3] = (byte) (tam /*>> 0*/);
                Log.d("Tamaño despues", ""+tam);
                try {
                    FileInputStream fis = new FileInputStream(song);
                    fis.read(songArray, 4, (int)song.length());
                    fis.close();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (mService.getState() != BTSharedPlayService.STATE_CONNECTED_AND_LISTEN) {
                    Toast.makeText(getApplicationContext(), "No se puede enviar sin conexion", Toast.LENGTH_LONG).show();

                } else {
                    mService.write(songArray);
                }


                /**  File song = new File("/storage/emulated/0/Music/C. Tangana - 10_15 (2015)/1 C.H.I.T.O..mp3");
                Intent intent = new Intent();
                intent.setAction(Intent.ACTION_SEND);
                intent.setType("music/*");

                intent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(song));
                intent.putExtra(BluetoothDevice.EXTRA_NAME, device.getName());
                PackageManager pm = getPackageManager();

                List<ResolveInfo> appsList = pm.queryIntentActivities( intent, 0);

                if(appsList.size() > 0 ){
                    String packageName = null;
                    String className = null;
                    boolean found = false;
                    for(ResolveInfo info: appsList) {

                        packageName = info.activityInfo.packageName;

                        if( packageName.equals("com.android.bluetooth")){
                            className = info.activityInfo.name;
                            found = true;
                            break;

                        }

                    }
                    intent.setClassName(packageName, className);
                    startActivity(intent);
                }*/

            }
        });
        // Get ListView object from xml
        listView = (ListView) findViewById(R.id.listView);

        // Defined Array playList to show in ListView
        playList = new ArrayList<>();


        // Define a new Adapter
        // First parameter - Context
        // Second parameter - Layout for the row
        // Third parameter - ID of the TextView to which the data is written
        // Forth - the Array of data

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, playList);


        // Assign adapter to ListView
        listView.setAdapter(adapter);

        // ListView Item Click Listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item index
                int itemPosition = position;

                // ListView Clicked item value
                String itemValue = (String) listView.getItemAtPosition(position);

                // Show Alert
                Toast.makeText(getApplicationContext(),
                        "Position :" + itemPosition + "  ListItem : " + itemValue, Toast.LENGTH_LONG)
                        .show();

            }

        });


        setupService();

    }

    /**
     * Ponemos en funcionamiento el servicio para conectarnos.
     */
    private void setupService() {
        //Inicializamos el servicio de Envio.
        mService = new BTSharedPlayService(getApplicationContext(), mHandler, TYPE);
        mService.start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_options, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.insecure_connect_scan: {
                // Launch the DeviceListActivity to see devices and do scan
                Intent serverIntent = new Intent(getApplicationContext(), DeviceListActivity.class);
                startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE_INSECURE);
                return true;
            }
            case R.id.discoverable: {
                // Hacer el dispositivo visible
                ensureDiscoverable();
                return true;
            }
        }
        return false;
    }

    /**
     * Metodo para hacer el dispositivo visible para los demas
     */
    private void ensureDiscoverable() {
        if(btAdapter.getScanMode() !=
                BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }


    private void sendMessage(String msg) {
        //Comprobamos que estamos conectados antes de enviar
        if (mService.getState() != BTSharedPlayService.STATE_CONNECTED_AND_LISTEN) {
            Toast.makeText(getApplicationContext(), "No es posible enviar sin una conexion", Toast.LENGTH_LONG).show();
            return;
        }
        //Comprobamos que hay algo en el mensaje para enviar
        if (msg.length() > 0) {
            //Obtenemos los bytes del mensaje y escribimos a traves del servicio
            byte[] message = msg.getBytes();
            mService.write(message);
            //Reseteamos el buffer y limpiamos la entrada de texto
        }
    }
    private void connectDevice(Intent data) {
        //Obtenemos la MAC
        String address = data.getExtras()
                .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
        //Obtenemos el objeto de dispositivo
        device = btAdapter.getRemoteDevice(address);
        //Intentamos conectar
        mService.connect(device);
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // When DeviceListActivity returns with a device to connect
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data);
                }
                break;
            case REQUEST_ENABLE_BT:
                // When the request to enable Bluetooth returns
                if (resultCode == Activity.RESULT_OK) {
                    // Bluetooth is now enabled, so set up a chat session
                    setupService();
                } else {
                    // User did not enable Bluetooth or an error occurred
                    finish();
                }
        }
    }

}


