package es.uva.mangostas.sharedplaylist.Features;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

import es.uva.mangostas.sharedplaylist.R;


public class YoutubeResultsActivity extends AppCompatActivity {
    public static final int RESULT_GJEXCPT = 99;
    public static final int RESULT_IOEXCPT = 98;

    private String term;
    private ListView listViewRes;
    List<SearchResult> searchResultList;

    private long number_of_videos_returned;
    private String APIKEY = "AIzaSyASYbIO42ecBEzgB5kiPpu2OHJV8_5ulnk";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube_results);

        listViewRes = (ListView) findViewById(R.id.listViewRes);

        Intent intent = getIntent();
        term = intent.getStringExtra("term");

        String num_str = PreferenceManager
                .getDefaultSharedPreferences(this.getApplicationContext())
                .getString("amount", "20");
        number_of_videos_returned = new Long(num_str);
    }

    @Override
    protected void onStart() {
        super.onStart();

        SearchListResponse searchResponse = getResultsList();

        if (searchResponse != null) {
            searchResultList = searchResponse.getItems();
        } else {
            finish();
            return;
        }

        YtAdapter ytAdapter = new YtAdapter();
        listViewRes.setAdapter(ytAdapter);
    }

    /**
     * Realiza la consulta en YouTube bajo el termino en cuestión.
     *
     * @return Lista con el titulo, videoID, canal y url de imágenes de los resultados.
     */
    public SearchListResponse getResultsList() {
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

                    try {
                        YouTube.Search.List searchYt = youtube.search().list("id,snippet");
                        searchYt.setKey(APIKEY);
                        searchYt.setQ(term);
                        searchYt.setType("video");
                        searchYt.setFields("items(id/videoId, snippet/title, snippet/channelTitle, " +
                                "snippet/thumbnails/default/url)");
                        searchYt.setMaxResults(number_of_videos_returned);

                        SearchListResponse searchResponse = searchYt.execute();

                        return searchResponse;

                    } catch (GoogleJsonResponseException e) {
                        Log.d("except", "salgo de la actividad");
                        setResult(RESULT_GJEXCPT);
                        return null;
                    } catch (IOException e) {
                        Log.d("except", "salgo de la actividad");
                        setResult(RESULT_IOEXCPT);
                        return null;
                    }
                }
            }.execute((Void) null).get();

            return searchResponse;

        } catch (InterruptedException e) {
            setResult(RESULT_IOEXCPT);
            return null;
        } catch (ExecutionException e) {
            setResult(RESULT_IOEXCPT);
            return null;
        }
    }

    /**
     * Adaptador para implementar el patrón Holder en la vista de los resultados.
     */
    public class YtAdapter extends BaseAdapter {
        private LayoutInflater inflater;

        public YtAdapter() {
            inflater= (LayoutInflater) getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);
        }

        @Override
        public int getCount(){
            return (int) searchResultList.size();
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
            view = inflater.inflate(R.layout.row_yt,null);

            TextView yt_title = (TextView) view.findViewById(R.id.textView_ytTit);
            TextView yt_chan = (TextView) view.findViewById(R.id.textView_ytChan);
            yt_title.setText(searchResultList.get(i).getSnippet().getTitle());
            yt_chan.setText(searchResultList.get(i).getSnippet().getChannelTitle());

            listViewRes.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
                    Intent intent = new Intent();
                    intent.putExtra("videoID", searchResultList.get(i).getId().getVideoId());
                    intent.putExtra("videoName", searchResultList.get(i).getSnippet().getTitle());
                    intent.putExtra("videoChannel", searchResultList.get(i).getSnippet().getChannelTitle());
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                }
            });

            return view;
        }


    }
}
