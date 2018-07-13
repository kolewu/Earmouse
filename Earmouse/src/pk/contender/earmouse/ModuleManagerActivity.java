package pk.contender.earmouse;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.net.http.AndroidHttpClient;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.ActionMode;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Toast;

import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.HttpResponse;
import org.apache.http.message.BasicHttpRequest;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Activity for displaying a list of Modules available for remote installation and installing these modules either one by one or in a batch.
 * @author Paul Klinkenberg <pklinken.development@gmail.com>
 */
public class ModuleManagerActivity extends Activity implements ManagerListFragment.OnModuleSelectedListener {

    /* SharedPreferences constants */
    private static final String PREFERENCES_MODULEMANAGERACTIVITY_SELECTIONPOSITION = "preferences_ModuleManagerActivity_selectionPosition";
    /** The index of the current selection in the ListView */
    private int selectionPosition = -1;

    /**
     * when in ActionMode, this List contains all the Modules the user currently has
     * selected.
     */
    private List<Module> selection = null;
    /**
     * reference to the Activity's ActionMode, is null if the Activity is not in ActionMode
     */
    private ActionMode mActionMode;
    /**
     * Adapter used by this Activity's ListView.
     */
    public static ModuleListAdapter mAdapter;

    /** List of currently shown Modules in the ListView,  */
    public static final List<Module> shownModuleList = new ArrayList<>();

    /**
     * Loads saved state and preferences and sets up the context actionbar (CAB).
     * Fetches a list of available modules from the server.
     * @param savedInstanceState the previously saved instance state, can be null.
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module_manager);

        SharedPreferences settings = getSharedPreferences(Main.PREFS_NAME, Activity.MODE_PRIVATE);
        selectionPosition = settings.getInt(PREFERENCES_MODULEMANAGERACTIVITY_SELECTIONPOSITION, -1);

        ListView lv = null;
        ManagerListFragment managerListFragment = (ManagerListFragment) getFragmentManager().findFragmentById(R.id.fragmentModuleList);
        if(managerListFragment != null){
            lv = managerListFragment.getListView();
            mAdapter = (ModuleListAdapter) managerListFragment.getListAdapter();
            managerListFragment.setEmptyText(getString(R.string.list_no_modules_available));
            managerListFragment.setListShown(false);
            new FetchListJsonFromServer().execute();

        }
        if(lv != null) {
            lv.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
            final ListView finalLv = lv;
            lv.setMultiChoiceModeListener(new AbsListView.MultiChoiceModeListener() {

                @Override
                public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
                    if (checked)
                        selection.add((Module) finalLv.getItemAtPosition(position));
                    else
                        //noinspection RedundantCast
                        selection.remove((Module)finalLv.getItemAtPosition(position));

                    if (selection.size() > 0)
                        mode.setTitle(selection.size() + " " + getString(R.string.cab_selected));
                    else
                        mode.setTitle("");
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    // Respond to clicks on the actions in the CAB
                    switch (item.getItemId()) {
                        case R.id.manager_ctx_install:
                            new fetchAndInstallSelection().execute();
                            mode.finish(); // Action picked, so close the CAB
                            return true;
                        default:
                            return false;
                    }
                }

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    // Inflate the menu for the CAB
                    MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(R.menu.manager_context, menu);
                    mActionMode = mode;
                    selection = new ArrayList<>();
                    // disable Install button, it's ambiguous and screws up the UI
                    Button installButton = (Button) findViewById(R.id.manager_button);
                    if(installButton != null) {
                        installButton.animate().setDuration(1000).alpha((float) 0.2);
                        installButton.setClickable(false);
                    }
                    if(savedInstanceState != null && savedInstanceState.getBoolean("isInActionMode")) {
                        // If there is a saved state and we were in ActionMode try to restore the previous
                        // selection. This happens when the screen is rotated in ActionMode.
                        for (int i = 0;i < finalLv.getCount(); i++) {
                            if(finalLv.isItemChecked(i)) {
                                Module mod = (Module) finalLv.getItemAtPosition(i);
                                if (mod != null)
                                    selection.add(mod);
                            }
                        }
                        mode.setTitle(selection.size() + " " + getString(R.string.cab_selected));
                    }
                    return true;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    // Enable install button when we exit CAB
                    Button installButton = (Button) findViewById(R.id.manager_button);
                    if(installButton != null) {
                        installButton.setClickable(true);
                        installButton.animate().setDuration(1000).alpha((float) 1.0);
                    }
                    mActionMode = null;

                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    // Here you can perform updates to the CAB due to
                    // an invalidate() request
                    return false;
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        SharedPreferences settings = getSharedPreferences(Main.PREFS_NAME, Activity.MODE_PRIVATE);
        settings.edit().putInt(PREFERENCES_MODULEMANAGERACTIVITY_SELECTIONPOSITION, selectionPosition).apply();
    }

    @Override
    protected void onSaveInstanceState(@SuppressWarnings("NullableProblems") Bundle outState) {

        // If we are in ActionMode save this state
        if(outState != null)
            outState.putBoolean("isInActionMode", (mActionMode != null));
        // FIXME: Shouldn't this go in sharedprefs
        //noinspection ConstantConditions
        super.onSaveInstanceState(outState);
    }

    /**
     * Shows the selected Module in detail, either by updating {@link pk.contender.earmouse.ManagerDetailsFragment} if available or otherwise
     * starting an Intent for {@link pk.contender.earmouse.ManagerDetailActivity}
     * @param position The position in the ListView of the user selection
     */
    @Override
    public void onModuleSelected(int position) {
        ManagerDetailsFragment detailFragment = (ManagerDetailsFragment) getFragmentManager().findFragmentById(R.id.fragmentDetailManager);
        if (detailFragment != null && detailFragment.isInLayout()) {
            // fragment available, do our stuff in here.
            if(position == selectionPosition)
                return;
            ManagerListFragment managerListFragment = (ManagerListFragment) getFragmentManager().findFragmentById(R.id.fragmentModuleList);
            if(managerListFragment != null) {
                detailFragment.setId(shownModuleList.get(position).getId());
                selectionPosition = position;
                detailFragment.update();
            }
        } else {
            // fragment unavailable, launch new activity.
            Intent intent = new Intent(getApplicationContext(), ManagerDetailActivity.class);
            ManagerListFragment managerListFragment = (ManagerListFragment) getFragmentManager().findFragmentById(R.id.fragmentModuleList);
            if(managerListFragment != null){
                intent.putExtra(ManagerDetailActivity.EXTRA_MODULE_ID, shownModuleList.get(position).getId());
                startActivity(intent);
            } else
                Log.d("DEBUG", "managerListFragment == null");
        }
    }

