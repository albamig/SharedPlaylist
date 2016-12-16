package es.uva.mangostas.sharedplaylist;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.quinny898.library.persistentsearch.SearchBox;
import com.quinny898.library.persistentsearch.SearchResult;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import es.uva.mangostas.sharedplaylist.BluetoothService.BTSharedPlayService;
import es.uva.mangostas.sharedplaylist.BluetoothService.Constants;
import es.uva.mangostas.sharedplaylist.BluetoothService.DeviceListActivity;
import es.uva.mangostas.sharedplaylist.Model.ShpMediaObject;
import es.uva.mangostas.sharedplaylist.Model.ShpVideo;

public class ClientActivity extends AppCompatActivity {


    //Codigos de los Intent
    private static final int REQUEST_CONNECT_DEVICE_INSECURE = 2;
    private static final String TYPE = "Client";
    private static final int SELECT_VIDEO = 1;

    private ListView listView;
    private ArrayList<ShpMediaObject> playList;
    private ArrayAdapter<ShpMediaObject> adapter;
    private BluetoothAdapter btAdapter;
    private BTSharedPlayService mService;
    private BluetoothDevice device;

    //Interfaz
    public SearchBox search;
    private FloatingActionsMenu fab;
    private com.getbase.floatingactionbutton.FloatingActionButton fab_yt;
    private com.getbase.floatingactionbutton.FloatingActionButton fab_local;


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
                    Toast.makeText(getApplicationContext(), "Enviado", Toast.LENGTH_SHORT).show();
                    break;
                case Constants.MESSAGE_VIDEO_READ:
                    byte[] readBuf = (byte[]) msg.obj;
                    Toast.makeText(getApplicationContext(), "Cancion Añadida", Toast.LENGTH_LONG).show();
                    // construct a string from the valid bytes in the buffer
                    String readMessage = new String(readBuf, 0, msg.arg1);
                    adapter.add(new ShpVideo(readMessage));
                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    Toast.makeText(getApplicationContext(), "Conectado", Toast.LENGTH_SHORT).show();
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

