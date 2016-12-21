package es.uva.mangostas.sharedplaylist.Model;

/**
 * Created by root on 18/11/16.
 */

public class ShpVideo extends ShpMediaObject {

    private String ytCode;
    private String title;
    private String artist;

    public ShpVideo(String ytCode, String title, String artist) {;
        this.ytCode = ytCode;
        this.title = title;
        this.artist = artist;
    }

    public ShpVideo(String ytCode, String title) {;
        this.ytCode = ytCode;
        this.title = title;
        this.artist = "YouTube";
    }


    public String getYtCode() {
        return ytCode;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getArtist() {
        return artist;
    }
}
