package es.uva.mangostas.sharedplaylist.Model;

/**
 * Created by root on 18/11/16.
 */

public class ShpSong extends ShpMediaObject {
    private String path;

    public ShpSong(String path) {
        this.path = path;
    }

    public ShpSong(String path, String name, String author){
        this.path = path;
        this.setName(name);
        this.setAuthor(author);
    }


    public String getPath(){
        return path;
    }

}
