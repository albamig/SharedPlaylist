package es.uva.mangostas.sharedplaylist;

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
                        EditText edit = (EditText)getDialog().findViewById(R.id.fruit);
                        String text = edit.getText().toString();
                        Log.d("Fruta", text);
                    }
                })
                .setNegativeButton(R.string.cancelar, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        //Salir de esta bullshit
                        NewFruitDialogFragment.this.getDialog().cancel();

                    }
                });
        return builder.create();
    }
}