        search = (SearchBox) findViewById(R.id.searchbox);
        fab = (FloatingActionsMenu) findViewById(R.id.menu_fab);
        fab_yt = (com.getbase.floatingactionbutton.FloatingActionButton) findViewById(R.id.action_yt);
        fab_local = (com.getbase.floatingactionbutton.FloatingActionButton) findViewById(R.id.action_fs);
        fab_yt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showSearchbox();
            }
        });

        fab_local.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Definimos el archivo que se va  enviar a traves de su path de almacenamiento
                File song = new File("/storage/emulated/0/Music/Ready To Die [1994]/02 Things Done Changed.mp3");
                //Declaramos un array de bytes el cual tiene el tamaño del archivo mas cuatro bytes para el tamaño del mismo
                byte[] songArray = new byte[(int) song.length()+4];
                //Guardamos y casteamos el tamaño del archivo al array de bytes
                int tam = (int) song.length();
                songArray[0] = (byte) (tam >> 24);
                songArray[1] = (byte) (tam >> 16);
                songArray[2] = (byte) (tam >> 8);
                songArray[3] = (byte) (tam);
                try {
                    //Escribimos sobre el array de bytes los datos del fichero a traves de un inputStream
                    FileInputStream fis = new FileInputStream(song);
                    fis.read(songArray, 4, (int)song.length());
                    //En la segunda lectura buscamos los ultimos 128 bytes en los cuales estan los metadatos
                    // en los archivos MP3.
                    fis.skip(song.length()-128);
                    byte[] last128 = new byte[128];
                    //Leemos los 128 ultimos
                    fis.read(last128);
                    String metaData = new String(last128);
                    //Creamos el substring con el nombre de la canción
                    String name = metaData.substring(3, 32);
                    Toast.makeText(getApplicationContext(), name, Toast.LENGTH_LONG).show();
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                //Finalmente si tenemos conexion enviamos el archivo a traves del servicio
                /**if (mService.getState() != BTSharedPlayService.STATE_CONNECTED_AND_LISTEN) {
                    Toast.makeText(getApplicationContext(), "No se puede enviar sin conexion", Toast.LENGTH_LONG).show();

                } else {
                    mService.write(songArray);
                }*/
            }

        });

        //Colocamos los escuchadores de la barra de busqueda de youtube
        setSearchBoxListeners();

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

        //Ponemos el servicio en funcionamiento
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


    private void sendVideo(String msg) {
        //Comprobamos que estamos conectados antes de enviar
        if (mService.getState() != BTSharedPlayService.STATE_CONNECTED_AND_LISTEN) {
            Toast.makeText(getApplicationContext(), "No es posible enviar sin una conexion", Toast.LENGTH_LONG).show();
            return;
        }
        //Comprobamos que hay algo en el mensaje para enviar
        if (msg.length() > 0) {
            //Creamos un array de bytes para enviar el cual tiene 4 bytes mas para el tamaño
            // que en el caso de los videos es 0.


            byte[] message = msg.getBytes();
            byte[] video = new byte[msg.length()+4];
            video[0] = (byte) (0 >> 24);
            video[1] = (byte) (0 >> 16);
            video[2] = (byte) (0 >> 8);
            video[3] = (byte) (0 /*>> 0*/);

            //Copiamos los datos del ID del video al array y lo enviamos
            for (int i = 0; i < message.length; i++) {
                video[i+4] = message[i];
            }
            mService.write(video);
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

    //Metodos para la barra de busqueda de YT
    private void showSearchbox () {
        search.revealFromMenuItem(R.id.action_yt, this);
    }

    private void setSearchBoxListeners() {

        search.setSearchListener(new SearchBox.SearchListener() {

            @Override
            public void onSearchOpened() {
                fab.collapse();
            }

            @Override
            public void onSearchClosed() {
                closeSearch();
            }

            @Override
            public void onSearchTermChanged(final String term) {
            }

            @Override
            public void onSearch(final String searchTerm) {
                SearchResult result = new SearchResult(searchTerm);
                search.addSearchable(result);
                startYoutubeResultsActivity(searchTerm);
            }

            @Override
            public void onResultClick(SearchResult result) {
                //React to result being clicked
                Log.d("ytSearch", "Result");
            }

            @Override
            public void onSearchCleared() {

            }

        });
    }

    private void startYoutubeResultsActivity(final String searchTerm) {
        Intent intent = new Intent(this, YoutubeResultsActivity.class);
        intent.putExtra("term" ,searchTerm);

        startActivityForResult(intent, SELECT_VIDEO);
    }

    protected void closeSearch() {
        search.hideCircularly(this);
    }

    /**
     * Metodo que gestiona los resultados devueltos por los intent que lanza la actividad.
     *
     * @param requestCode Código de la petición
     * @param resultCode  Código del resultado de la petición
     * @param data        Datos devueltos por la petición.
     */
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            case REQUEST_CONNECT_DEVICE_INSECURE:
                // Cuando vuelve con el dispositivo al que desea conectarse
                if (resultCode == Activity.RESULT_OK) {
                    connectDevice(data);
                }
                break;
            case SELECT_VIDEO:
                // Cuando la peticion de seleccionar un video de la lista vuelve
                if (resultCode == Activity.RESULT_OK) {
                    String msg = data.getStringExtra("videoID");
                    sendVideo(msg);
                } else if (resultCode == Activity.RESULT_CANCELED) {
                    Toast.makeText(getApplicationContext(), getString(R.string.serviceErrorYt),
                            Toast.LENGTH_SHORT).show();
                } else {
                    // Ha ocurrido un error con el video
                    Toast.makeText(getApplicationContext(), "El video seleccionado no esta disponible",
                            Toast.LENGTH_LONG).show();
                }
                break;
        }
    }

}


