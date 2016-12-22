package es.uva.mangostas.sharedplaylist;

/**
 * @author Alberto Amigo Alonso
 * @author Sergio Delgado Álvarez
 * @author Óscar Fernández Angulo
 * @author Santos Ángel Prado
 */

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;

import es.uva.mangostas.sharedplaylist.features.Help;
import es.uva.mangostas.sharedplaylist.features.PreferencesActivity;

import static android.Manifest.permission;


/**
 * @author Alberto Amigo Alonso
 * @author Sergio Delgado Álvarez
 * @author Óscar Fernández Angulo
 * @author Santos Ángel Prado
 */

/**
 * Actividad que implementa el menú principal de la aplicación y
 * redirige a el resto de funcionalidades.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {

    private final int REQUEST_ENABLE_BT = 3;
    private BluetoothAdapter btAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        Button serverButton = (Button) findViewById(R.id.buttonServer);
        serverButton.setOnClickListener(this);

        Button clientButton = (Button) findViewById(R.id.buttonClient);
        clientButton.setOnClickListener(this);

        Button preferencesButton = (Button) findViewById(R.id.buttonPreferences);
        preferencesButton.setOnClickListener(this);

        Button helpButton = (Button) findViewById(R.id.buttonHelp);
        helpButton.setOnClickListener(this);
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this,
                permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (!ActivityCompat.shouldShowRequestPermissionRationale(this,
                    permission.READ_EXTERNAL_STORAGE)) {
                ActivityCompat.requestPermissions(this,
                        new String[]{permission.READ_EXTERNAL_STORAGE},
                        1);
            }
        }

        if (!btAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, REQUEST_ENABLE_BT);
        }
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.buttonServer :
                final File appState = new File(getApplicationContext().getCacheDir(),"appState");
                final Intent intentServidor = new Intent(this,ServerActivity.class);

                if(appState.exists()){
                    AlertDialog.Builder builder = new AlertDialog.Builder(this);
                    builder.setMessage(getString(R.string.createlist))
                            .setTitle(getString(R.string.copyfound))
                            .setCancelable(true)
                            .setNegativeButton(getString(R.string.neww),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            if (!appState.delete()) {
                                                Toast.makeText(getApplicationContext(), "No se pudo" +
                                                        "borrar el fichero de la lista, podrá resta" +
                                                        "urarlo la proxima vez", Toast.LENGTH_LONG)
                                                        .show();
                                            }
                                            if (!btAdapter.isEnabled()) {
                                                Toast.makeText(getApplicationContext(),
                                                        "Esta caracteristica requiere " +
                                                                "activar el bluetooth",
                                                        Toast.LENGTH_LONG).show();
                                                Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                                                startActivityForResult(enableBT, REQUEST_ENABLE_BT);
                                            } else {
                                                startActivity(intentServidor);
                                            }
                                        }
                                    })
                            .setPositiveButton(getString(R.string.restore),
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            startActivity(intentServidor);
                                        }
                                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                } else {
                    if (!btAdapter.isEnabled()) {
                        Toast.makeText(getApplicationContext(),
                                getString(R.string.reqblue),
                                Toast.LENGTH_LONG).show();
                        Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                        startActivityForResult(enableBT, REQUEST_ENABLE_BT);
                    } else {
                        startActivity(intentServidor);
                    }
                }

                break;

            case R.id.buttonClient :
                if (!btAdapter.isEnabled()) {
                    Toast.makeText(getApplicationContext(),
                            getString(R.string.reqblue),
                            Toast.LENGTH_LONG).show();
                    Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                    startActivityForResult(enableBT, REQUEST_ENABLE_BT);
                } else {
                    startActivity(new Intent(this, ClientActivity.class));
                }
                break;

            case R.id.buttonPreferences:
                startActivity(new Intent(this, PreferencesActivity.class));
                break;

            case R.id.buttonHelp:
                startActivity(new Intent(this, Help.class));
                break;
        }

    }
}
