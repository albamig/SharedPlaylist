package es.uva.mangostas.sharedplaylist;


import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.MediaController;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

import es.uva.mangostas.sharedplaylist.BluetoothService.BTSharedPlayService;
import es.uva.mangostas.sharedplaylist.BluetoothService.Constants;
import es.uva.mangostas.sharedplaylist.Model.ShpMediaObject;
import es.uva.mangostas.sharedplaylist.Model.ShpSong;
import es.uva.mangostas.sharedplaylist.Model.ShpVideo;

public class ServerActivity extends AppCompatActivity implements YouTubePlayer.OnInitializedListener, YouTubePlayer.PlayerStateChangeListener, MediaController.MediaPlayerControl {

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
    private ArrayAdapter<ShpMediaObject> adapter;
    private YouTubePlayer yTPlayer;
    private ArrayList<ShpMediaObject> playList;
    private ArrayList<String> songTitles;
    private ArrayList<String> songArtists;
    private TrackListAdapter tladapter;
    private YouTubePlayerFragment youTubePlayerFragmen;
    private Boolean isIni = false;
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
                    //Lo añadimos a la lista
                    songTitles.add(videoName);
                    adapter.add(new ShpVideo(readMessage.substring(30)));
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
                        String name = getTitle(songBuf);
                        File finalSong = new File(getFilesDir(), name);
                        song.renameTo(finalSong);
                        songTitles.add(name);
                        songArtists.add(getArtist(songBuf));
                        adapter.add(new ShpSong(finalSong.getPath()));
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
        songTitles = new ArrayList<>();
        songArtists = new ArrayList<>();


        // Define a new Adapter
        // First parameter - Context
        // Second parameter - Layout for the row
        // Third parameter - ID of the TextView to which the data is written
        // Forth - the Array of data
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, playList);


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


        //Añadimos elementos a la lista de manera estática
        adapter.add(new ShpVideo("OBXRJgSd-aU"));
        songTitles.add("Rasputin");
        adapter.add(new ShpSong("/storage/emulated/0/Music/C. Tangana - 10_15 (2015)/1 C.H.I.T.O..mp3"));
        songTitles.add("C.H.I.T.O");
        songArtists.add("C-Tangana");
        adapter.add(new ShpVideo("cytK7Nl0U60"));
        songTitles.add("Pura droga sin cortar");

        //Metodo para encender el servicio de Bluetooth
        setupService();
        //Comenzamos a reproducir los elementos de la lista
        nextSong();
        //Prep the media player
        prepMediaPlayer();

        // Assign adapter to ListView
        tladapter = new TrackListAdapter();
        listView.setAdapter(tladapter);

    }

    @Override
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
                    yTPlayer.loadVideo(((ShpVideo) adapter.getItem(0)).getYtCode());
                    adapter.remove(adapter.getItem(0));
                }
                //Si no es un video lanzamos el reproductor propio y liberamos los recursoso del yt
            } else {
                getFragmentManager().beginTransaction().hide(youTubePlayerFragmen).commit();
                yTPlayer.release();
                isIni = false;
                ShpSong song;
                song = (ShpSong)adapter.getItem(0);
                try{
                    myMediaPlayer.setDataSource(song.getPath());
                    myMediaPlayer.prepare();
                    myMediaPlayer.start();

                    myMediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                        @Override
                        public void onCompletion(MediaPlayer mp) {
                            myMediaPlayer.reset();
                            myMediaController.hide();
                            adapter.remove(adapter.getItem(0));
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
            video = (ShpVideo)adapter.getItem(0);
            yTPlayer.loadVideo(video.getYtCode());
            adapter.remove(adapter.getItem(0));
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
        //Toast.makeText(getApplicationContext(), "Cambiando de tema...", Toast.LENGTH_LONG).show();
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



    //Clase del adaptador de la lista de reproduccion
    public class TrackListAdapter extends BaseAdapter {
        private LayoutInflater inflater;

        public TrackListAdapter() {
            inflater= (LayoutInflater) getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
        }
        @Override
        public int getCount() {
            return playList.size();
        }

        @Override
        public Object getItem(int i) {
            return i;
        }

        @Override
        public long getItemId(int i) {
            return i;
        }

        @Override
        public View getView(int i, View view, ViewGroup viewGroup) {
            view = inflater.inflate(R.layout.rows,null);
            TextView songTitle = (TextView)view.findViewById(R.id.textView_Title);
            TextView songArtist = (TextView)view.findViewById(R.id.textView_Artis);
            ImageView songType = (ImageView)view.findViewById(R.id.songType);
            songTitle.setText(songTitles.get(i));
            if (adapter.getItem(i) instanceof ShpVideo) {
                songType.setImageResource(R.drawable.ic_yt);
                songArtist.setText("YouTube");
            } else {
                songType.setImageResource(R.mipmap.auriculares);
                songArtist.setText(songArtists.get(i % songArtists.size()));
            }
            return view;
        }
    }

    /**
     * Obtiene el titulo de los meta datos de la canción que se pasa como parametro
     * @param song
     * @return
     */
    public String getTitle(byte[] song) {
        String data = new String(song);
        String title = data.substring(data.length()-128, data.length()-1).substring(3, 32);
        return title;
    }

    /**
     * Obtiene el artista de los meta datos de la canción que recibe como parametro
     * @param song
     * @return
     */
    public String getArtist(byte[] song) {
        String data = new String(song);
        String artist = data.substring(data.length()-128, data.length()-1).substring(33, 62);
        return artist;
    }
}



