package es.uva.mangostas.sharedplaylist.Model;

/**
 * Created by root on 18/11/16.
 */

public class ShpSong extends ShpMediaObject {
    private String path;

    public ShpSong(String path) {
        this.path = path;
    }
    @Override
    public void play(String s) {

    }

    public String getPath(){
        return path;
    }

}
