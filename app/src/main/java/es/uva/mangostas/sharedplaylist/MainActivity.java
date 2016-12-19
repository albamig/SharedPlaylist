package es.uva.mangostas.sharedplaylist;


import android.app.AlertDialog;
import android.content.DialogInterface;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;


import java.io.File;

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
    }

    @Override
    protected void onStart() {
        super.onStart();
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
                    builder.setMessage("¿Desea recuperar la lista o crear una nueva?")
                            .setTitle("Copia de seguridad encontrada")
                            .setCancelable(false)
                            .setNegativeButton("Nueva",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            appState.delete();
                                            startActivity(intentServidor);
                                            //dialog.cancel
                                        }
                                    })
                            .setPositiveButton("Recuperar",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            startActivity(intentServidor);
                                        }
                                    });
                    AlertDialog alert = builder.create();
                    alert.show();
                } else {
                    startActivity(intentServidor);
                }



                break;

            case R.id.buttonClient :
                startActivity(new Intent(this, ClientActivity.class));
                break;
        }

    }



}
