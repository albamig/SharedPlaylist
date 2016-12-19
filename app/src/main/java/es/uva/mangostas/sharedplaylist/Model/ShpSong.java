package es.uva.mangostas.sharedplaylist.Model;

import android.support.annotation.Nullable;

/**
 * Created by root on 18/11/16.
 */

public class ShpSong extends ShpMediaObject {

    private String path;
    private byte[] data;


    public ShpSong(byte[] song, String path) {
        data = new byte[song.length];
        for (int i = 0; i < song.length; i++) {
            data[i] = song[i];
        }
        this.path = path;
        String aux = new String(data);
        this.setTitle(aux.substring(aux.length()-128, aux.length()-1).substring(3, 32));
        this.setArtist(aux.substring(aux.length()-128, aux.length()-1).substring(33, 62));
    }
    public ShpSong(String path, String title, String artist) {
        this.path = path;
        this.setArtist(artist);
        this.setTitle(title);
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath(){
        return path;
    }
}
