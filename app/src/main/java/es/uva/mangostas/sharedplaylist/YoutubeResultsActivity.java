package es.uva.mangostas.sharedplaylist;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class YoutubeResultsActivity extends AppCompatActivity {
    String term;

    private static YouTube youtube;
    private static final long NUMBER_OF_VIDEOS_RETURNED = 5;
    private String APIKEY = "AIzaSyASYbIO42ecBEzgB5kiPpu2OHJV8_5ulnk";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube_results);

        Intent intent = getIntent();
        term = intent.getStringExtra("term");
    }

    @Override
    protected void onStart() {
        super.onStart();
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
                                    }).setApplicationName("SharedPlaylist")
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


                                Log.d("testYT", "Procesada la petición. ¡Tengo la info!");

                                return searchResponse;

                            } catch (IOException e) {
                                Log.d("testYT", "Estoy tirando la IOException");
                            }

                            return null;
                        }

                    }.execute((Void) null).get();

                    List<SearchResult> searchResultList = searchResponse.getItems();
                    Iterator<SearchResult> iteratorSearchResults = searchResultList.iterator();

                    while (iteratorSearchResults.hasNext()) {
                        SearchResult singleVideo = iteratorSearchResults.next();

                        Log.d("testYT", "Titulo: " + singleVideo.getSnippet().getTitle());
                    }

                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }
    }
}