    /**
     * Defer onClick() event to {@link pk.contender.earmouse.ManagerDetailsFragment} (tablet only)
     * @param v The view that was clicked
     */
    public void onButtonClick(View v) {
        ManagerDetailsFragment detailFragment = (ManagerDetailsFragment) getFragmentManager().findFragmentById(R.id.fragmentDetailManager);
        if(detailFragment != null && detailFragment.isInLayout()) {
            detailFragment.onButtonClick(v);
            selectionPosition = -1;
            detailFragment.update();
        } else
            Log.d("DEBUG", "Could not relay click event");
    }

    /**
     * Downloads and installs all the modules in {@link #selection}
     */
    private class fetchAndInstallSelection extends AsyncTask<Void, Void, Integer> {

        private Context mCtx;
        private List<Module> removals;

        @Override
        protected Integer doInBackground(Void... params) {

            Integer installed = 0;

            for(Module mod : selection) {
                if(mod == null)
                    continue;

                HttpURLConnection urlConn = null;
                URL url = null;
                String localizedModulePath = "module" + (Main.getLocaleSuffix().equals("") ? "_" : Main.getLocaleSuffix()) +
                        "_" + mod.getId() + ".json";

                try {
                    url = new URL(Main.generateModuleUrl(getApplicationContext()) + localizedModulePath);
                } catch (MalformedURLException e) {
                    e.printStackTrace();
                    cancel(false);
                }

                try {
                    assert url != null;
                    urlConn = (HttpURLConnection) url.openConnection();
                    // Limit the time the user sits and waits for a malformed custom URL to timeout
                    urlConn.setConnectTimeout(5000);
                } catch (IOException e) {
                    e.printStackTrace();
                    cancel(false);
                }

                InputStreamReader reader = null;
                try {
                    assert urlConn != null;
                    reader = new InputStreamReader(urlConn.getInputStream());
                    Module result = new Module(mCtx, reader);

                    // TODO: If an error occurred during the read, this module should not be written to disk ...
                    if(result.writeModuleToJson()) {
                        installed++;
                        for (Module listMod : shownModuleList) {
                            if (listMod.getId() == mod.getId()) {
                                // We want to remove the items from the adapter straight away but have to do this on the UI thread.
                                removals.add(listMod);
                                break;
                            }
                        }
                    }
                    reader.close();
                } catch (IllegalStateException | IOException e) {
                    e.printStackTrace();
                    cancel(false);
                    return installed;
                } finally {
                    assert urlConn != null;
                    urlConn.disconnect();
                }


            }
            return installed;
        }

        @Override
        protected void onPreExecute() {

            mCtx = getApplicationContext();
            removals = new ArrayList<>(selection.size());
        }

        @Override
        protected void onPostExecute(Integer installed) {

            // update ListView
            for(Module mod : removals)
                mAdapter.remove(mod);

            // FIXME: Returning to main before installation is completed will not update ListView in Main with new modules.

            // Inform user of amount of successfully installed modules
            Resources res = mCtx.getResources();
            String s = installed + " " + res.getQuantityString(R.plurals.plural_module, installed) + " " + getString(R.string.cab_installed);
            Toast toast = Toast.makeText(mCtx, s, Toast.LENGTH_LONG);
            toast.show();

            // update detailfragment
            ManagerDetailsFragment detailFragment = (ManagerDetailsFragment) getFragmentManager().findFragmentById(R.id.fragmentDetailManager);
            if(detailFragment != null && detailFragment.isInLayout()) {
                detailFragment.setId(-1); // this will set detailfragment to empty view
                detailFragment.update();
            }
        }

