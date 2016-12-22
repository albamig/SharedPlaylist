package es.uva.mangostas.sharedplaylist;


import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;

import es.uva.mangostas.sharedplaylist.BluetoothService.BTSharedPlayService;
import es.uva.mangostas.sharedplaylist.BluetoothService.Constants;
import es.uva.mangostas.sharedplaylist.Features.TrackListAdapter;
import es.uva.mangostas.sharedplaylist.Model.ShpMediaObject;
import es.uva.mangostas.sharedplaylist.Model.ShpSong;
import es.uva.mangostas.sharedplaylist.Model.ShpVideo;


public class ServerActivity extends AppCompatActivity implements YouTubePlayer.OnInitializedListener,
        YouTubePlayer.PlayerStateChangeListener, MediaController.MediaPlayerControl {

    //Codigos de los Intent
    private static final int REQUEST_ENABLE_BT = 3;

    private ListView listView;
    private Toolbar toolbar;
    //Nombre del dispositivo conectado
    String mConnectedDevice = null;

    //Adaptador para BT
    private BluetoothAdapter btAdapter = null;

    //Servicio de envio de texto
    private BTSharedPlayService mSendService = null;
    private static final String TYPE = "Server";

    private MediaPlayer myMediaPlayer;
    private Handler handler;
    private MediaController myMediaController;
    private YouTubePlayer yTPlayer;
    private ArrayList<ShpMediaObject> playList;
    private TrackListAdapter tladapter;
    private YouTubePlayerFragment youTubePlayerFragmen;
    private Boolean isIni = false;

    private int currentTime = 0;
    private static int unknownCount = 1;

    //Preferencias
    private boolean verificarItems;
    private boolean reproduccionCiclica;

    //Manejador para devolver información al servicio
    private final Handler mHandler = new Handler() {

        private AlertDialog.Builder builder;
        AlertDialog alert;
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
                    Toast.makeText(getApplicationContext(), "ELEMENTO ENVIADO", Toast.LENGTH_LONG).show();
                    break;
                case Constants.MESSAGE_VIDEO_READ:
                    byte[] videoBuf = (byte[]) msg.obj;
                    final String readMessage = new String(videoBuf, 0, msg.arg1);
                    //Extraemos el nombre del video
                    final String videoName = readMessage.substring(0,29);
                    //Extraemos el canal del video
                    final String videoChannel = readMessage.substring(30, 59);

                    if(verificarItems) {
                        builder = new AlertDialog.Builder(ServerActivity.this);
                        builder.setMessage("Desea añadir " + videoName)
                                .setTitle("Video recibido")
                                .setCancelable(false)
                                .setNegativeButton("No",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.dismiss();
                                            }
                                        })
                                .setPositiveButton("Sí",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.dismiss();
                                                //Lo añadimos a la lista
                                                tladapter.add(new ShpVideo(readMessage.substring(60), videoName, videoChannel));
                                                if (tladapter.getCount() == 1) {
                                                    nextSong();
                                                }
                                                Toast.makeText(getApplicationContext(), R.string.videoadded, Toast.LENGTH_LONG).show();
                                            }
                                        });
                        alert = builder.create();
                        alert.show();
                    } else {
                        //Lo añadimos a la lista
                        tladapter.add(new ShpVideo(readMessage.substring(60), videoName, videoChannel));
                        if (tladapter.getCount() == 1) {
                            nextSong();
                        }
                        Toast.makeText(getApplicationContext(), R.string.videoadded, Toast.LENGTH_LONG).show();
                    }
                    break;

                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDevice = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != getApplicationContext()) {
                        Toast.makeText(getApplicationContext(), R.string.conectedto
                                + mConnectedDevice, Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_TOAST:
                    if (null != getApplicationContext()) {
                        Toast.makeText(getApplicationContext(), msg.getData().getString(Constants.TOAST),
                                Toast.LENGTH_SHORT).show();
                    }
                    break;
                case Constants.MESSAGE_SONG_READ:
                    ShpSong newSong = null;
                    byte[] songBuf = (byte[]) msg.obj;
                    File song = new File(getFilesDir(), "cancion");
                    if (song.exists()) {
                        song.delete();
                    }
                    try {
                        FileOutputStream fos = new FileOutputStream(song);
                        fos.write(songBuf);
                        fos.close();

                        newSong = new ShpSong(songBuf, "");
                        File finalSong = new File(getFilesDir(), newSong.getTitle());
                        song.renameTo(finalSong);
                        newSong.setPath(finalSong.getPath());


                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    if(verificarItems) {
                        builder = new AlertDialog.Builder(ServerActivity.this);
                        final ShpSong finalNewSong = newSong;
                        builder.setMessage("Desea añadir " + finalNewSong.getTitle() + " de " + finalNewSong.getArtist())
                                .setTitle("Cancion recibida")
                                .setCancelable(false)
                                .setNegativeButton("No",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.dismiss();
                                            }
                                        })
                                .setPositiveButton("Sí",
                                        new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int id) {
                                                dialog.dismiss();
                                                //Lo añadimos a la lista
                                                tladapter.add(finalNewSong);
                                                if (tladapter.getCount() == 1) {
                                                    nextSong();
                                                }
                                                Toast.makeText(getApplicationContext(), R.string.songaddedlist, Toast.LENGTH_LONG).show();

                                            }
                                        });
                        alert = builder.create();
                        alert.show();
                    } else {
                        //Lo añadimos a la lista
                        tladapter.add(newSong);
                        if (tladapter.getCount() == 1) {
                            nextSong();
                        }
                        Toast.makeText(getApplicationContext(), R.string.songaddedlist, Toast.LENGTH_LONG).show();
                    }


            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        //Obtenemos los valores de las preferencias
        verificarItems = PreferenceManager
                .getDefaultSharedPreferences(this.getApplicationContext())
                .getBoolean("check_items", false);

        reproduccionCiclica = PreferenceManager
                .getDefaultSharedPreferences(this.getApplicationContext())
                .getBoolean("cyclRep", false);

        setContentView(R.layout.server);
        //Obtener el adaptador bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        //Si es null, el dispositivo no soporta el bluetooth
        if(btAdapter == null) {
            Toast.makeText(getApplicationContext(), R.string.bluetoothnotsuported, Toast.LENGTH_LONG).show();
            finish();
        }

        toolbar = (Toolbar) findViewById(R.id.appBarLayout);
        this.setSupportActionBar(toolbar);


        // Get ListView object from xml
        listView = (ListView) findViewById(R.id.listView);
        // Defined Array playList to show in ListView
        playList = new ArrayList<>();
        //Assign adapter to ListView
        tladapter = new TrackListAdapter(playList, this);
        listView.setAdapter(tladapter);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ServerActivity.this);
                builder.setMessage(R.string.deletelistelement)
                        .setTitle(R.string.deleteelement)
                        .setCancelable(false)
                        .setNegativeButton(R.string.no,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                })
                        .setPositiveButton(R.string.yes,
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        if (tladapter.getItem(position) instanceof ShpSong) {
                                            File song = new File(((ShpSong) tladapter.getItem(position)).getPath());
                                            song.delete();
                                        }
                                        tladapter.remove(position);
                                        if(position==0){
                                            nextSong();
                                        }
                                        dialog.cancel();
                                    }
                                });
                AlertDialog alert = builder.create();
                alert.show();
                return true;
            }
        });

        //Inicializamos el fragmento
        youTubePlayerFragmen = (YouTubePlayerFragment) getFragmentManager().findFragmentById(R.id.youtubeFragment);

    }

    @Override
    protected void onResume(){
        super.onResume();

        File appState = new File(getApplicationContext().getCacheDir(),"appState");
        if(!appState.exists()) {
            //Inicializamos el reproductor de Youtube (SOLO SI SE EMPIEZA CON VIDEOS EN LA LISTA)
            //tladapter.add(new ShpVideo("OBXRJgSd-aU","mojoy", "oyo"));
            //tladapter.add(new ShpVideo("0rEVwwB3Iw0", "topo", "el topor"));
            //tladapter.add(new ShpSong("/storage/emulated/0/Music/C. Tangana - 10_15 (2015)/1 C.H.I.T.O..mp3","espinaca","caranchoa"));
            //tladapter.add(new ShpSong("/storage/emulated/0/Music/Black Sabbath - Paranoid.mp3","Paranoid","Black Sabbath"));

            //Añadimos elementos a la lista de manera estática
            tladapter.add(new ShpVideo("OBXRJgSd-aU", "Rasputin","Boney M"));
            tladapter.add(new ShpVideo("cytK7Nl0U60", "Es Épico", "Un mono"));
        }

        //Ponemos en marcha el servicio
        setupService();
        //Cargamos el estado anterior si es que existe
        loadState();
        //Prep the media player
        prepMediaPlayer();
        //Comenzamos a reproducir
        nextSong();
        

    }

    @Override
    protected void onPause(){
        super.onPause();
        myMediaController.hide();
        saveState();
        myMediaPlayer.release();
    }

    protected void onStart() {
        super.onStart();
        //Si el Bluetooth no esta activado, lanzamos un intent para activarlo
        if(!btAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, REQUEST_ENABLE_BT);
        }else if (mSendService == null) {
            setupService();
        }

    }

    /**
     * Metodo que inicializa el servicio de envio y recepción.
     */
    private void setupService() {
        //Inicializamos el servicio de Envio.
        mSendService = new BTSharedPlayService(getApplicationContext(), mHandler, "Server");
        mSendService.start();

    }

    /**
     * Guardamos el estado de la aplicacion en la cache.
     */
    private void saveState(){

        if(tladapter.isEmpty()){
            return;
        }
        
        //Apuntamos al fichero en el que vamos a guardar el estado
        File appState = new File(getApplicationContext().getCacheDir(),"appState");
        try {
            FileWriter fw = new FileWriter(appState);
            PrintWriter pw = new PrintWriter(fw);
            //Anotamos en que punto de la reproduccion se encuentra la cancion en este momento.
            if(tladapter.getItem(0) instanceof ShpVideo){
                if (isIni) {
                    pw.print(((yTPlayer.getCurrentTimeMillis() == -1) ? 0 : yTPlayer.getCurrentTimeMillis()) + "\n");
                } else  {
                    pw.print(0 + "\n");
                }
            } else {
                pw.print(myMediaPlayer.getCurrentPosition()+"\n");
            }

            //recorremos la lista de reproduccion guardando el codigo de las canciones en la cache.
            while(!tladapter.isEmpty()){

                if(tladapter.getItem(0) instanceof ShpVideo){
                    ShpVideo video = ((ShpVideo) tladapter.getItem(0));
                    pw.print("V"+video.getYtCode()+'\n'+video.getTitle()+'\n'+video.getArtist()+'\n');


                    tladapter.remove(0);
                } else {
                    ShpSong song = ((ShpSong) tladapter.getItem(0));
                    pw.print(song.getPath()+'\n'+song.getTitle()+'\n'+song.getArtist()+'\n');

                    tladapter.remove(0);
                }
            }
            pw.close();
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

        //Comprobamos que hay un estado guardado en la cache
        if(appState.exists()) {


            try {
                FileReader fr = new FileReader(appState);
                BufferedReader br = new BufferedReader(fr);
                //recuperamos el tiempo por el que se llegaba la priemra cancion.
                currentTime = Integer.parseInt(br.readLine());
                //recuperamos la lista de reproduccion completa

                String data, title, artist;
                while ((data = br.readLine()) != null) {
                    title = br.readLine();
                    artist = br.readLine();

                    if (data.substring(0, 1).equals("V")) {
                        tladapter.add(new ShpVideo(data.substring(1), title, artist));
                    } else {
                        tladapter.add(new ShpSong(data, title, artist));
                    }
                }
                br.close();
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
        if(!tladapter.isEmpty()) {
            //Si el elemento es de tipo video lanzamos el youtube
            if(tladapter.getItem(0) instanceof ShpVideo) {
                if(!isIni) {
                    getFragmentManager().beginTransaction().show(youTubePlayerFragmen).commit();
                    youTubePlayerFragmen.initialize(Constants.APIKEY, this);
                    isIni = true;
                } else {
                    yTPlayer.loadVideo(((ShpVideo) tladapter.getItem(0)).getYtCode(),currentTime);
                }
                //Si no es un video lanzamos el reproductor propio y liberamos los recursoso del yt
            } else {
                getFragmentManager().beginTransaction().hide(youTubePlayerFragmen).commit();

                if(isIni) {
                    yTPlayer.release();
                    isIni = false;
                }
                ShpSong song;
                song = (ShpSong)tladapter.getItem(0);
                try{
                    myMediaPlayer.setDataSource(song.getPath());
                    myMediaPlayer.prepare();
                    myMediaPlayer.seekTo(currentTime);
                    myMediaPlayer.start();

                    myMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            myMediaPlayer.reset();
                            myMediaController.hide();
                            currentTime = 0;

                            if(reproduccionCiclica){
                                tladapter.add(tladapter.getItem(0));
                            }
                            //Si no hay reproducción ciclica eliminamos el archivo de la memoria
                            File song = new File(((ShpSong) tladapter.getItem(0)).getPath());
                            song.delete();
                            tladapter.remove(0);
                            nextSong();

                        }
                    });
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            getFragmentManager().beginTransaction().hide(youTubePlayerFragmen).commit();
            Toast.makeText(getApplicationContext(), R.string.endreproductionlist, Toast.LENGTH_LONG).show();
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
                    }
                });
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_server, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.discoverable: {
                // Hacer el dispositivo visible
                ensureDiscoverable();
                return true;
            }
        }
        return false;
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
            video = (ShpVideo)tladapter.getItem(0);
            yTPlayer.loadVideo(video.getYtCode(),currentTime);
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

        if(reproduccionCiclica){
            tladapter.add(tladapter.getItem(0));
         }
        tladapter.remove(0);
        currentTime = 0;
        nextSong();
    }
    @Override
    public void onError(YouTubePlayer.ErrorReason errorReason) {
        if(!errorReason.equals(YouTubePlayer.ErrorReason.NETWORK_ERROR)) {
          Toast toast = Toast.makeText(getApplicationContext(), R.string.reproductionerror +tladapter.getItem(0).getTitle()+
                  ".\n "+R.string.reproductingnextsong  ,Toast.LENGTH_LONG);
          toast.setGravity(Gravity.TOP,0, 0);
          toast.show();
          tladapter.remove(0);
          nextSong();
        }
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

    @Override
    public boolean onTouchEvent(MotionEvent event){
        if(!isIni) {
            myMediaController.show(0);
        }
        return true;
    }

    public static int getUnknownCount(){
        return unknownCount;
    }

    public static void incrementUnknownCount(){
        unknownCount++;
    }
}



