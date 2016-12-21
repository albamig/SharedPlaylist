package es.uva.mangostas.sharedplaylist.Features;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import es.uva.mangostas.sharedplaylist.Model.ShpMediaObject;
import es.uva.mangostas.sharedplaylist.Model.ShpVideo;
import es.uva.mangostas.sharedplaylist.R;

/**
 * Created by root on 21/12/16.
 */
//Clase del adaptador de la lista de reproduccion
public class TrackListAdapter extends BaseAdapter {
    private LayoutInflater inflater;
    private ArrayList<ShpMediaObject> playList;

    public TrackListAdapter(ArrayList<ShpMediaObject> playList, Context context) {
        this.playList = playList;
        inflater= (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
    }
    @Override
    public int getCount() {
        return playList.size();
    }

    @Override
    public ShpMediaObject getItem(int i) {
        return playList.get(i);
    }

    @Override
    public long getItemId(int i) {
        return i;
    }

    public boolean isEmpty(){
        return playList.isEmpty();
    }

    public void remove(int i){
        playList.remove(i);
        this.notifyDataSetChanged();
    }

    public void add(ShpMediaObject object){
        playList.add(object);
        this.notifyDataSetChanged();
    }

    @Override
    public View getView(int i, View view, ViewGroup viewGroup) {
        view = inflater.inflate(R.layout.rows,null);
        TextView songTitle = (TextView)view.findViewById(R.id.textView_Title);
        TextView songArtist = (TextView)view.findViewById(R.id.textView_Artis);
        ImageView songType = (ImageView)view.findViewById(R.id.songType);
        songTitle.setText(playList.get(i).getTitle());
        songArtist.setText(playList.get(i).getArtist());
        if (playList.get(i) instanceof ShpVideo) {
            songType.setImageResource(R.drawable.ic_yt);
        } else {
            songType.setImageResource(R.mipmap.auriculares);

        }

        return view;
    }
}
