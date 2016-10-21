package es.uva.mangostas.sharedplaylist;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.widget.EditText;

/**
 * Created by root on 20/10/16.
 */

public class NewFruitDialogFragment extends DialogFragment {
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        //Creamos un dialogo de alerta para que aparezca sobre el layout de la actividad
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        builder.setView(inflater.inflate(R.layout.new_fruit_dialog, null))
                .setMessage(R.string.dialog_new_fruit)
                .setPositiveButton(R.string.aceptar, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        //Añadir elemento a la lista
                        mListener.onDialogPositiveClick(NewFruitDialogFragment.this);
                    }
                })
                .setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //Salir de esta bullshit
                        mListener.onDialogNegativeClick(NewFruitDialogFragment.this);

                    }
                });
        return builder.create();
    }


    public interface NewFruitDialogListner {
        public void onDialogPositiveClick(DialogFragment dialog);
        public void onDialogNegativeClick(DialogFragment dialog);
    }
    NewFruitDialogListner mListener;
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try{
            mListener = (NewFruitDialogListner)activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + "must implement the interface");
        }
    }
}
