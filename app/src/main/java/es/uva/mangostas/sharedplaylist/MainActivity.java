package es.uva.mangostas.sharedplaylist;

import android.app.DialogFragment;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;

import java.util.ArrayList;

import es.uva.mangostas.sharedplaylist.Model.ShpMediaObject;
import es.uva.mangostas.sharedplaylist.Model.ShpVideo;

public class MainActivity extends AppCompatActivity implements  NewFruitDialogFragment.NewFruitDialogListner, YouTubePlayer.OnInitializedListener, YouTubePlayer.PlayerStateChangeListener {
    ListView listView;
    ArrayAdapter<ShpMediaObject> adapter;
    private YouTubePlayer youTubePlayer;
    private ArrayList<ShpMediaObject> playList;
    private YouTubePlayerFragment youTubePlayerFragmen;
    private ShpPlayer shpPlayerFragment;
    private String APIKEY = "AIzaSyASYbIO42ecBEzgB5kiPpu2OHJV8_5ulnk"; //SERGIO DIJO ALGO DEL MANIFIESTO (IGUAL HAY QUE MOVER ESTO)

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Get ListView object from xml
        listView = (ListView) findViewById(R.id.listView);

        // Defined Array playList to show in ListView
        playList = new ArrayList<ShpMediaObject>();

        ;


        // Define a new Adapter
        // First parameter - Context
        // Second parameter - Layout for the row
        // Third parameter - ID of the TextView to which the data is written
        // Forth - the Array of data

        adapter = new ArrayAdapter<ShpMediaObject>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, playList);


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

        // Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        //setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showNewFruitDialog();
            }
        });

        //Inicializamos el fragmento
        youTubePlayerFragmen = (YouTubePlayerFragment) getFragmentManager().findFragmentById(R.id.youtubeFragment);
        //shpPlayerFragment = getFragmentManager().findFragmentById(R.id.youtubeFragment);

        //Lo escondemos hasta que le llegue algo que reproducir
        getFragmentManager().beginTransaction().hide(youTubePlayerFragmen).commit();
        //getFragmentManager().beginTransaction().hide(shpPlayerFragment).commit();

        //Inicializamos el reproductor de Youtube (SOLO SI SE EMPIEZA CON VIDEOS EN LA LISTA)
        //youTubePlayerFragmen.initialize("AIzaSyASYbIO42ecBEzgB5kiPpu2OHJV8_5ulnk", this);
        adapter.add(new ShpVideo("OBXRJgSd-aU"));
        adapter.add(new ShpVideo("0rEVwwB3Iw0"));

        nextVideo();


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //Metodos de la interfaz del dialogo.

    public void showNewFruitDialog() {
        DialogFragment dialog = new NewFruitDialogFragment();
        dialog.show(getFragmentManager(), "Fruta");
    }

    //Si quedan videos en la playList reproduce el primero
    public void nextVideo(){

        //comprobamos que hay videos en la lista
        if(!adapter.isEmpty()) {
            //Comprobamos si el objeto es un video de YT
            if(adapter.getItem(0) instanceof ShpVideo) {
                //Guardamos el objeto y lo eliminamos de la lista
                ShpVideo video = (ShpVideo)adapter.getItem(0);
                adapter.remove(video);
                getFragmentManager().beginTransaction().show(youTubePlayerFragmen).commit();
                youTubePlayerFragmen.initialize(APIKEY, this);
                Log.d("LLEGA:" , "Despues de Inicialize");
                youTubePlayer.loadVideo(video.getYtCode());
            } else {
                //Llamar al de SANTOS
            }
        } else {
            //No quedan videos por reproducir!!
            youTubePlayer.release();
            //Escondemos el fragmento
            getFragmentManager().beginTransaction().hide(youTubePlayerFragmen).commit();
            getFragmentManager().beginTransaction().hide(shpPlayerFragment).commit();


        }
    }

    /**
     * Metodo para añadir elementos a la lista
     * @param dialog
     */
    @Override
    public void onDialogPositiveClick(DialogFragment dialog) {

        //CANCIONES DE PRUEBA

        /**EditText edit = (EditText) dialog.getDialog().findViewById(R.id.fruit);
        String text = edit.getText().toString();
        //Si el adaptador esta vacio habria que iniciar el reproductor
        if(adapter.isEmpty()){
            adapter.add(text);
            getFragmentManager().beginTransaction().show(youTubePlayerFragmen).commit();
            youTubePlayerFragmen.initialize(APIKEY, this);
        } else {
            adapter.add(text);
        }*/


    }

    @Override
    public void onDialogNegativeClick(DialogFragment dialog) {
        dialog.getDialog().cancel();
    }

    //se llama a este metodo al inicializar el reproductor con exito
    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
        if (!b) {
            this.youTubePlayer=youTubePlayer;
            //le ponemos un controlador de cambios de estado al reproductor
            youTubePlayer.setPlayerStateChangeListener(this);
            //reproducimos el primer video
        }
    }

    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {

    }


    @Override
    public void onLoading() {

    }

    @Override
    public void onLoaded(String s) {

    }

    @Override
    public void onAdStarted() {

    }

    @Override
    public void onVideoStarted() {

    }

    @Override
    public void onVideoEnded() {
        //cuando se acaba un video reproducimos el siguiente
        nextVideo();
    }

    @Override
    public void onError(YouTubePlayer.ErrorReason errorReason) {

    }
}
