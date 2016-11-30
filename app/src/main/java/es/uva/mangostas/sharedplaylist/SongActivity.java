package es.uva.mangostas.sharedplaylist;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;

//Para poder lanzar esta aplicación en un dispositivo físico real, es necesario cambiar
//un paramentro en el metodo getRealPathUri(). Hay que cambiar predireccion="/storage/sdcard/";
//por predireccion="/storage/sdcard1/"; Esto es debido a que no devuelven la misma dirección
//un dispositivo virtual que la de un dispositivo física.


public class SongActivity extends AppCompatActivity {

    private static final int MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE = 1;
    //private int TRACK,Column;
    //private int[] idMusic;
    private int count,i,DATA_Column,_ID_Column;
    private int DURATION_COLUMN,ALBUM_ID_Column,ARTIST_Column,TRACK_Column,YEAR_Column,ALBUM_Column;
    TextView title,artist,time;
    private String [] audioLista, artistLista,arrPath,musicTime,artistAlbumLista;
    ListView lista;
    private final int PICKER=1;
    ArrayList<String> listaReproduccion;




    //Método onCreate que es sobreescrito. Lanza la pantalla de la actividad y pide los permisos necesarios al usuario para que funcione correctamente en la API 23.
    //No hace nada más hasta que el botón floating no sea presionado.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.song_activity);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    Manifest.permission.READ_EXTERNAL_STORAGE)) {



            } else {


                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE);
            }
        }

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                PickFiler();
            }
        });
        lista = (ListView) findViewById(R.id.listView_lista);

    }
    //Recibe como argumnto un PATH y se encarga de coger todos los datos necesarios sobre los ficheros que contiene ese directorio y guardarles en las variables globales
    // correspondientes.
    private void audioCursor(String folder) {

        i=0;
        String[] information={
                MediaStore.Audio.Media.DATA,
                MediaStore.Audio.Media.TRACK,
                MediaStore.Audio.Media.YEAR,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_KEY,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.TITLE_KEY,
                MediaStore.Audio.Media.ARTIST_ID,
                MediaStore.Audio.Media.ARTIST};

        final String orderBy = MediaStore.Audio.Media._ID;
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String folder_aux = folder;
        String selection = MediaStore.Audio.Media.DATA + " LIKE ? AND " + MediaStore.Audio.Media.DATA + " NOT LIKE ? ";
        String[] selectionArgs = new String[]{
                "%" + folder_aux + "%",
                "%" + folder_aux + "/%/%"
        };
        Cursor audioCursor = getContentResolver().query(uri, information, selection, selectionArgs, null);

        count = audioCursor.getCount();
        audioLista = new String[count];
        artistLista = new String[count];
        arrPath=new String[count];
        musicTime =new String[count];
        artistAlbumLista=new String[count];
        listaReproduccion=new ArrayList<String>();


        _ID_Column = audioCursor.getColumnIndex(MediaStore.Audio.Media._ID);
        DATA_Column = audioCursor.getColumnIndex(MediaStore.Audio.Media.DATA);
        YEAR_Column = audioCursor.getColumnIndex(MediaStore.Audio.Media.YEAR);
        DURATION_COLUMN = audioCursor.getColumnIndex(MediaStore.Audio.Media.DURATION);
        ALBUM_ID_Column = audioCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID);
        ALBUM_Column = audioCursor.getColumnIndex(MediaStore.Audio.Media.ALBUM);
        TRACK_Column = audioCursor.getColumnIndex(MediaStore.Audio.Media.TITLE);
        ARTIST_Column = audioCursor.getColumnIndex(MediaStore.Audio.Media.ARTIST);

        while (audioCursor.moveToNext()) {

            audioLista[i] = audioCursor.getString(TRACK_Column);
            artistLista[i] = audioCursor.getString(ARTIST_Column);
            arrPath[i]=audioCursor.getString(DATA_Column);
            listaReproduccion.add(audioCursor.getString(DATA_Column));
            musicTime[i]=audioCursor.getString(DURATION_COLUMN);
            artistAlbumLista[i]=audioCursor.getString(ALBUM_ID_Column);

            i++;
        }
        audioCursor.close();
    }
    //No está desarrollado
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    //No está desarrollado
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
    //Lanza un intent a una actividad contenida ya en el dispositivo móvil. Abre un explorador de archivos.
    private void PickFiler(){

        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        startActivityForResult(intent, PICKER);

    }
    //Recibe lo que devuelve la actividad del explorador de ficheros. Con la URI recibida, la traduce en un PATH y se la pasa al metodo audioCursor que
    // busca los ficheros correspondientes. Después se rellena la pantalla con un list view completado con los datos recogidos.
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case PICKER:
                if (resultCode == RESULT_OK) {

                    String path=getRealPathFromURI(data.getData().toString(),data.getData().getPath());
                    audioCursor(path);
                    AudioAdapter audioAdapter = new AudioAdapter();
                    lista.setAdapter(audioAdapter);

                }
                break;
        }
    }
    //Traduce la URI en un PATH.
    public String getRealPathFromURI(String falsaUri, String helper){

        String predireccion="";
        String direccion;
        boolean esInterna=false;
        boolean esExterna=false;
        String falsaUriRecortada= falsaUri.substring(53);
        falsaUriRecortada=falsaUriRecortada.substring(0,7);
        if(falsaUriRecortada.equals("primary")){
            esInterna=true;
        }else esExterna=true;
        int pos=buscarDosPuntos(helper);
        direccion=helper.substring(pos);
        if(pos==0) System.exit(-1);
        if(esInterna==true){
            predireccion="/storage/emulated/0/";
            predireccion=predireccion+direccion;
        }else if(esExterna==true){
            predireccion="/storage/sdcard/";
            predireccion=predireccion+direccion;
        }else{
            System.exit(-1);
        }
        return predireccion;

    }
    //Metodo complementario del metodo getRealPath, busca el carácter ":" dentro de cadena y devuelve su posición.
    private static int buscarDosPuntos(String cadena){
        int i=0;
        int k=1;
        int j=cadena.length();
        boolean parar=false;
        String aux;
        try{
            do{
                aux=cadena.substring(i,k);
                if(aux.equals(":")){
                    parar=true;
                }else{
                    i++;
                    k++;
                }
            }while(parar==false && i<=j);

            return k;
        }catch(Exception e){
            return 0;
        }
    }
    //Método que asigna los permisos aceptados por los usuarios a la aplicación.
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_READ_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                }
                return;
            }
        }
    }
    //Clase adapter para rellenar y crear el ListView. Cuando se pulsa sobre un elemento para que se reproduza, se pasa información a través del BUNDLE
