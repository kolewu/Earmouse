package pk.contender.earmouse;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.text.Html;
import android.text.Spanned;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.ActionMode;
import android.view.ContextMenu;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AbsListView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;


/**
 * Main Activity class, starting point for the App and implementation of
 * the Main activity.
 *
 * Also has code that will install a given set of Modules on the first
 * launch of the app.
 *
 * @author Paul Klinkenberg <pklinken.development@gmail.com>
 */
public class Main extends Activity implements ModuleListFragment.OnModuleSelectedListener,
        ButtonGridFragment.AnswerSelectedListener,
        ButtonGridFragment.PracticeModeToggleListener,
        ConfirmDeleteDialog.ConfirmDeleteDialogListener,
        ConfirmResetDialog.ConfirmResetDialogListener {

    // TODO: Remember scroll position

    /** Version string, only used in About dialog. Should always match version in Manifest.
     */
    public static final String VERSION = "v1.2";

    /** The default server address to use for fetching remote data */
    public static final String SERVER_HOST = "pklinken.github.io";
    /** The default server port to use for fetching remote data */
    public static final int SERVER_PORT = 80;
    /** The default path on the server where the data can be found */
    public static final String SERVER_PATH = "/Earmouse/Earmouse_localized/";
    /** The full default url */
    public static final String SERVER_URL = "http://" + SERVER_HOST + ":" + SERVER_PORT + SERVER_PATH;
    /** Available locales */
    public static final String[] SUPPORTED_LOCALES = { "de" };

    /** Set to true to enable StrictMode testing */
    private static final boolean DEVELOPER_MODE = false;

    /** The index of the currently selected entry in the ModuleListFragment */
    //private int selectionIndex;

    /* SharedPreferences constants */
    private static final String PREFS_FIRSTLAUNCH = "prefs_firstlaunch";
    public static final String PREFS_FASTPLAY = "prefs_fastplay";
    //private static final String PREFS_SELECTIONINDEX = "prefs_main_selectionindex";
    public static final String PREFS_NAME = "EarmousePrefs";
    private static final String PREFS_USE_CUSTOM_HOST = "prefs_custom_host_toggle";
    private static final String PREFS_CUSTOM_HOSTNAME = "prefs_custom_host";
    private static final String PREFS_CUSTOM_PATH = "prefs_custom_path";
    private static final String PREFS_CUSTOM_PORT = "prefs_custom_port";

    /**
     * reference to the Activity's ActionMode, is null if the Activity is not in ActionMode
     */
    private ActionMode mActionMode;
    /**
     * when in ActionMode, this List contains all the Modules the user currently has
     * selected.
     */
    private List<Module> selection = null;
    /**
     * Lists all the Modules currently installed, used by the Main Activity's ListView
     * adapter.
     */
    private static List<Module> mModules;
    /**
     * Adapter used by the Main Activity's ListView.
     */
    private ModuleListAdapter mAdapter;
    /**
     * Dialog currently being shown or null if none.
     * Only used for the About dialog, not the ActionMode DialogFragments
     */
    private Dialog mDialog;

    /**
     * Loads saved state and preferences, installs default package of modules if this is the
     * first time the App runs, ties the volume controls to the media stream that we use
     * and sets up the context actionbar (CAB).
     * @param savedInstanceState the previously saved instance state, can be null.
     */
    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        if (DEVELOPER_MODE) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectDiskReads()
                    .detectDiskWrites()
                    .detectNetwork()   // or .detectAll() for all detectable problems
                    .penaltyLog()
                    .penaltyFlashScreen()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        super.onCreate(savedInstanceState);

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean firstLaunch = settings.getBoolean(PREFS_FIRSTLAUNCH, true);
        //selectionIndex = settings.getInt(PREFS_SELECTIONINDEX, -2);

        if(firstLaunch) {
            // This is the first time the App runs, install the default modules and set up empty fragment view
            Log.d("DEBUG", "First launch of app");
            installDefaultModules();
            settings.edit().putBoolean(PREFS_FIRSTLAUNCH, false).apply();
        }

        refreshModuleList(this);

        setContentView(R.layout.activity_main);

        ActionBar actionBar = getActionBar();
        if(actionBar != null) // We do use the UP navigation, just not in the Main activity
            actionBar.setDisplayHomeAsUpEnabled(false);

        // Set the in-app volume control to always control the stream we use for playback.
        setVolumeControlStream(AudioManager.STREAM_MUSIC);

        // Set up the Context ActionMode and the mAdapter field
        ListView lv = null;
        ModuleListFragment moduleListFragment = (ModuleListFragment) getFragmentManager().findFragmentById(R.id.fragment_modulelist);
        if(moduleListFragment != null){
            lv = moduleListFragment.getListView();
            mAdapter = (ModuleListAdapter) moduleListFragment.getListAdapter();
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
                        //noinspection SuspiciousMethodCalls
                        selection.remove(finalLv.getItemAtPosition(position));

                    if (selection.size() > 0)
                        mode.setTitle(selection.size() + " " + getString(R.string.cab_selected));
                    else
                        mode.setTitle("");
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    // Respond to clicks on the actions in the CAB
                    switch (item.getItemId()) {
                        case R.id.main_ctx_delete:
                            DialogFragment dialog = new ConfirmDeleteDialog();
                            dialog.show(getFragmentManager(), "confirmDeleteDialog");
                            return true;
                        case R.id.main_ctx_reset:
                            DialogFragment dialog2 = new ConfirmResetDialog();
                            dialog2.show(getFragmentManager(), "confirmResetDialog");
                            return true;
                        default:
                            return false;
                    }
                }

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    // Inflate the menu for the CAB
                    MenuInflater inflater = mode.getMenuInflater();
                    inflater.inflate(R.menu.main_context, menu);
                    mActionMode = mode;
                    selection = new ArrayList<>();
                    if(savedInstanceState != null && savedInstanceState.getBoolean("isInActionMode")) {
                        // If there is a saved state and we were in ActionMode try to restore the previous
                        // selection. This happens e.g. when the screen is rotated in ActionMode.
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
                    // Here you can make any necessary updates to the activity when
                    // the CAB is removed. By default, selected items are deselected/unchecked.
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

    /**
     * Close any open Dialogs to prevent window leak.
     * Does not appear to be necessary for ActionMode dialogs.
     */
    @Override
    public void onPause() {
        super.onPause();
        if(mDialog != null)
            mDialog.dismiss();
        //SharedPreferences settings = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        //settings.edit().putInt(PREFS_SELECTIONINDEX, selectionIndex).apply();
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    /**
     * Implements the Manage and About Actionbar options.
     * @param item Menu item that was selected
     * @return False to allow normal menu processing to proceed, true to consume it here.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        switch(id) {
            case R.id.action_manage:
                Intent intent = new Intent(getApplicationContext(), ModuleManagerActivity.class);
                startActivity(intent);
                return true;
            case R.id.action_about:
                String text = String.format(getResources().getString(R.string.about_message), VERSION);
                CharSequence styledText = Html.fromHtml(text);
                final AlertDialog d = new AlertDialog.Builder(this)
                        .setTitle(R.string.about_title)
                        .setMessage(styledText)
                        .setCancelable(true)

                        .create();
                mDialog = d;
                d.show();

                // Make text a bit smaller
                TextView dialogView = (TextView) d.findViewById(android.R.id.message);
                dialogView.setTextSize(14);

                // ??
                ((TextView) d.findViewById(android.R.id.message)).setMovementMethod(LinkMovementMethod.getInstance());

                return true;
            case R.id.action_prefs:
                intent = new Intent();
                intent.setClass(getApplicationContext(), SettingsActivity.class);
                startActivity(intent);
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Listener for the attached ListView, handles item selection.
     * Depending on the layout, either starts an {@link pk.contender.earmouse.ExerciseActivity} Intent
     * with the position of the clicked item or updates the {@link pk.contender.earmouse.ExerciseFragment }
     * to display the item at position.
     * @param position Position of the list item that was clicked.
     */
    @Override
    public void onModuleSelected(View view, int position) {

        ExerciseFragment fragment = (ExerciseFragment) getFragmentManager()
                .findFragmentById(R.id.fragment_exercise);
        if (fragment == null || !fragment.isInLayout()) {
            // fragment unavailable, launch new activity.
            Intent intent = new Intent(getApplicationContext(), ExerciseActivity.class);
            intent.putExtra(ExerciseActivity.EXTRA_POSITION, position);
            SharedPreferences settings = getSharedPreferences(Main.PREFS_NAME, Activity.MODE_PRIVATE);
            settings.edit().putBoolean(ExerciseFragment.PREFERENCES_ISFRESHINTENT, true).apply();
            startActivity(intent);
        } else {
            // fragment available, do our stuff in here.
            if (fragment.getModuleIndex() != position) {
                //selectionIndex = position;
                fragment.setModule(position);
            }
        }
    }


    /**
     * Loads all the locally installed modules, and returns them as a sorted list.
     *
     * @param ctx The context used for the File functions and the Module constructor.
     * @return A List<Module> of all the modules locally installed on the device,
     * can be empty.
     */
    static private List<Module> loadModulesList(Context ctx) {

        List<Module> moduleList = new ArrayList<>();
        File currentDir = ctx.getDir("files", MODE_PRIVATE);
        FilenameFilter moduleFilter = new FilenameFilter() {

            @Override
            public boolean accept(File dir, String filename) {
                return filename.startsWith("module" + getLocaleSuffix());
            }
        };
        File moduleFileList [] = currentDir.listFiles(moduleFilter);

        for (File moduleFile : moduleFileList) {
            moduleList.add(new Module(ctx, moduleFile));
        }

        Collections.sort(moduleList);

        return moduleList;
    }

    /**
     * Refreshes {@link Main#mModules} to contain all the locally installed modules.
     * Invalidates the current adapter used for the ListView in {@link pk.contender.earmouse.Main}.
     */
    static public void refreshModuleList(Context ctx) {
        mModules = loadModulesList(ctx);
    }


    /**
     * Returns the value of {@link Main#mModules}.
     * @return the value of {@link Main#mModules}.
     */
    static public List<Module> getModuleList() {
        return mModules;
    }

    /**
     * Returns the module with given id or null if not found
     * @param id
     * @return found module or null
     */
    @Nullable
    static public Module getModuleById(int id) {
        for(Module mod : mModules) {
            if (mod.getId() == id) {
                return mod;
            }
        }
        return null;
    }

    /**
     * Install the default module_*.json files from the assets.
     * Called only the first time the application runs to provide the user
     * with a basic selection of Modules.
     */
    private void installDefaultModules() {
        AssetManager assetMan = getAssets();
        String localizedAssetDir = "modules" + getLocaleSuffix();

        try {
            String [] assetList = assetMan.list(localizedAssetDir);

            File currentDir = getDir("files", Context.MODE_PRIVATE);
            for(String item : assetList) {
                InputStream in = assetMan.open(localizedAssetDir + "/" + item, AssetManager.ACCESS_BUFFER);
                File outputFile = new File(currentDir, item);
                FileOutputStream out = new FileOutputStream(outputFile);

                byte [] buf = new byte[1024];
                int len;
                while((len = in.read(buf)) > 0)
                    out.write(buf, 0, len);
                in.close();
                out.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(this,"Error loading default modules", Toast.LENGTH_LONG).show();
        }
    }

    /**
     * Defers click event on Play button to the ExerciseFragment (tablets only)
     * @param view The view that was clicked.
     */
    public void onClickPlay(View view) {
        ExerciseFragment fragment = (ExerciseFragment) getFragmentManager()
                .findFragmentById(R.id.fragment_exercise);
        if(fragment != null && fragment.isInLayout()) {
            fragment.onClickPlay(view);
        }
    }

    /**
     * Defers click event on the ButtonGrid to the ExerciseFragment (tablets only)
     * @param position Position of the button that was clicked.
     */
    @Override
    public void onAnswerSelected(int position) {
        ExerciseFragment fragment = (ExerciseFragment) getFragmentManager()
                .findFragmentById(R.id.fragment_exercise);
        if(fragment != null && fragment.isInLayout()) {
            fragment.onAnswerSelected(position);
            mAdapter.notifyDataSetChanged();
        }
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
        super.onCreateContextMenu(menu, v, menuInfo);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_context, menu);

    }

    /**
     * Handler for when user confirms to delete the selected modules.
     * Will purge the selected modules, ask {@link pk.contender.earmouse.ExerciseFragment} to display the
     * module at index 0 and display a Toast indicating the successful removal.
     * @param dialog The dialog that was clicked will be passed into the method.
     */
    @Override
    public void onDeleteConfirm(DialogFragment dialog) {

        int deleted = 0;

        for (Module mod : selection) {
            if(mod != null) {
                mAdapter.remove(mod);
                mod.purgeModule();
                deleted++;
            }
        }

        //selectionIndex = 0;
        ExerciseFragment fragment = (ExerciseFragment) getFragmentManager()
                .findFragmentById(R.id.fragment_exercise);
        if (fragment != null && fragment.isInLayout()) {
            fragment.setModule(0);
        }

        //mAdapter.notifyDataSetChanged();

        Resources res = getResources();
        String s = deleted + " " + res.getQuantityString(R.plurals.plural_module, deleted) + " " + getString(R.string.cab_deleted);
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();

        mActionMode.finish(); // Action picked, so close the CAB
    }

    /**
     * Handler for when the user aborts to delete the selected modules.
     * @param dialog The dialog that was clicked will be passed into the method.
     */
    @Override
    public void onDeleteAbort(DialogFragment dialog) {
        mActionMode.finish(); // Action picked, so close the CAB
    }

    /**
     * Handler for when user confirms to reset the selected modules.
     * Resets the history of the selected modules and updates {@link pk.contender.earmouse.ModuleListFragment}
     * and {@link pk.contender.earmouse.ExerciseFragment} and displays a Toast indicating the successful history reset.
     * @param dialog The dialog that was clicked will be passed into the method.
     */
    @Override
    public void onResetConfirm(DialogFragment dialog) {

        int reset = 0;

        for(Module mod : selection) {
            if(mod != null) {
                mod.resetStats();
                reset++;
            }
        }

        mAdapter.notifyDataSetChanged();

        ExerciseFragment fragment = (ExerciseFragment) getFragmentManager().findFragmentById(R.id.fragment_exercise);
        if (fragment != null && fragment.isInLayout()) {
            //fragment.setModule(selectionIndex);
            fragment.updateFeedbackStatistics();
        }

        Resources res = getResources();
        String s = reset + " " + res.getQuantityString(R.plurals.plural_module, reset) + " " + getString(R.string.cab_reset);
        Toast.makeText(this, s, Toast.LENGTH_LONG).show();

        mActionMode.finish(); // Action picked, so close the CAB
    }

    /**
     * Test whether the default Locale is supported by the lesson material
     * @return True if the default locale is supported by the lesson material
     */
    public static boolean isDefaultLocaleSupported() {

        for(String locale : SUPPORTED_LOCALES) {
            if(Locale.getDefault().getLanguage().equals(locale))
                return true;
        }

        return false;
    }

    /**
     * Return a string that can be inserted into a filename to match the default Locale
     * @return A string to match the default locale or "" if the default locale is not supported
     */
    public static String getLocaleSuffix() {

        if(isDefaultLocaleSupported()) {
            return "_" + Locale.getDefault().getLanguage();
        } else {
            return "_en";
        }
    }

    /**
     * Handler for when the user aborts to reset the selected modules.
     * @param dialog The dialog that was clicked will be passed into the method.
     */
    @Override
    public void onResetAbort(DialogFragment dialog) {
        mActionMode.finish(); // Action picked, so close the CAB
    }

    @Override
    public void onPracticeModeToggle() {
        ExerciseFragment fragment = (ExerciseFragment) getFragmentManager().findFragmentById(R.id.fragment_exercise);
        if(fragment != null && fragment.isInLayout()) {
            fragment.onPracticeModeToggle();
        }
    }

    /**
     * Returns an URL used for installing new Modules based on the user's preferences.
     * @param ctx The application context required to access the SharedPreferences
     * @return The default or user-customized URL for installing new Modules
     */
    public static String generateModuleUrl(Context ctx) {
        SharedPreferences s = ctx.getSharedPreferences(Main.PREFS_NAME, MODE_PRIVATE);

        if(s.getBoolean(Main.PREFS_USE_CUSTOM_HOST, false)) {
            String hostname, path, port;
            hostname = s.getString(Main.PREFS_CUSTOM_HOSTNAME, SERVER_HOST);
            path = s.getString(Main.PREFS_CUSTOM_PATH, SERVER_PATH);
            port = s.getString(Main.PREFS_CUSTOM_PORT, String.valueOf(SERVER_PORT));
            return "http://" + hostname + ":" + port + path;
        } else {
            return "http://" + SERVER_HOST + ":" + SERVER_PORT + SERVER_PATH;
        }
    }
}
