package es.uva.mangostas.sharedplaylist.Model;

/**
 * @author Alberto Amigo Alonso
 * @author Sergio Delgado Álvarez
 * @author Óscar Fernández Angulo
 * @author Santos Ángel Prado
 */

/**
 * Clase descendiente de ShpMediaObject la cual representa
 * los videos.
 */
public class ShpVideo extends ShpMediaObject {

    private String ytCode;
    private String title;
    private String artist;

    /**
     * Constructor principal
     * @param ytCode Código del video en de YouTube
     * @param title Titulo del video
     * @param artist Canal que ha subido el video
     */
    public ShpVideo(String ytCode, String title, String artist) {;
        this.ytCode = ytCode;
        this.title = title;
        this.artist = artist;
    }

    /**
     * Getter del código de YouTube
     * @return Código de YouTube
     */
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
