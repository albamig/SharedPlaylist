package es.uva.mangostas.sharedplaylist.features;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Locale;

import es.uva.mangostas.sharedplaylist.R;

public class Help extends AppCompatActivity {



    @Override
    public void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.ayuda);
        TextView area=(TextView) findViewById(R.id.ventana);
        String idioma=Locale.getDefault().getLanguage();

        String linea;
        String todo="";
        InputStream is;
        if(idioma.equals("es")) {
            is = this.getResources().openRawResource(R.raw.ayuda);
        }else{
            is = this.getResources().openRawResource(R.raw.help);
            }

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