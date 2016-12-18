package es.uva.mangostas.sharedplaylist;

import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import es.uva.mangostas.sharedplaylist.Model.ShpMediaObject;
import es.uva.mangostas.sharedplaylist.Model.ShpSong;
import es.uva.mangostas.sharedplaylist.Model.ShpVideo;

public class ServerActivity extends AppCompatActivity implements YouTubePlayer.OnInitializedListener,
        YouTubePlayer.PlayerStateChangeListener, MediaController.MediaPlayerControl {
    private ListView listView;
    private Toolbar toolbar;
    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private MediaPlayer myMediaPlayer;
    private Handler handler;
    private MediaController myMediaController;
    private ArrayAdapter<ShpMediaObject> adapter;
    private YouTubePlayer yTPlayer;
    private ArrayList<ShpMediaObject> playList;
    private YouTubePlayerFragment youTubePlayerFragmen;
    private Boolean isIni = false;
    private String APIKEY = "AIzaSyASYbIO42ecBEzgB5kiPpu2OHJV8_5ulnk";
    private int currentTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.server);

        toolbar = (Toolbar) findViewById(R.id.appBarLayout);
        this.setSupportActionBar(toolbar);


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

        //Inicializamos el fragmento
        youTubePlayerFragmen = (YouTubePlayerFragment) getFragmentManager().findFragmentById(R.id.youtubeFragment);


        File appState = new File(getApplicationContext().getCacheDir(),"appState");
        Log.d("OSCAR","Comprobar si existe: "+appState.exists());
        if(!appState.exists()) {
            Log.d("OSCAR","no existe");
            //Inicializamos el reproductor de Youtube (SOLO SI SE EMPIEZA CON VIDEOS EN LA LISTA)
            adapter.add(new ShpVideo("OBXRJgSd-aU"));
            adapter.add(new ShpVideo("0rEVwwB3Iw0"));
            //adapter.add(new ShpSong("/storage/emulated/0/Music/C. Tangana - 10_15 (2015)/1 C.H.I.T.O..mp3"));
            adapter.add(new ShpSong("/storage/emulated/0/Music/Black Sabbath - Paranoid.mp3"));
            adapter.add(new ShpVideo("0rEVwwB3Iw0"));
        }



    }

    @Override
    protected void onResume(){
        super.onResume();

        loadState();

        //Prep the media player
        prepMediaPlayer();

        nextSong();
    }

    @Override
    protected void onPause(){
        super.onPause();

        saveState();

        myMediaPlayer.release();
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        Log.d("OSCAR","onDestroy: "+ deleteState());
    }

    /**
     * Guardamos el estado de la aplicacion en la cache.
     */
    private void saveState(){
        //Apuntamos al fichero en el que vamos a guardar el estado
        File appState = new File(getApplicationContext().getCacheDir(),"appState");
        Log.d("OSCAR",getApplicationContext().getCacheDir().toString());
        try {
            FileWriter fw = new FileWriter(appState);
            PrintWriter pw = new PrintWriter(fw);
            //Anotamos en que punto de la reproduccion se encuentra la cancion en este momento.
            if(playList.get(0) instanceof ShpVideo){

                pw.print(((yTPlayer.getCurrentTimeMillis() == -1) ? 0 : yTPlayer.getCurrentTimeMillis())+"\n");

                Log.d("OSCAR","save: "+yTPlayer.getCurrentTimeMillis());
            } else {
                pw.print(myMediaPlayer.getCurrentPosition()+"\n");
            }

            //recorremos la lista de reproduccion guardando el codigo de las canciones en la cache.
            while(!adapter.isEmpty()){

                if(adapter.getItem(0) instanceof ShpVideo){
                    pw.print("V"+((ShpVideo) adapter.getItem(0)).getYtCode()+'\n');
                    Log.d("OSCAR","save: "+((ShpVideo) adapter.getItem(0)).getYtCode());
                    adapter.remove(adapter.getItem(0));
                } else {
                    pw.print(((ShpSong) adapter.getItem(0)).getPath()+'\n');
                    adapter.remove(adapter.getItem(0));
                }
            }
            fw.close();
        } catch (IOException e) {
            Log.e("ERROR","error reading file", e);
        }
    }

    /**
     * Cargamos el estado de la aplicacion almacenado en la cache.
     */
    private void loadState(){

        File appState = new File(getApplicationContext().getCacheDir(),"appState");
        Log.d("OSCAR","Comprobar si existe (onResume)"+appState.toString()+": "+appState.exists());
        Log.d("OSCAR",getApplicationContext().getCacheDir().toString());

        //Comprobamos que hay un estado guardado en la cache
        if(appState.exists()) {
            Log.d("OSCAR","existe");


            try {
                FileReader fr = new FileReader(appState);
                Log.d("OSCAR","fr");
                BufferedReader br = new BufferedReader(fr);
                Log.d("OSCAR","br");

                //recuperamos el tiempo por el que se llegaba la priemra cancion.
                String linea;
                Log.d("OSCAR","1");
                linea=br.readLine();
                Log.d("OSCAR","2"+linea);
                currentTime = Integer.parseInt(linea);
                Log.d("OSCAR","current");

                //recuperamos la lista de reproduccion completa

                while ((linea = br.readLine()) != null) {
                    Log.d("OSCAR","load");
                    if (linea.substring(0, 1).equals("V")) {
                        adapter.add(new ShpVideo(linea.substring(1)));
                    } else {
                        adapter.add(new ShpSong(linea));
                    }
                }

                fr.close();
            } catch (FileNotFoundException e) {
                Log.e("ERROR", "file not found", e);
            } catch (IOException e) {
                Log.e("ERROR", "error writing file", e);
            }

            //Borramos el fichero de la cache.
            appState.delete();

        }
    }

    /**
     * Eliminamos de la cache el fichero que almacena el estado de la aplicacion.
     * @return resultado del borrado del fichero.
     */
    private boolean deleteState(){
        File appState = new File(getApplicationContext().getCacheDir(),"appState");
        return appState.delete();
    }

    /**
     * Este metodo llama a los reprouctores depoendiendo del tipo
     * de cancion que hay en la primera posicion de la lista.
     */
    public void nextSong() {
        //Comprobamos que la lista no esta vacia
        if(!adapter.isEmpty()) {

            //Si el elemento es de tipo video lanzamos el youtube
            if(adapter.getItem(0) instanceof ShpVideo) {
                if(!isIni) {
                    getFragmentManager().beginTransaction().show(youTubePlayerFragmen).commit();
                    youTubePlayerFragmen.initialize(APIKEY, this);
                    isIni = true;
                } else {
                    yTPlayer.cueVideo(((ShpVideo) adapter.getItem(0)).getYtCode(),currentTime);
                    yTPlayer.play();
                }
                //Si no es un video lanzamos el reproductor propio y liberamos los recursoso del yt
            } else {
                getFragmentManager().beginTransaction().hide(youTubePlayerFragmen).commit();

                if(isIni) {
                    yTPlayer.release();
                    isIni = false;
                }
                ShpSong song;
                song = (ShpSong)adapter.getItem(0);
                try{
                    myMediaPlayer.setDataSource(song.getPath());
                    myMediaPlayer.prepare();
                    seekTo(currentTime);
                    myMediaPlayer.start();

                    myMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            myMediaPlayer.reset();
                            myMediaController.hide();

                            adapter.remove(adapter.getItem(0));
                            currentTime = 0;

                            nextSong();

                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
                /*Intent intent = new Intent(ServerActivity.this, SongActivity.class);
                startActivity(intent);
                adapter.remove(adapter.getItem(0));*/
            }
        } else {
            getFragmentManager().beginTransaction().hide(youTubePlayerFragmen).commit();
            Toast.makeText(getApplicationContext(), "Fin de la lista de reproducción.", Toast.LENGTH_LONG).show();
        }
    }



    /**
     * Metodo para preparar el media player para reproducir canciones
     */
    private void prepMediaPlayer() {
        myMediaPlayer = new MediaPlayer();
        myMediaController = new MediaController(this);
        myMediaController.setMediaPlayer(this);
        myMediaController.setAnchorView(findViewById(R.id.mediaPlayer));
        handler = new Handler();

        myMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer player) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        myMediaController.setEnabled(true);
                        myMediaController.show(0);
                        myMediaPlayer.start();
                    }
                });
            }
        });

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




    //se llama a este metodo al inicializar el reproductor con exito
    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {

        if (!b) {

            if(youTubePlayer != null) {
                this.yTPlayer=youTubePlayer;
            }

            //le ponemos un controlador de cambios de estado al reproductor
            yTPlayer.setPlayerStateChangeListener(this);

            //reproducimos el primer video
            ShpVideo video;
            video = (ShpVideo) adapter.getItem(0);
            yTPlayer.cueVideo(video.getYtCode(),currentTime);
            yTPlayer.play();
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

    /**
     * Se ejecuta cuando finaliza un video de youTube. Se borra la cancion que se estaba
     * reproduciendo, se pone el tiempo de reproduccion a cero y se inicia la siguiente cancion.
     */
    @Override
    public void onVideoEnded() {
        adapter.remove(adapter.getItem(0));
        currentTime = 0;

        nextSong();
    }
    @Override
    public void onError(YouTubePlayer.ErrorReason errorReason) {

    }


    //Metodos para el control del reproductor local
    @Override
    public void start() {
        myMediaPlayer.start();
    }

    @Override
    public void pause() {
        myMediaPlayer.pause();
    }

    @Override
    public int getDuration() {
        return myMediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return myMediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int pos) {
        myMediaPlayer.seekTo(pos);
    }

    @Override
    public boolean isPlaying() {
        return myMediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        int porcentage = (myMediaPlayer.getCurrentPosition() * 100) / myMediaPlayer.getDuration();
        return porcentage;
    }

    @Override
    public boolean canPause() {
        return true;
    }

    @Override
    public boolean canSeekBackward() {
        return true;
    }

    @Override
    public boolean canSeekForward() {
        return true;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        myMediaController.show(0);
        return true;
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);

        // Checks the orientation of the screen
        if (newConfig.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            if(isIni){
                yTPlayer.setFullscreen(true);
            }

        }/* else if (newConfig.orientation == Configuration.ORIENTATION_PORTRAIT){
                CAMBIOS AL PONER LA PANTALLA VERTICAL
        }*/
    }
}

