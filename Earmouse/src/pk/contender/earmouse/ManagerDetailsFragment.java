package pk.contender.earmouse;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;

import java.io.IOException;
import java.io.InputStreamReader;

/**
 * Implements a detailed view and installation option of user-selected Module in {@link pk.contender.earmouse.ModuleManagerActivity}
 *
 * @author Paul Klinkenberg <pklinken.development@gmail.com>
 */
public class ManagerDetailsFragment extends Fragment {

    /* SharedPreferences constants */
    private static final String PREFERENCES_MANAGERDETAILSFRAGMENT_ID = "preferences_ManagerDetailsFragment_id";

    private Context mCtx;
    /** The user-selected Module */
    private Module mod = null;
    /** ID of the selected Module, used for fetching and storing
     * @see Module#id */
    private int id = -1;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_managerdetail, container, false);

        mCtx = getActivity();
        if(mCtx == null)
            Log.d("DEBUG", "Context is null in ManagerDetailsFragment onCreate()");

        return view;
    }

    /**
     * Restore fragment state.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if(getActivity().getClass() != ManagerDetailActivity.class)
        { // When running on a tablet restore the saved ID, on a handheld the activity will be reconstructed from the Intent.
            SharedPreferences settings = mCtx.getSharedPreferences(Main.PREFS_NAME, Activity.MODE_PRIVATE);
            id = settings.getInt(PREFERENCES_MANAGERDETAILSFRAGMENT_ID, -1);
            // TODO: Would refetch a loaded module on screen rotate when saving the state would be sufficient.
            update();
        }
    }

    /**
     * Save fragment state.
     */
    @Override
    public void onPause() {
        super.onPause();
        SharedPreferences settings = mCtx.getSharedPreferences(Main.PREFS_NAME, Activity.MODE_PRIVATE);
        settings.edit().putInt(PREFERENCES_MANAGERDETAILSFRAGMENT_ID, id).apply();
    }

    void setId(int id) {
        this.id = id;
    }

    /**
     * Update UI, either sets the UI to the empty UI or fetches the selected Module from the server.
     */
    void update() {
        if(id < 0) {
            setEmpty();
        } else {
            new FetchModuleJsonFromServer().execute();
        }
    }

    /**
     * Installs the selected Module. After installation displays a Toast message and tells {@link pk.contender.earmouse.ModuleManagerActivity}
     * to update itself.
     * @param view The View that received the click event
     */
    public void onButtonClick(@SuppressWarnings("UnusedParameters") View view) {
        if(mod != null){
            // User request to install this module
            if(mod.writeModuleToJson()) {
                Toast toast = Toast.makeText(mCtx, mCtx.getString(R.string.toast_module_installed), Toast.LENGTH_LONG);
                toast.show();
                // FIXME: Should check for null here, apparently static isn't so holy.
                for(Module mod : ModuleManagerActivity.shownModuleList) {
                    if(mod.getId() == id) {
                        ModuleManagerActivity.mAdapter.remove(mod);
                        ModuleManagerActivity.mAdapter.notifyDataSetChanged();
                        break;
                    }
                }
                mod = null;
                id = -1;
                //update();
            } else {
                Toast toast = Toast.makeText(mCtx, mCtx.getString(R.string.toast_error_installing_module), Toast.LENGTH_LONG);
                toast.show();
            }
        }
    }

    /**
     * Set the UI to only display a 'no module selected' message
     */
    void setEmpty() {
        TextView titleView = (TextView) getActivity().findViewById(R.id.module_title);
        TextView descriptionView = (TextView) getActivity().findViewById(R.id.module_description);
        View divider = getActivity().findViewById(R.id.button_divider);
        Button button = (Button) getActivity().findViewById(R.id.manager_button);
        TextView messageView = (TextView) getActivity().findViewById(R.id.message_text);

        if(titleView != null) {
            titleView.setText("");
            titleView.setVisibility(View.GONE);
        }
        if(descriptionView != null) {
            descriptionView.setText("");
            descriptionView.setVisibility(View.GONE);
        }
        if(divider != null) {
            divider.setVisibility(View.GONE);
        }
        if(button != null) {
            button.setText("");
            button.setVisibility(View.GONE);
        }

        if(messageView != null)
            messageView.setVisibility(View.VISIBLE);

        mod = null;
    }

    /**
     * Set the UI to display the standard layout.
     */
    void setNotEmpty() {

        TextView titleView = (TextView) getActivity().findViewById(R.id.module_title);
        TextView descriptionView = (TextView) getActivity().findViewById(R.id.module_description);
        View divider = getActivity().findViewById(R.id.button_divider);
        Button button = (Button) getActivity().findViewById(R.id.manager_button);
        TextView messageView = (TextView) getActivity().findViewById(R.id.message_text);

        if(titleView != null) {
            titleView.setText("");
            titleView.setVisibility(View.VISIBLE);
        }
        if(descriptionView != null) {
            descriptionView.setText("");
            descriptionView.setVisibility(View.VISIBLE);
        }
        if(divider != null) {
            divider.setVisibility(View.VISIBLE);
        }
        if(button != null) {
            button.setText("");
            button.setVisibility(View.VISIBLE);
        }

        if(messageView != null)
            messageView.setVisibility(View.GONE);

    }


    /**
     * Contacts a remote host, fetches the Module JSON for {@link ManagerDetailsFragment#id},
     * loads its data and sets up the UI.
     */
    private class FetchModuleJsonFromServer extends AsyncTask<Void, Void, Module> {

        private AndroidHttpClient httpClient = null;
        private Context mCtx;

        @Override
        protected void onPostExecute(Module result) {

            if(httpClient != null)
                httpClient.close();

            if(result == null) {
                Toast toast = Toast.makeText(mCtx, mCtx.getResources().getText(R.string.http_received_empty), Toast.LENGTH_LONG);
                toast.show();
                return;
            }

            mod = result;
            setNotEmpty();

            Activity callingActivity = getActivity();

            if(callingActivity != null) {
                // Can't use mCtx here as Activity may be destroyed once we reach this point
                TextView titleView = (TextView) callingActivity.findViewById(R.id.module_title);
                TextView descriptionView = (TextView) callingActivity.findViewById(R.id.module_description);
                if (titleView != null)
                    titleView.setText(mod.getTitle());
                if (descriptionView != null)
                    descriptionView.setText(mod.getDescription());

                Button button = (Button) callingActivity.findViewById(R.id.manager_button);
                if (button != null) {
                    button.setText(callingActivity.getResources().getText(R.string.manager_install_button));
                } else
                    Log.d("DEBUG", "Button is null");
            } else
                Log.d("DEBUG", "callingActivity is null in onPostExecute in FetchModuleJsonFromServer");
        }

        @Override
        protected void onCancelled(Module result) {
            // Task was cancelled so we cannot display the module, end activity and display a Toast message
            if(httpClient != null)
                httpClient.close();

            Activity callingActivity = getActivity();

            if(callingActivity != null) {
                // Can't use mCtx here as Activity may be destroyed once we reach this point
                Toast toast = Toast.makeText(callingActivity, mCtx.getResources().getText(R.string.http_error), Toast.LENGTH_LONG);
                toast.show();
                id = -1;
                setEmpty();
            }
        }

        // TODO: Set up a progressbar to display while contacting a server.
        @Override
        protected Module doInBackground(Void... params) {
            httpClient = AndroidHttpClient.newInstance("Earmouse/" + Main.VERSION);
            HttpHost host = new HttpHost(Main.SERVER_HOST, Main.SERVER_PORT);
            String localizedModulePath = Main.SERVER_PATH + "module" + Main.getLocaleSuffix() +
                    "_" + id + ".json";
            BasicHttpRequest request = new BasicHttpRequest("GET", localizedModulePath);

            HttpResponse response;
            try {
                response = httpClient.execute(host, request);
            } catch (IOException e) {
                e.printStackTrace();
                cancel(false);
                return null;
            }

            HttpEntity entity;

            if(response != null) {
                entity = response.getEntity();
            } else {
                cancel(false);
                return null;
            }
            if(entity != null) {
                Log.d("DEBUG", "entity.getContentLength() returns " + entity.getContentLength());
            } else {
                cancel(false);
                return null;
            }

            InputStreamReader reader;
            try {
                reader = new InputStreamReader(entity.getContent());
            } catch (IllegalStateException | IOException e) {
                e.printStackTrace();
                cancel(false);
                return null;
            }

            Module result = new Module(mCtx, reader);
            try {
                reader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }

            return result;
        }

        @Override
        protected void onPreExecute() {
            mCtx = getActivity();
        }
    }

}
