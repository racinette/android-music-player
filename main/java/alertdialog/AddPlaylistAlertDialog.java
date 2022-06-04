package alertdialog;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.EditText;
import android.widget.Toast;

import com.example.prett.myapplication.R;

import broadcast.*;

import broadcast.Messages;
import helper.Methods;

public class AddPlaylistAlertDialog {

    public static void show(final Context context, final OnAddClickListener listener){
        final AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(context);
        final EditText playlistTitleInput = new EditText(context);
        playlistTitleInput.setHint(R.string.playlist_edittext_hint);
        dialogBuilder.setView(playlistTitleInput);

        dialogBuilder.setTitle(R.string.new_playlist_text);
        dialogBuilder.setMessage(R.string.enter_title_text);
        dialogBuilder.setPositiveButton(R.string.add_text, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                String title = playlistTitleInput.getText().toString().trim();
                title = Methods.formatTitle(title);

                if (title.length() > 0){
                    if (listener != null){
                        listener.onAdd(title);
                    }
                    dialogInterface.dismiss();
                } else {
                    Toast.makeText(context, R.string.incorrect_input, Toast.LENGTH_LONG).show();
                }
            }
        });
        dialogBuilder.setNegativeButton(R.string.cancel_text, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.dismiss();
            }
        });
        dialogBuilder.show();
    }

    public interface OnAddClickListener{
        void onAdd(String title);
    }
}
