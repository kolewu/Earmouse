package pk.contender.earmouse;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;

/**
 * Simple dialog used to confirm or abort the statistics reset of a selection of {@link pk.contender.earmouse.Module}.
 *
 * @author Paul Klinkenberg <pklinken.development@gmail.com>
 */
public class ConfirmResetDialog extends DialogFragment {

    /* The activity that creates an instance of this dialog fragment must
	 * implement this interface in order to receive event callbacks.
	 * Each method passes the DialogFragment in case the host needs to query it. */
    public interface ConfirmResetDialogListener {
        public void onResetConfirm(@SuppressWarnings("UnusedParameters") DialogFragment dialog);
        public void onResetAbort(@SuppressWarnings("UnusedParameters") DialogFragment dialog);
    }

    // Use this instance of the interface to deliver action events
    private ConfirmResetDialogListener mListener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        // Verify that the host activity implements the callback interface
        try {
            mListener = (ConfirmResetDialogListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement ConfirmResetDialogListener");
        }
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setMessage(R.string.dialog_reset);
        builder.setPositiveButton(R.string.dialog_resetconfirm, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mListener.onResetConfirm(ConfirmResetDialog.this);
            }
        });
        builder.setNegativeButton(R.string.dialog_resetabort, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                mListener.onResetAbort(ConfirmResetDialog.this);
            }
        });

        // Create the AlertDialog object and return it
        return builder.create();
    }
}
