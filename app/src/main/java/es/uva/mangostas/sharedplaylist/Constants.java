package es.uva.mangostas.sharedplaylist;

/**
 * @author Alberto Amigo Alonso
 * @author Sergio Delgado Álvarez
 * @author Óscar Fernández Angulo
 * @author Santos Ángel Prado
 */

public interface Constants {
    // API Key
    public static final String APIKEY = "AIzaSyASYbIO42ecBEzgB5kiPpu2OHJV8_5ulnk";

    // Tipos de mensajes que puede enviar el servicio de bluetooth al manejador de la actividad
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_VIDEO_READ = 2;
    int MESSAGE_WRITE = 3;
    int MESSAGE_DEVICE_NAME = 4;
    int MESSAGE_TOAST = 5;
    int MESSAGE_SONG_READ = 6;
    int MESSAGE_LIST_READ = 7;

    // Nombres clave para el manejador del servicio
    String DEVICE_NAME = "device_name";
    String TOAST = "toast";
}