package es.uva.mangostas.sharedplaylist;


import android.app.Activity;
import android.app.AlertDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.Configuration;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.DragEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.TextView;
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

    private final String APIKEY = "AIzaSyASYbIO42ecBEzgB5kiPpu2OHJV8_5ulnk";

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
                    break;
                case Constants.MESSAGE_VIDEO_READ:
                    byte[] videoBuf = (byte[]) msg.obj;
                    String readMessage = new String(videoBuf, 0, msg.arg1);
                    //Extraemos el nombre del video
                    String videoName = readMessage.substring(0,29);
                    //Extraemos el canal del video
                    String videoChannel = readMessage.substring(30, 59);
                    //Lo añadimos a la lista
                    playList.add(new ShpVideo(readMessage.substring(60), videoName, videoChannel));
                    tladapter.notifyDataSetChanged();
                    Toast.makeText(getApplicationContext(), "Video añadido a la lista", Toast.LENGTH_LONG).show();
                    // construct a string from the valid bytes in the buffer

                    break;
                case Constants.MESSAGE_DEVICE_NAME:
                    // save the connected device's name
                    mConnectedDevice = msg.getData().getString(Constants.DEVICE_NAME);
                    if (null != getApplicationContext()) {
                        Toast.makeText(getApplicationContext(), "Connected to "
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
                    byte[] songBuf = (byte[]) msg.obj;
                    File song=new File(getFilesDir(), "CHITO.mp3");
                    if (song.exists()) {
                        song.delete();
                    }
                    try {
                        FileOutputStream fos = new FileOutputStream(song);
                        fos.write(songBuf);
                        fos.close();
                        ShpSong newSong = new ShpSong(songBuf, "");
                        File finalSong = new File(getFilesDir(), newSong.getTitle());
                        song.renameTo(finalSong);
                        newSong.setPath(finalSong.getPath());
                        playList.add(newSong);
                        tladapter.notifyDataSetChanged();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Toast.makeText(getApplicationContext(), "Cancion añadida a la lista", Toast.LENGTH_LONG).show();
            }
        }
    };
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.server);
        //Obtener el adaptador bluetooth
        btAdapter = BluetoothAdapter.getDefaultAdapter();

        //Si es null, el dispositivo no soporta el bluetooth
        if(btAdapter == null) {
            Toast.makeText(getApplicationContext(), "El dispositivo no soporta Bluetooth", Toast.LENGTH_LONG).show();
            finish();
        }

        toolbar = (Toolbar) findViewById(R.id.appBarLayout);
        this.setSupportActionBar(toolbar);


        // Get ListView object from xml
        listView = (ListView) findViewById(R.id.listView);
        // Defined Array playList to show in ListView
        playList = new ArrayList<>();
        //Assign adapter to ListView
        tladapter = new TrackListAdapter(playList);
        listView.setAdapter(tladapter);
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                AlertDialog.Builder builder = new AlertDialog.Builder(ServerActivity.this);
                builder.setMessage("¿Desea eliminar este elemento de la lista?")
                        .setTitle("Eliminar elemento")
                        .setCancelable(false)
                        .setNegativeButton("No",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        dialog.cancel();
                                    }
                                })
                        .setPositiveButton("Sí",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int id) {
                                        tladapter.remove(position);
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
            Log.d("OSCAR", "no existe");
            //Inicializamos el reproductor de Youtube (SOLO SI SE EMPIEZA CON VIDEOS EN LA LISTA)
            tladapter.add(new ShpVideo("OBXRJgSd-aU","mojoy", "oyo"));
            tladapter.add(new ShpVideo("0rEVwwB3Iw0", "topo", "el topor"));
            tladapter.add(new ShpSong("/storage/emulated/0/Music/C. Tangana - 10_15 (2015)/1 C.H.I.T.O..mp3","espinaca","caranchoa"));
            //tladapter.add(new ShpSong("/storage/emulated/0/Music/Black Sabbath - Paranoid.mp3","Paranoid","Black Sabbath"));

            //Añadimos elementos a la lista de manera estática
            tladapter.add(new ShpVideo("OBXRJgSd-aU", "Rasputin","Boney M"));
            tladapter.add(new ShpVideo("cytK7Nl0U60", "Es Épico", "Un mono"));
        }

        loadState();
        //Metodo para encender el servicio de Bluetooth
        setupService();
        //Prep the media player
        prepMediaPlayer();
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
            mSendService.start();
        }
    }

    private void setupService() {
        //Inicializamos el servicio de Envio.
        mSendService = new BTSharedPlayService(getApplicationContext(), mHandler, TYPE);
        mSendService.start();

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
            if(tladapter.getItem(0) instanceof ShpVideo){

                pw.print(((yTPlayer.getCurrentTimeMillis() == -1) ? 0 : yTPlayer.getCurrentTimeMillis())+"\n");

                Log.d("OSCAR","save: "+yTPlayer.getCurrentTimeMillis());
            } else {
                pw.print(myMediaPlayer.getCurrentPosition()+"\n");
            }

            //recorremos la lista de reproduccion guardando el codigo de las canciones en la cache.
            while(!tladapter.isEmpty()){

                if(tladapter.getItem(0) instanceof ShpVideo){
                    ShpVideo video = ((ShpVideo) tladapter.getItem(0));
                    pw.print("V"+video.getYtCode()+'\n'+video.getTitle()+'\n'+video.getArtist()+'\n');

                    //Log.d("OSCAR","save: "+((ShpVideo) playList.get(0)).getYtCode());

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


                currentTime = Integer.parseInt(br.readLine());
                Log.d("OSCAR","current");

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
                    youTubePlayerFragmen.initialize(APIKEY, this);
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

    /**
     * Se ejecuta cuando finaliza un video de youTube. Se borra la cancion que se estaba
     * reproduciendo, se pone el tiempo de reproduccion a cero y se inicia la siguiente cancion.
     */
    @Override
    public void onVideoEnded() {

        tladapter.remove(0);
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


    //Clase del adaptador de la lista de reproduccion
    public class TrackListAdapter extends BaseAdapter {
        private LayoutInflater inflater;
        private ArrayList<ShpMediaObject> playList;

        public TrackListAdapter(ArrayList<ShpMediaObject> playList) {
            this.playList = playList;
            inflater= (LayoutInflater) getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
        }
        @Override
        public int getCount() {
            return playList.size();
        }

        @Override
        public Object getItem(int i) {
            return playList.get(i);
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        public boolean isEmpty(){
            return playList.isEmpty();
        }

        public void remove(int i){
            playList.remove(i);
            this.notifyDataSetChanged();
        }

        public void add(ShpMediaObject object){
            playList.add(object);
            this.notifyDataSetChanged();
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = inflater.inflate(R.layout.rows,null);
            TextView songTitle = (TextView)view.findViewById(R.id.textView_Title);
            TextView songArtist = (TextView)view.findViewById(R.id.textView_Artis);
            ImageView songType = (ImageView)view.findViewById(R.id.songType);
            songTitle.setText(playList.get(i).getTitle());
            if (playList.get(i) instanceof ShpVideo) {
                songType.setImageResource(R.drawable.ic_yt);
                songArtist.setText(playList.get(i).getArtist());
            } else {
                songType.setImageResource(R.mipmap.auriculares);
                songArtist.setText(playList.get(i).getArtist());
            }

            return view;
        }
    }
}