        @Override
        protected void onCancelled(Integer integer) {
            Toast toast = Toast.makeText(mCtx, mCtx.getString(R.string.toast_error_installing_module), Toast.LENGTH_LONG);
            toast.show();
        }
    }

    /**
     * Downloads a list of available modules and displays its contents, minus the modules that are already installed,
     * in {@link pk.contender.earmouse.ManagerListFragment}
     * @author Paul Klinkenberg <pklinken.development@gmail.com>
     */
    private class FetchListJsonFromServer extends AsyncTask<Void, Void, List<Module>> {

        //private AndroidHttpClient httpClient = null;
        private Context mCtx = null;

        @Override
        protected void onPreExecute() {
            mCtx = getApplicationContext();
        }

        @Override
        protected void onPostExecute(List<Module> result) {

            if(result == null) {
                Toast toast = Toast.makeText(mCtx, mCtx.getResources().getText(R.string.http_received_empty), Toast.LENGTH_LONG);
                toast.show();
                return;
            }

            List<Integer> idsToRemove = new ArrayList<>();

            for(Module mod : shownModuleList)
                idsToRemove.add(mod.getId());
            // idsToRemove now contains all the Module IDS that are already shown in the list so need not be added.
            Module modLocal;
            for(Module mod : result) {
                if (!idsToRemove.contains(mod.getId())) {
                    if ((modLocal = Main.getModuleById(mod.getId())) == null || modLocal.getModuleVersion() < mod.getModuleVersion()) {
                        shownModuleList.add(mod);
                    }
                }
            }

            Collections.sort(shownModuleList);

            ManagerListFragment managerListFragment = (ManagerListFragment) getFragmentManager().findFragmentById(R.id.fragmentModuleList);
            if(managerListFragment != null){
                managerListFragment.setListShown(true);
            }
            mAdapter.notifyDataSetChanged();
        }

        @Override
        protected void onCancelled(List<Module> result) {
            // Task was cancelled as there was an error contacting server, show only an empty list and a Toast error message

            if(mCtx != null) {

                Toast toast = Toast.makeText(mCtx, mCtx.getResources().getText(R.string.http_error), Toast.LENGTH_LONG);
                toast.show();
                shownModuleList.clear(); // just show an empty list
                ManagerListFragment managerListFragment = (ManagerListFragment) getFragmentManager().findFragmentById(R.id.fragmentModuleList);
                if(managerListFragment != null){
                    managerListFragment.setListShown(true);
                }
                mAdapter.notifyDataSetChanged();
            }
            // else: Calling activity was destroyed before AsyncTask completed, do nothing.
        }

        @Override
        protected List<Module> doInBackground(Void... params) {

            HttpURLConnection urlConn = null;
            URL url = null;
            String localizedModulePath = "list" + Main.getLocaleSuffix() +  ".json";

            try {
                url = new URL(Main.generateModuleUrl(getApplicationContext()) + localizedModulePath);
            } catch (MalformedURLException e) {
                e.printStackTrace();
                cancel(false);
            }

            try {
                assert url != null;
                urlConn = (HttpURLConnection) url.openConnection();
                // Limit the time the user sits and waits for a malformed custom URL to timeout
                urlConn.setConnectTimeout(5000);
            } catch (IOException e) {
                e.printStackTrace();
                cancel(false);
            }

            InputStreamReader reader;
            List<Module> moduleList;

            try {
                assert urlConn != null;
                reader = new InputStreamReader(urlConn.getInputStream());
                moduleList = readListFromJson(reader);
            } catch (IllegalStateException | IOException e) {
                e.printStackTrace();
                cancel(false);
                return null;
            } finally {
                assert urlConn != null;
                urlConn.disconnect();
            }

            return moduleList;
        }

        /*
         * Note that the JSON names here are different, because the python variable naming convention ended up being the JSON naming
         * so shortDescription is short_description etc.
         */
        private List<Module> readListFromJson(InputStreamReader in) throws IOException {

            JsonReader reader = new JsonReader(in);
            List<Module> serverModuleList = new ArrayList<>();

            reader.beginArray();
            while (reader.hasNext()) {
                reader.beginObject();
                Module mod = new Module(mCtx);
                while(reader.hasNext()) {
                    String name = reader.nextName();
                    switch (name) {
                        case "module_id":
                            mod.setId(reader.nextInt());
                            break;
                        case "module_title":
                            mod.setTitle(reader.nextString());
                            break;
                        case "difficulty":
                            mod.setDifficulty(reader.nextInt());
                            break;
                        case "short_description":
                            mod.setShortDescription(reader.nextString());
                            break;
                        case "module_version":
                            mod.setModuleVersion(reader.nextInt());
                            break;
                        default:
                            reader.skipValue();
                            break;
                    }
                }
                serverModuleList.add(mod);
                reader.endObject();
            }
            reader.endArray();

            reader.close();
            in.close();

            Log.d("DEBUG", "serverModuleList.size() returns " + serverModuleList.size());

            return serverModuleList;
        }
    }
}
