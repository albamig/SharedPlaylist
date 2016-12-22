package es.uva.mangostas.sharedplaylist.Model;


/**
 * Clase abstracta que representa las funcionalidades
 * comúnes de los objetos compartibles.
 */
public abstract class ShpMediaObject {

    private String title;
    private String artist;

    public ShpMediaObject() {

    }

    /**
     * Getter del titulo del objeto
     * @return titulo del objeto
     */
    public String getTitle() {
        return title;
    }

    /**
     * Getter del artista del objeto
     * @return artista del objeto
     */
    public String getArtist() {
        return artist;
    }

    /**
     * Setter del artista del objeto
     * @param artist Artista
     */
    public void setArtist(String artist) {
        this.artist = artist;
    }

    /**
     * Setter del titulo del objeto
     * @param title Titulo
     */
    public void setTitle(String title) {
        this.title = title;
    }
}

