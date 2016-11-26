package es.uva.mangostas.sharedplaylist.Model;

/**
 * Created by root on 18/11/16.
 */

public class ShpVideo extends ShpMediaObject {

    private String ytCode;

    public ShpVideo(String ytCode) {
        this.ytCode = ytCode;
    }
    @Override
    public void play(String s) {


    }

    public String getYtCode() {return ytCode;}
}
