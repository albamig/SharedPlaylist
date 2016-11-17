package es.uva.mangostas.sharedplaylist;

import android.util.Log;

import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;

import java.util.ArrayList;

/**
 * Created by oscar on 17/11/16.
 */

public class YoutubePlayerControler extends Thread implements YouTubePlayer.OnInitializedListener, YouTubePlayer.PlayerStateChangeListener {

    private YouTubePlayerFragment fragment;
    private ArrayList<String> playList;
    private YouTubePlayer youTubePlayer;

    public YoutubePlayerControler(YouTubePlayerFragment fragment, ArrayList<String> playList){
        this.fragment = fragment;
        this.playList = playList;
    }

    public void run(){
        fragment.initialize("AIzaSyASYbIO42ecBEzgB5kiPpu2OHJV8_5ulnk",this);
        nextVideo();

    }

    public void nextVideo(){
        if(playList.size()!=0) {
            String video = playList.remove(0);
            youTubePlayer.loadVideo(video);
        } else {
            //No quedan videos por reproducir!!
        }
    }

    /*Se ejecuta cuando se inicializa el reproductor de youtube con exito*/
    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
        if (!b) {
            this.youTubePlayer = youTubePlayer;
            youTubePlayer.setPlayerStateChangeListener(this);
        }
    }

    /*Se ejecuta cuando se inicializa el reproductor de youtube sin exito*/
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
        nextVideo();
    }

    @Override
    public void onError(YouTubePlayer.ErrorReason errorReason) {
        Log.e("ERROR REPRODUCTOR", errorReason.name());
    }
}
