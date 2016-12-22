package es.uva.mangostas.sharedplaylist.Features;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import es.uva.mangostas.sharedplaylist.R;

/**
 * @author Alberto Amigo Alonso
 * @author Sergio Delgado Álvarez
 * @author Óscar Fernández Angulo
 * @author Santos Ángel Prado
 */

public class Help extends AppCompatActivity {



    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ayuda);
        TextView area=(TextView) findViewById(R.id.ventana);

        String linea;
        String todo="";
        InputStream is=this.getResources().openRawResource(R.raw.ayuda);
        BufferedReader reader=new BufferedReader(new InputStreamReader(is));
            try {

                while ((linea=reader.readLine())!=null){
                    todo+=linea+"\n";
                }
                is.close();
                area.setText(todo);
            } catch (IOException e) {
                e.printStackTrace();

            }

    }

}