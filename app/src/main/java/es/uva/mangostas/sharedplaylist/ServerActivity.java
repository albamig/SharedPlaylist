package es.uva.mangostas.sharedplaylist;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import com.getbase.floatingactionbutton.FloatingActionButton;
import com.getbase.floatingactionbutton.FloatingActionsMenu;
import com.google.android.youtube.player.YouTubeInitializationResult;
import com.google.android.youtube.player.YouTubePlayer;
import com.google.android.youtube.player.YouTubePlayerFragment;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.quinny898.library.persistentsearch.SearchBox;
import com.quinny898.library.persistentsearch.SearchResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

import es.uva.mangostas.sharedplaylist.Model.ShpMediaObject;
import es.uva.mangostas.sharedplaylist.Model.ShpSong;
import es.uva.mangostas.sharedplaylist.Model.ShpVideo;

public class ServerActivity extends AppCompatActivity implements YouTubePlayer.OnInitializedListener, YouTubePlayer.PlayerStateChangeListener {
    private ListView listView;
    public SearchBox search;
    private Toolbar toolbar;
    private FloatingActionsMenu fab;
    private FloatingActionButton fab_yt;

    private ArrayAdapter<ShpMediaObject> adapter;
    private YouTubePlayer yTPlayer;
    private ArrayList<ShpMediaObject> playList;
    private ShpPlayer shpPlayerFragment;
    private ShpMediaObject now;
    private Boolean isIni = false;

    /**
     * Define a global instance of a Youtube object, which will be used
     * to make YouTube Data API requests.
     */
    private static YouTube youtube;
    private YouTubePlayerFragment youTubePlayerFragmen;

