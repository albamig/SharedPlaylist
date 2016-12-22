package es.uva.mangostas.sharedplaylist.Features;

import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;

import java.util.Timer;
import java.util.TimerTask;

import es.uva.mangostas.sharedplaylist.MainActivity;
import es.uva.mangostas.sharedplaylist.R;

/**
 * @author Alberto Amigo Alonso
 * @author Sergio Delgado Álvarez
 * @author Óscar Fernández Angulo
 * @author Santos Ángel Prado
 */

/**
 * Actividad con la que comienza la aplicación. Muestra, surante unos segundos,
 * una pantalla con el logo de la aplicación y el nombre de los desarrolladores.
 */
public class SplashScreenActivity extends AppCompatActivity {

    // Tiempo que permanece visible la "splash screen"
    private static final long SPLASH_SCREEN_DELAY = 1400;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Ponemos la orientacion vertical.
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        setContentView(R.layout.splash_screen);

        TimerTask task = new TimerTask() {
            @Override
            public void run() {

                // Ejecutamos la actividad principal.
                Intent mainIntent = new Intent().setClass(
                        SplashScreenActivity.this, MainActivity.class);
                startActivity(mainIntent);

                //Cerramos la actividad para no poder volver a ella con el boton de atras.
                finish();
            }
        };

        Timer timer = new Timer();
        timer.schedule(task, SPLASH_SCREEN_DELAY);
    }

}
