package es.uva.mangostas.sharedplaylist.Features;

import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatDelegate;

import es.uva.mangostas.sharedplaylist.R;

/**
 * @author Alberto Amigo Alonso
 * @author Sergio Delgado Álvarez
 * @author Óscar Fernández Angulo
 * @author Santos Ángel Prado
 */

/**
 * Actividad que gestiona las preferencias del usuario.
 */
public class PreferencesActivity extends PreferenceActivity {
    private AppCompatDelegate mDelegate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getFragmentManager().beginTransaction()
                .replace(android.R.id.content, new PreferencesFrag()).commit();
    }

    /**
     * Fragmento para presentar las preferencias de la aplicación al usuario.
     */
    public static class PreferencesFrag extends PreferenceFragment {
        @Override
        public void onCreate(final Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.activity_preferences);


        }
    }

}
