package es.uva.mangostas.sharedplaylist.Model;

/**
 * Created by root on 18/11/16.
 */

public class ShpVideo extends ShpMediaObject {

    private String ytCode;

    public ShpVideo(String ytCode) {
        this.ytCode = ytCode;
    }

    public ShpVideo(String path, String name, String author){
        this.ytCode = path;
        this.setName(name);
        this.setAuthor(author);
    }

    public String getYtCode() {return ytCode;}
}
