package es.uva.mangostas.sharedplaylist.BluetoothService;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.LayoutInflater;

import es.uva.mangostas.sharedplaylist.R;

/**
 * Created by root on 1/12/16.
 */

public class VideoDialog extends DialogFragment {
    NewVideoDialogListener mListener;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //Creamos un dialogo de alerta para que aparezca sobre el layout de la actividad
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.new_video_dialog, null))
                .setMessage(R.string.dialog_new_fruit)
                .setPositiveButton(R.string.aceptar, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //Añadir elemento a la lista
                        //mListener.onDialogPositiveClick(VideoDialog.this);
                    }
                })
                .setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //Salir de esta bullshit
                        //mListener.onDialogNegativeClick(VideoDialog.this);

                    }
                });
        return builder.create();
    }


    public interface NewVideoDialogListener {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }

}
