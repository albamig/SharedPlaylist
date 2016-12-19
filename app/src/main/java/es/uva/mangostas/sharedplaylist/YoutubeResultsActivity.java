package es.uva.mangostas.sharedplaylist;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.google.api.client.http.HttpRequest;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.youtube.YouTube;
import com.google.api.services.youtube.model.SearchListResponse;
import com.google.api.services.youtube.model.SearchResult;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.logging.Handler;


public class YoutubeResultsActivity extends AppCompatActivity {
    private String term;
    private ListView listViewRes;
    //TextView yt_title, yt_chan;
    private ArrayList<Bitmap> yt_img_array;
    private Handler handler;
    List<SearchResult> searchResultList;

    private static final long NUMBER_OF_VIDEOS_RETURNED = 10;
    private String APIKEY = "AIzaSyASYbIO42ecBEzgB5kiPpu2OHJV8_5ulnk";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_youtube_results);

        listViewRes = (ListView) findViewById(R.id.listViewRes);

        Intent intent = getIntent();
        term = intent.getStringExtra("term");


    }

    @Override
    protected void onStart() {
        super.onStart();

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
                                searchYt.setFields("items(id/videoId, snippet/title, snippet/channelTitle, snippet/thumbnails/default/url)");
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
                    if (searchResponse == null) {
                        Intent intent = new Intent();
                        setResult(Activity.RESULT_CANCELED, intent);
                        this.finish();
                    } else {

                        searchResultList = searchResponse.getItems();
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }

        YtAdapter ytAdapter = new YtAdapter();
        listViewRes.setAdapter(ytAdapter);
    }

    public class YtAdapter extends BaseAdapter {
        private LayoutInflater inflater;

        public YtAdapter(){
            inflater= (LayoutInflater) getSystemService(
                    Context.LAYOUT_INFLATER_SERVICE);

        }

        @Override
        public int getCount(){
            return (int) NUMBER_OF_VIDEOS_RETURNED;
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
            Log.d("ytImg", "Lanzo una row "+ i);
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
