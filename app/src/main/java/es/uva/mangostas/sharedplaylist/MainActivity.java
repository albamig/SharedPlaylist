package es.uva.mangostas.sharedplaylist;

import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
    private final int REQUEST_ENABLE_BT = 3;
    private BluetoothAdapter btAdapter;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        setContentView(R.layout.activity_main);

        btAdapter = BluetoothAdapter.getDefaultAdapter();

        Button serverButton = (Button) findViewById(R.id.buttonServer);
        serverButton.setOnClickListener(this);

        Button clientButton = (Button) findViewById(R.id.buttonClient);
        clientButton.setOnClickListener(this);

        Button preferencesButton = (Button) findViewById(R.id.buttonPreferences);
        preferencesButton.setOnClickListener(this);
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
                startActivity(new Intent(this, ServerActivity.class));
                break;
            case R.id.buttonClient :
                startActivity(new Intent(this, ClientActivity.class));
                break;
            case R.id.buttonPreferences:
                startActivity(new Intent(this, PreferencesActivity.class));
                break;
        }

    }
}
