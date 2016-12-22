package es.uva.mangostas.sharedplaylist;

import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import java.io.File;

import es.uva.mangostas.sharedplaylist.Features.Help;
import es.uva.mangostas.sharedplaylist.Features.PreferencesActivity;

import static android.Manifest.permission;

/**
 * Actividad que implementa el menú principal de la aplicación y
 * redirige a el resto de funcionalidades.
 */
public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    Button serverButton;
    Button clientButton;
    Button preferencesButton;

    private final int REQUEST_ENABLE_BT = 3;
    private BluetoothAdapter btAdapter;
    Button helpButton;
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        serverButton = (Button) findViewById(R.id.buttonServer);
        serverButton.setOnClickListener(this);

        clientButton = (Button) findViewById(R.id.buttonClient);
        clientButton.setOnClickListener(this);

        preferencesButton = (Button) findViewById(R.id.buttonPreferences);
        preferencesButton.setOnClickListener(this);

        helpButton = (Button) findViewById(R.id.buttonHelp);
        helpButton.setOnClickListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (ContextCompat.checkSelfPermission(this,
                permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    permission.READ_EXTERNAL_STORAGE)) {
            } else {
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
                                            appState.delete();
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
                                            //dialog.cancel
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

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

}