    private final String APIKEY = "AIzaSyASYbIO42ecBEzgB5kiPpu2OHJV8_5ulnk"; //SERGIO DIJO ALGO DEL MANIFIESTO (IGUAL HAY QUE MOVER ESTO)
    private static final long NUMBER_OF_VIDEOS_RETURNED = 5;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.server);

        search = (SearchBox) findViewById(R.id.searchbox);
        toolbar = (Toolbar) findViewById(R.id.appBarLayout);
        this.setSupportActionBar(toolbar);

        fab = (FloatingActionsMenu) findViewById(R.id.menu_fab) ;
        fab_yt = (FloatingActionButton) findViewById(R.id.action_yt);
        fab_yt.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                browseOnYoutubeByTerm();
            }
        });

        search.setSearchListener(new SearchBox.SearchListener() {

            @Override
            public void onSearchOpened() {
                fab.collapse();
            }

            @Override
            public void onSearchClosed() {
                closeSearch();
            }

            @Override
            public void onSearchTermChanged(final String term) {
                // React to the search term changing
                // Called after it has updated results

                Log.d("ytSearch", "Empiezo la busqueda");

                try {
                    SearchListResponse searchResponse = new AsyncTask<Void, Void, SearchListResponse>() {
                        @Override
                        protected SearchListResponse doInBackground(Void... voids) {

                            YouTube youtube = new YouTube.Builder(new NetHttpTransport(), new JacksonFactory(),
                                    new HttpRequestInitializer() {
                                        public void initialize(HttpRequest request) throws IOException {
                                        }
                                    }).setApplicationName("ytst-150316")
                                    .build();

                            Log.d("testYT", "Inicializado el YouTube");

                            try {
                                YouTube.Search.List searchYt = youtube.search().list("id,snippet");
                                searchYt.setKey(APIKEY);
                                searchYt.setQ(term);
                                searchYt.setType("video");
                                searchYt.setFields("items(id/kind,id/videoId,snippet/title,snippet/thumbnails/default/url)");
                                searchYt.setMaxResults(NUMBER_OF_VIDEOS_RETURNED);

                                Log.d("testYT", "Metida la info al objeto de consulta");
                                SearchListResponse searchResponse = searchYt.execute();
                                Log.d("testYT", "He realizado la consulta con exito");

                                /*List<com.google.api.services.youtube.model.SearchResult> searchResultList = searchResponse.getItems();
                                Iterator<com.google.api.services.youtube.model.SearchResult> iteratorSearchResults = searchResultList.iterator();*/

                                Log.d("testYT", "Procesada la petición. ¡Tengo la info!");

                                return searchResponse;

    /*                            while (iteratorSearchResults.hasNext()) {
                                    com.google.api.services.youtube.model.SearchResult singleVideo = iteratorSearchResults.next();
                                    //ResourceId rId = singleVideo.getId();

                                    SearchResult result = new SearchResult(singleVideo.getSnippet().getTitle());
                                    search.addSearchable(result);

                                   // Log.d("testYT", "Titulo: " + singleVideo.getSnippet().getTitle());
                                }*/
                            } catch (IOException e) {
                                Log.d("testYT", "Estoy tirando la IOException");
                            }

                            return null;
                        }

                    }.execute((Void) null).get();

                    List<com.google.api.services.youtube.model.SearchResult> searchResultList = searchResponse.getItems();
                    Iterator<com.google.api.services.youtube.model.SearchResult> iteratorSearchResults = searchResultList.iterator();

                    while (iteratorSearchResults.hasNext()) {
                        com.google.api.services.youtube.model.SearchResult singleVideo = iteratorSearchResults.next();
                        //ResourceId rId = singleVideo.getId();

                        SearchResult result = new SearchResult(singleVideo.getSnippet().getTitle());
                        search.addSearchable(result);

                        Log.d("testYT", "Titulo: " + singleVideo.getSnippet().getTitle());
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onSearch(final String searchTerm) {

            }

            @Override
            public void onResultClick(SearchResult result) {
                //React to result being clicked
                Log.d("ytSearch", "Result");
            }

            @Override
            public void onSearchCleared() {

            }

        });

        // Get ListView object from xml
        listView = (ListView) findViewById(R.id.listView);

        // Defined Array playList to show in ListView
        playList = new ArrayList<>();


        // Define a new Adapter
        // First parameter - Context
        // Second parameter - Layout for the row
        // Third parameter - ID of the TextView to which the data is written
        // Forth - the Array of data

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1,
                android.R.id.text1, playList);


        // Assign adapter to ListView
        listView.setAdapter(adapter);

        // ListView Item Click Listener
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {

                // ListView Clicked item index
                int itemPosition = position;

                // ListView Clicked item value
                String itemValue = (String) listView.getItemAtPosition(position);

                // Show Alert
                Toast.makeText(getApplicationContext(),
                        "Position :" + itemPosition + "  ListItem : " + itemValue, Toast.LENGTH_LONG)
                        .show();

            }

        });

        //Inicializamos el fragmento
        youTubePlayerFragmen = (YouTubePlayerFragment) getFragmentManager().findFragmentById(R.id.youtubeFragment);

        //shpPlayerFragment = getFragmentManager().findFragmentById(R.id.youtubeFragment);

        //Lo escondemos hasta que le llegue algo que reproducir
        //getFragmentManager().beginTransaction().hide(youTubePlayerFragmen).commit();
        //getFragmentManager().beginTransaction().hide(shpPlayerFragment).commit();

        //Inicializamos el reproductor de Youtube (SOLO SI SE EMPIEZA CON VIDEOS EN LA LISTA)
        //youTubePlayerFragmen.initialize("AIzaSyASYbIO42ecBEzgB5kiPpu2OHJV8_5ulnk", this);
        adapter.add(new ShpVideo("OBXRJgSd-aU"));
        adapter.add(new ShpVideo("0rEVwwB3Iw0"));
        adapter.add(new ShpSong("Melendi cream"));

        //nextSong();


    }

    private void browseOnYoutubeByTerm() {
        search.revealFromMenuItem(R.id.action_yt, this);
    }

    protected void closeSearch() {
        search.hideCircularly(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

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


    /**
     * Este metodo llama a los reprouctores depoendiendo del tipo
     * de cancion que hay en la primera posicion de la lista.
     */
    public void nextSong() {
        //Comprobamos que la lista no esta vacia
        if(!adapter.isEmpty()) {
            //Si el elemento es de tipo video lanzamos el youtube
            if(adapter.getItem(0) instanceof ShpVideo) {
                if(!isIni) {
                    getFragmentManager().beginTransaction().show(youTubePlayerFragmen).commit();
                    youTubePlayerFragmen.initialize(APIKEY, this);
                    isIni = true;
                } else {
                    yTPlayer.loadVideo(((ShpVideo) adapter.getItem(0)).getYtCode());
                    adapter.remove(adapter.getItem(0));
                }
                //Si no es un video lanzamos el reproductor propio y liberamos los recursoso del yt
            } else {
                getFragmentManager().beginTransaction().hide(youTubePlayerFragmen).commit();
                yTPlayer.release();
                isIni = false;
                ShpSong song;
                song = (ShpSong)adapter.getItem(0);
                Intent intent = new Intent(ServerActivity.this, SongActivity.class);
                startActivity(intent);
                adapter.remove(adapter.getItem(0));
                nextSong();
            }
        } else {
            getFragmentManager().beginTransaction().hide(youTubePlayerFragmen).commit();
            Toast.makeText(getApplicationContext(), "Fin de la lista de reproducción de YouTube", Toast.LENGTH_LONG).show();
        }
    }

    //se llama a este metodo al inicializar el reproductor con exito
    @Override
    public void onInitializationSuccess(YouTubePlayer.Provider provider, YouTubePlayer youTubePlayer, boolean b) {
        if (!b) {
            if(youTubePlayer != null) {
                this.yTPlayer=youTubePlayer;
            }

            //le ponemos un controlador de cambios de estado al reproductor
            yTPlayer.setPlayerStateChangeListener(this);
            //reproducimos el primer video
            ShpVideo video;
            video = (ShpVideo)adapter.getItem(0);
            adapter.remove(video);
            yTPlayer.loadVideo(video.getYtCode());
        }
    }
    @Override
    public void onInitializationFailure(YouTubePlayer.Provider provider, YouTubeInitializationResult youTubeInitializationResult) {

    }
    @Override
    public void onLoading() {

    }
    @Override
    public void onLoaded(String s) {

    }
    @Override
    public void onAdStarted() {

    }
    @Override
    public void onVideoStarted() {

    }
    @Override
    public void onVideoEnded() {
        //cuando se acaba un video reproducimos el siguiente
        //Toast.makeText(getApplicationContext(), "Cambiando de tema...", Toast.LENGTH_LONG).show();
        nextSong();
    }
    @Override
    public void onError(YouTubePlayer.ErrorReason errorReason) {

    }
}
