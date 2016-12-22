package es.uva.mangostas.sharedplaylist.BluetoothService;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Set;

import es.uva.mangostas.sharedplaylist.R;

/**
 * Created by root on 17/11/16.
 */

/**
 * Actividad que se encarga de gestionar la busqueda y selección
 * de los dispositivos bluetooth que estan disponibles.
 */
public class DeviceListActivity extends Activity {

    //Información extra para el intent

    public static String EXTRA_DEVICE_ADDRESS = "device_address";

    //Adaptador del Bluetooth
    BluetoothAdapter btAdapter;

    //ArrayAdapter para los dispositivos descubiertos
    private ArrayAdapter<String> mNewDevicesAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //Colocarlo como un modal y no como una nueva venatna
        setContentView(R.layout.activity_device_list);

        setResult(Activity.RESULT_CANCELED);

        //Boton para escanear
        Button scanButton = (Button) findViewById(R.id.button_scan);
        scanButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startDiscovery();
                v.setVisibility(View.GONE);
            }
        });

        //Inicializar el Adaptador para los dispositivos encontrados
        // y uno mas para los dispositivos pareados
        ArrayAdapter<String> pairedDevicesAdapter = new ArrayAdapter<String>(this, R.layout.device_name);
        mNewDevicesAdapter = new ArrayAdapter<String>(this, R.layout.device_name);

        //Buscamos e inicializamos la lista con los dispositivos pareados
        ListView pairedListView = (ListView) findViewById(R.id.paired_devices);
        pairedListView.setAdapter(pairedDevicesAdapter);
        pairedListView.setOnItemClickListener(mDeviceClickListener);

        //Buscar e inicializar los dispositivos escaneados
        ListView newDevicesListView = (ListView) findViewById(R.id.new_devices);
        newDevicesListView.setAdapter(mNewDevicesAdapter);
        newDevicesListView.setOnItemClickListener(mDeviceClickListener);

        //Registrar para el broadcast las diferentes situaciones
        IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
        this.registerReceiver(mReciver, filter);
        filter = new IntentFilter(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        this.registerReceiver(mReciver, filter);

        //Obtener el adaptador de bluetooth y la lista de dispositivos vinculados
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        Set<BluetoothDevice> pairedDevices = btAdapter.getBondedDevices();

        //Añadirlos al ArrayAdapter de los dispositivos vinculados
        if (pairedDevices.size() > 0) {
            findViewById(R.id.title_paired_devices).setVisibility(View.VISIBLE);
            for (BluetoothDevice device : pairedDevices) {
                pairedDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
            }
        }else {
            String noDevices = String.valueOf(R.string.notvinculateddispositives);
            pairedDevicesAdapter.add(noDevices);
        }



    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        //Cancelamos el escaneo
        if(btAdapter != null) {
            btAdapter.cancelDiscovery();
        }

        //Eliminamos el registro del broadcast
        this.unregisterReceiver(mReciver);
    }

    /**
     * Metodo con el cual encendemos el escaneo de dispositivos.
     */
    private void startDiscovery() {

        //Marcamos el sub titulo para los nuevos dispositivos
        findViewById(R.id.title_new_devices).setVisibility(View.VISIBLE);

        //Si estamos escaneando lo detenemos y despues comenzamos
        if(btAdapter.isDiscovering()) {
            btAdapter.cancelDiscovery();
        }
        btAdapter.startDiscovery();
    }

    /**
     * Metodo para tratar el funcionamiento cuando el usuario pulsa sobre un item
     * de las listas de dispositivos
     */

    private AdapterView.OnItemClickListener mDeviceClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
            //Paramos de escanear
            btAdapter.cancelDiscovery();

            //Obtenemos la direccion MAC del dispositivo que ha quedado en los
            // ultimos 17 caracteres del string
            String info = ((TextView) view).getText().toString();
            String address = info.substring(info.length()-17);
            Toast.makeText(getApplicationContext(), R.string.iamconectedto + address, Toast.LENGTH_LONG).show();
            //Lanzamos ahora un Intent pàra conectarnos a ese dispositivo
            Intent intent = new Intent();
            intent.putExtra(EXTRA_DEVICE_ADDRESS, address);

            // Set result and finish this Activity
            setResult(Activity.RESULT_OK, intent);

            finish();
        }
    };

    /**
     * El broadcastReciever para escuchar eventos a la hora de
     * realizar el descubrimiento de dispositivos.
     */
    private final BroadcastReceiver mReciver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            //Cuando encuentra un dispositivo
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //Si esta vinculado no lo añadimos
                if(device.getBondState() != BluetoothDevice.BOND_BONDED) {
                    mNewDevicesAdapter.add(device.getName() + "\n" + device.getAddress());
                }
            } else if (BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if(mNewDevicesAdapter.getCount() == 0) {
                    String noDevices = String.valueOf(R.string.notfound);
                    mNewDevicesAdapter.add(noDevices);
                }
            }
        }
    };
}