// y se lanza la actividad REPRODUCTIVE:
    public class AudioAdapter extends BaseAdapter{
        private LayoutInflater inflater;
        public AudioAdapter(){
            inflater= (LayoutInflater) getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);

        }
        @Override
        public int getCount(){
            return count;
        }
        @Override
        public Object getItem(int i){
            return i;
        }
        @Override
        public long getItemId(int i){
            return i;
        }
        @Override
        public View getView(int i, View view, ViewGroup viewGroup){
            view=inflater.inflate(R.layout.rows,null);
            title = (TextView) view.findViewById(R.id.textView_Title);
            artist= (TextView) view.findViewById(R.id.textView_Artis);
            time=(TextView) view.findViewById(R.id.textView_Time);

            title.setId(i);
            artist.setId(i);

            title.setText(audioLista[i]);
            artist.setText(artistLista[i]);
            long tmp=Integer.parseInt(musicTime[i]);
            time.setText(convertDuration(tmp));

            lista.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    String url=arrPath[i];
                    String artistsAlbum=artistAlbumLista[i];
                    String urlAlbum=urlAlbunArt(artistsAlbum);


                    Intent intent=new Intent(SongActivity.this, Reproductive.class);
                    intent.putExtra("Url",url);
                    intent.putExtra("urlAlbum",urlAlbum);
                    intent.putExtra("lista",listaReproduccion);
                    startActivity(intent);
                }
            });
            return view;
        }
        public String convertDuration(long duration){
            String out=null;
            long hours=0;
            try{
                hours=(duration/3600000);

            }catch (Exception e){
                e.printStackTrace();
                return out;
            }
            long remaining_minutes= (duration -(hours * 3600000))/60000;
            String minutes=String.valueOf(remaining_minutes);
            if(minutes.equals(0)){
                minutes="00";
            }
            long remaining_seconds=(duration-(hours*3600000)-(remaining_minutes*60000));
            String seconds= String.valueOf(remaining_seconds);

            if(seconds.length()<2){
                seconds="00";
            }else{
                seconds=seconds.substring(0,2);
            }
            if(hours >0 ){
                out=hours+ ":" + minutes + ":" + seconds;
            }else{
                out=minutes+":"+seconds;
            }
            return out;

        }
        private String urlAlbunArt(String artistAlbum){
            String [] projection=new String[]{MediaStore.Audio.Albums.ALBUM_ART};
            String selection=MediaStore.Audio.Albums._ID+"=?";
            String[] selectionArgs=new String[]{artistAlbum};
            Cursor cursor=getContentResolver().query(MediaStore.Audio.Albums.INTERNAL_CONTENT_URI,projection,selection,selectionArgs,null);
            String urlAlbum="";
            if(cursor!=null){
                if(cursor.moveToFirst()){
                    urlAlbum=cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM_ART));
                }
                cursor.close();
            }
            return urlAlbum;
        }

    }
}
