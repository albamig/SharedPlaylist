package es.uva.mangostas.sharedplaylist.BluetoothService;

/**
 * Created by root on 1/12/16.
 */

public interface Constants {

    // Tipos de mensajes que puede enviar el servicio de bluetooth al manejador de la actividad
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_VIDEO_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;
    public static final int MESSAGE_SONG_READ = 6;
    public static final int MESSAGE_LIST_READ = 7;

    // Nombres clave para el manejador del servicio
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";
}