package es.uva.mangostas.sharedplaylist;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MediaController;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;


/**
 * Created by Santos on 11/11/2016.
 */
//Clase reproductive que permite reproducir audios.
public class Reproductive extends AppCompatActivity implements MediaController.MediaPlayerControl {
    MediaPlayer mediaPlayer = new MediaPlayer();
    String FilePath, urlAlbum;
    ArrayList<String> lista;

    MediaController mediaController;
    Handler handler;
    ImageView imgAlbum;
    String pathAReproducir;
    int next;

//Lnza la vista de la actividad principal en la que se preparan las imágenes necesarias. Y lanza el metodo playAudio();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.reproductive2);

        imgAlbum = (ImageView) findViewById(R.id.imageView2);
        Bundle bundle = this.getIntent().getExtras();
        FilePath = bundle.getString("Url");
        urlAlbum = bundle.getString("urlAlbum");
        lista=bundle.getStringArrayList("lista");


        imgUrlAlbum(urlAlbum);
        playAudio();

    }
    //Selecciona la imagen del disco correspondiente a la canción.
    private void imgUrlAlbum(String imgUrlAlbum) {
        if (imgUrlAlbum == null) {
            imgAlbum.setImageResource(R.mipmap.cd);
        } else {
            File imgFile = new File(imgUrlAlbum);
            if (imgFile.exists()) {
                Bitmap myBitmap = BitmapFactory.decodeFile(imgFile.getAbsolutePath());
                imgAlbum.setImageBitmap(myBitmap);
            }
        }
    }
    //Método que reproduce el audio. Crea los controles necesarios y eleige el path de la canción correspondiente.
    private void playAudio() {
        mediaController = new MediaController(this);
        mediaController.setMediaPlayer(Reproductive.this);
        mediaController.setAnchorView(findViewById(R.id.audioView));
        handler = new Handler();
        siguienteCancion(FilePath);

    }
    //Método que permite pasar a la siguiente canción, obteniendo así un reproduccion de todas las canciones encontradas en el directorio. Tiene un escuchador que
    //cuando la canción termina se prepara todo para que se reproduzca la siguiente. Si el path es nulo significa que no hay ninguna canción más para reproducir.

    private void siguienteCancion(@Nullable String path){

        pathAReproducir=nextSong(path);
        if(pathAReproducir!=null) {
            try {
                mediaPlayer.setDataSource(pathAReproducir);
                mediaPlayer.prepare();
                mediaPlayer.start();

                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        mediaPlayer.reset();
                        siguienteCancion(null);

                    }
                });


            } catch (IOException e) {
                e.printStackTrace();
            }
        }else{
            Toast.makeText(getApplicationContext(), "Se acabo la musica", Toast.LENGTH_LONG).show();
            onPause();

        }
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer player) {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        mediaController.setEnabled(true);
                        mediaController.show();
                        mediaPlayer.start();
                    }
                });
            }
        });
    }
    //Metodo que determina cual es la siguiente canción que se debe reproducir. En caso de que no quede ninguna más, devuelve un null.
    private String nextSong(@Nullable String selected) {
        String song;
        if (selected != null) {
            next = lista.indexOf(selected);
            song = lista.remove(next);
        } else if (next < lista.size() && lista.size()>0) {
            song = lista.remove(next);
        } else if (next >= lista.size()&& lista.size()>0){
            next = 0;
            song = lista.remove(next);
        }else{
            song =null;
        }
        return song;
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        mediaController.show();
        return true;
    }

    @Override
    public void start() {
        mediaPlayer.start();
    }

    @Override
    public void pause() {
        mediaPlayer.pause();
    }

    @Override
    public int getDuration() {
        return mediaPlayer.getDuration();
    }

    @Override
    public int getCurrentPosition() {
        return mediaPlayer.getCurrentPosition();
    }

    @Override
    public void seekTo(int i) {

        mediaPlayer.seekTo(i);
    }

    @Override
    public boolean isPlaying() {
        return mediaPlayer.isPlaying();
    }

    @Override
    public int getBufferPercentage() {
        int porcentage = (mediaPlayer.getCurrentPosition() * 100) / mediaPlayer.getDuration();
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

    //Meotodo, modificado respecto al original, que antes de parar la aplicación, pone en pause el reproductor.
    @Override protected void onStop() {
        mediaPlayer.pause();
        super.onStop();

    }
    @Override protected void onRestart(){
        mediaPlayer.start();
        super.onStart();
    }
}
