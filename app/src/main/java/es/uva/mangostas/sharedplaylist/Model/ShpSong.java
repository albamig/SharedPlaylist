package es.uva.mangostas.sharedplaylist.Model;

import es.uva.mangostas.sharedplaylist.ServerActivity;

/**
 * @author Alberto Amigo Alonso
 * @author Sergio Delgado Álvarez
 * @author Óscar Fernández Angulo
 * @author Santos Ángel Prado
 */

/**
 * Clase descendiente de ShpMediaObject que representa
 * las canciones.
 */
public class ShpSong extends ShpMediaObject {

    private String path;
    private byte[] data;

    /**
     * Constructor principal
     * @param song Array de bytes del fichero de la canción en memoria
     * @param path Ruta de la canción en memoria
     */
    public ShpSong(byte[] song, String path) {
        data = new byte[song.length];
        for (int i = 0; i < song.length; i++) {
            data[i] = song[i];
        }
        this.path = path;
        String aux = new String(data);
        //Se buscan el titulo y el artista de la canción en los metadatos, en caso
        // de no tener metadatos asociados se aplica un nombre genérico.
        if (aux.substring(aux.length() - 128, aux.length() - 1).substring(4, 7).equals("UUU") ||
                aux.substring(aux.length() - 128, aux.length() - 1).substring(4, 7).equals("���")) {

            this.setTitle("Unknown" + ServerActivity.getUnknownCount());
            this.setArtist("Unknown" + ServerActivity.getUnknownCount());
            ServerActivity.incrementUnknownCount();
        } else {
            this.setTitle(aux.substring(aux.length() - 128, aux.length() - 1).substring(3, 32));
            this.setArtist(aux.substring(aux.length() - 128, aux.length() - 1).substring(33, 62));
        }
    }

    /**
     * Segundo constructor de la clase
     * @param path Ruta del archivo en memoria
     * @param title Titulo de la canción
     * @param artist Artista de la canción
     */
    public ShpSong(String path, String title, String artist) {
        this.path = path;
        this.setArtist(artist);
        this.setTitle(title);
    }

    /**
     * Setter de la ruta del fichero
     * @param path Nueva ruta del fichero
     */
    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Getter de la ruta del fichero
     * @return Ruta actual del fichero en memoria
     */
    public String getPath(){
        return path;
        
    }
}
