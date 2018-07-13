package pk.contender.earmouse;

import android.app.Activity;
import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

/**
 * Implements a selected {@link pk.contender.earmouse.Module} by presenting the user with random exercises
 * from said {@link pk.contender.earmouse.Module} and processing the answers.
 *
 * @author Paul Klinkenberg <pklinken.development@gmail.com>
 */
public class ExerciseFragment extends Fragment {

    /**
     * State in which an exercise is prepared and has been played at least once, the buttons in the
     * ButtonGrid are enabled. Exercise can be played as often as the user wants.
     */
    private static final int EXERCISE_READY = 0;
    /**
     * State in which an exercise has been correctly answered and a click on the ButtonGrid or
     * Play button will load the next exercise.
     * Clicking on the Play button will, in addition to preparing the next exercise, also play
     * the media as soon as it is available.
     * @see pk.contender.earmouse.MediaFragment#requestPlayback()
     */
    private static final int EXERCISE_CONTINUE = 1;
    /**
     * State in which an exercise is prepared but has not been played, the buttons in the ButtonGrid
     * are disabled as we do not want the user to (inadvertently) give an answer before listening to the
     * exercise.
     */
    private static final int EXERCISE_READY_NOTPLAYED = 2;

    /* SharedPreferences constants */
    private static final String PREFERENCES_ISEMPTY = "preferences_isEmpty";
    private static final String PREFERENCES_MODINDEX = "preferences_modIndex";
    private static final String PREFERENCES_MODID = "preferences_modId";
    private static final String PREFERENCES_CURRENTEXERCISE = "preferences_currentExercise";
    private static final String PREFERENCES_EXERCISESTATE = "preferences_exerciseState";
    static final String PREFERENCES_ISFRESHINTENT = "preferences_isFreshIntent";
    static final String PREFERENCES_PRACTICEMODE = "preferences_practiceMode";

    /**
     * The current state.
     */
    private int exerciseState;
    /**
     * The {@link pk.contender.earmouse.Module} we are currently doing exercises from.
     */
    private Module mod = null;
    /**
     * Index of the {@link pk.contender.earmouse.Module} in {@link Main#mModules}
     */
    private int modIndex = -1;
    /**
     * ID of the {@link pk.contender.earmouse.Module} we are currently doing exercises from.
     */
    private int modId = -1;
    /**
     * The index of the {@link pk.contender.earmouse.Exercise} in {@link #mod} that we are currently doing.
     */
    private int currentExercise = -1;

    /**
     * If true the instance should request the mediaFragment to play the selected answer
     */
    private boolean practiceMode = false;

    /** Set to true if we are currently hiding the UI
     * The UI is hidden when there is no {@link pk.contender.earmouse.Module} selected. */
    private boolean isEmpty;

    private Activity mCtx;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_exercise, container, false);

        mCtx = getActivity();
        if(mCtx == null)
            Log.d("DEBUG", "Context is null in ExerciseFragment onCreateView()");

        return view;
    }

    /**
     * Initializes or restores the Fragment's state. If the parent activity was started from an {@link android.content.Intent}, set up the UI
     * from the given position value, otherwise restore the state the fragment was left in.
     */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SharedPreferences settings = mCtx.getSharedPreferences(Main.PREFS_NAME, Activity.MODE_PRIVATE);
        Bundle extras = mCtx.getIntent().getExtras();
        // Restore state
        practiceMode = settings.getBoolean(PREFERENCES_PRACTICEMODE, false);
        // If started from an Intent and isFreshIntent, set that up and set isFreshIntent to false.
        if(extras != null && settings.getBoolean(PREFERENCES_ISFRESHINTENT, false)) {
            int position = extras.getInt(ExerciseActivity.EXTRA_POSITION);
            setModule(position);
            settings.edit().putBoolean(PREFERENCES_ISFRESHINTENT, false).apply();
        } else {
            // If not started from a fresh intent, just restore the UI and fragments how we left them.
            isEmpty = settings.getBoolean(PREFERENCES_ISEMPTY, true);
            if (isEmpty)
                setModule(-1);
            else {
                exerciseState = settings.getInt(PREFERENCES_EXERCISESTATE, EXERCISE_READY);
                modId = settings.getInt(PREFERENCES_MODID, -1);
                if(modId < 0) {
                    setModule(-1);
                    return;
                }
                else {
                    for (Module m : Main.getModuleList()) {
                        if(m.getId() == modId) {
                            mod = m;
                            modIndex = Main.getModuleList().indexOf(mod);

                        }
                    }
                    currentExercise = settings.getInt(PREFERENCES_CURRENTEXERCISE, 0);
                    prepareExercise(false);
                }

                ModuleDetailsFragment detailFragment = (ModuleDetailsFragment) getFragmentManager().findFragmentById(R.id.moduledetail);
                if (detailFragment != null) {
                    detailFragment.setTitle(mod.getTitle());
                    detailFragment.setDescription(mod.getDescription());
                } else
                    Log.d("DEBUG", "ModuleDetailsFragment is null");


                updateFeedbackStatistics();
            }
        }
    }

    /**
     * Hide the main UI of the fragment and display a single TextView instead.
     * Used on tablets when there is no {@link pk.contender.earmouse.Module} selected, instead of showing an empty layout
     * we show this TextView saying 'No Module Selected'
     */
    void setEmpty() {

        View v;

        ModuleDetailsFragment detailFragment = (ModuleDetailsFragment) getFragmentManager().findFragmentById(R.id.moduledetail);
        if(detailFragment != null) {
            detailFragment.setTitle("");
            detailFragment.setDescription("");
            v = detailFragment.getView();
            if(v != null)
                v.setVisibility(View.GONE);
        } else
            Log.d("DEBUG", "ModuleDetailsFragment is null");

        ButtonGridFragment buttonFragment = (ButtonGridFragment) getFragmentManager().findFragmentById(R.id.buttongrid);
        if(buttonFragment != null) {
            buttonFragment.initGrid(null);
            v = buttonFragment.getView();
            if(v != null)
                v.setVisibility(View.GONE);
        } else
            Log.d("DEBUG", "ButtonGridFragment is null");

        FeedbackBarFragment feedbackFragment = (FeedbackBarFragment) getFragmentManager().findFragmentById(R.id.feedbackbar);
        if(feedbackFragment != null) {
            feedbackFragment.setEmpty();
            v = feedbackFragment.getView();
            if(v != null)
                v.setVisibility(View.GONE);
        } else {
            Log.d("DEBUG", "FeedbackBarFragment is null");
        }

        MediaFragment mediaFragment = (MediaFragment) getFragmentManager().findFragmentById(R.id.media);
        if(mediaFragment != null) {
            //mediaFragment.setEmpty();
            v = mediaFragment.getView();
            if(v != null)
                v.setVisibility(View.GONE);
        } else
            Log.d("DEBUG", "MediaFragment is null");

        View fragmentView = getView();
        if(fragmentView != null) {
            v = fragmentView.findViewById(R.id.feedbackbar_divider);
            if (v != null)
                v.setVisibility(View.GONE);
            v = fragmentView.findViewById(R.id.buttongrid_divider);
            if (v != null)
                v.setVisibility(View.GONE);
            v = fragmentView.findViewById(R.id.media_divider);
            if (v != null)
                v.setVisibility(View.GONE);

            v = fragmentView.findViewById(R.id.message_text);
            if(v != null)
                v.setVisibility(View.VISIBLE);
        }

        isEmpty = true;
    }

    /**
     * Unhide the main UI, does the exact opposite of setEmpty().
     */
    void setNotEmpty() {

        View v;

        ModuleDetailsFragment detailFragment = (ModuleDetailsFragment) getFragmentManager().findFragmentById(R.id.moduledetail);
        if(detailFragment != null) {
            v = detailFragment.getView();
            if(v != null)
                v.setVisibility(View.VISIBLE);
        } else
            Log.d("DEBUG", "ModuleDetailsFragment is null");

        ButtonGridFragment buttonFragment = (ButtonGridFragment) getFragmentManager().findFragmentById(R.id.buttongrid);
        if(buttonFragment != null) {
            v = buttonFragment.getView();
            if(v != null)
                v.setVisibility(View.VISIBLE);
        } else
            Log.d("DEBUG", "ButtonGridFragment is null");

        FeedbackBarFragment feedbackFragment = (FeedbackBarFragment) getFragmentManager().findFragmentById(R.id.feedbackbar);
        if(feedbackFragment != null) {
            v = feedbackFragment.getView();
            if(v != null)
                v.setVisibility(View.VISIBLE);
        } else {
            Log.d("DEBUG", "FeedbackBarFragment is null");
        }

        MediaFragment mediaFragment = (MediaFragment) getFragmentManager().findFragmentById(R.id.media);
        if(mediaFragment != null) {
            v = mediaFragment.getView();
            if(v != null)
                v.setVisibility(View.VISIBLE);
        } else
            Log.d("DEBUG", "MediaFragment is null");

        View fragmentView = getView();
        if(fragmentView != null) {
            v = fragmentView.findViewById(R.id.feedbackbar_divider);
            if (v != null)
                v.setVisibility(View.VISIBLE);
            v = fragmentView.findViewById(R.id.buttongrid_divider);
            if (v != null)
                v.setVisibility(View.VISIBLE);
            v = fragmentView.findViewById(R.id.media_divider);
            if (v != null)
                v.setVisibility(View.VISIBLE);

            v = fragmentView.findViewById(R.id.message_text);
            if(v != null)
                v.setVisibility(View.GONE);
        }

        isEmpty = false;
    }

    /**
     * Display the Module at position in {@link pk.contender.earmouse.Main#getModuleList()} <p>
     * If position is out of bounds set the UI to 'empty', otherwise load the selected
     * Module, set up the UI and prepare an exercise.
     * @param position The position of the Module in {@link pk.contender.earmouse.Main#getModuleList()} to display
     */
    public void setModule(int position) {

        List<Module> moduleList = Main.getModuleList();
        if(position >= moduleList.size() || position < 0) {
            mod = null;
            modIndex = -1;
            modId = -1;
            setEmpty();
            return;
        } else if (isEmpty)
            setNotEmpty();
        mod = moduleList.get(position);
        modIndex = position;
        modId = mod.getId();
        ModuleDetailsFragment detailFragment = (ModuleDetailsFragment) getFragmentManager().findFragmentById(R.id.moduledetail);
        if(detailFragment != null) {
            detailFragment.setTitle(mod.getTitle());
            detailFragment.setDescription(mod.getDescription());
        } else
            Log.d("DEBUG", "ModuleDetailsFragment is null");

        // Set up grid of buttons with the Module's answerList
        ButtonGridFragment buttonFragment = (ButtonGridFragment) getFragmentManager().findFragmentById(R.id.buttongrid);
        if(buttonFragment != null) {
            buttonFragment.initGrid(mod.getAnswerList());
        } else
            Log.d("DEBUG", "ButtonGridFragment is null");

        mod.refreshState();
        prepareExercise(false);
        updateFeedbackStatistics();
    }

    public int getModuleIndex() {
        return modIndex;
    }

    /**
     * Save complete state.
     */
    @Override
    public void onPause() {
        super.onPause();
        // Possibly superfluous to write to disk here.
        if(mod != null)
            mod.saveState();

        SharedPreferences settings = mCtx.getSharedPreferences(Main.PREFS_NAME, Activity.MODE_PRIVATE);
        settings.edit().putBoolean(PREFERENCES_ISEMPTY, isEmpty).putInt(PREFERENCES_MODINDEX, modIndex)
                .putInt(PREFERENCES_CURRENTEXERCISE, currentExercise).putInt(PREFERENCES_EXERCISESTATE, exerciseState)
                .putInt(PREFERENCES_MODID, modId).putBoolean(PREFERENCES_PRACTICEMODE, practiceMode).apply();
    }


    @Override
    public void onResume() {
        super.onResume();
        if(mod != null)
            mod.refreshState();
    }

    /**
     * Prepare an exercise for this activity.
     * <p>
     * This function will receive an Exercise index from the loaded Module and use that to:<br>
     * - Set up the ButtonGrid with the answers<br>
     * - Start the MediaFragment to prepare and load the required WAV file<br>
     * - Set up the FeedbackBarFragment to reflect the current state.
     */
    private void prepareExercise(boolean playNow){
        currentExercise = mod.getWeightedExerciseIndex();

        ButtonGridFragment buttonFragment = (ButtonGridFragment) getFragmentManager().findFragmentById(R.id.buttongrid);
        if(buttonFragment != null) {
            buttonFragment.resetGridButtonState();
        } else
            Log.d("DEBUG", "ButtonGridFragment is null");

        MediaFragment mediaFragment = (MediaFragment) getFragmentManager().findFragmentById(R.id.media);
        if(mediaFragment != null) {
            mediaFragment.prepareExercise(mod.getExercise(currentExercise), playNow);
        } else
            Log.d("DEBUG", "MediaFragment is null");

        setFeedbackText((String) this.getResources().getText(R.string.feedback_ready));
        exerciseState = EXERCISE_READY_NOTPLAYED;
    }

    /**
     * On receiving a click event on the Play button, relay it to the MediaFragment and:
     * If the current state is {@link #EXERCISE_READY_NOTPLAYED}, move the state to {@link #EXERCISE_READY}
     * If the current state is {@link #EXERCISE_CONTINUE}, prepare an exercise and request the MediaFragment
     * to play it as soon as it is ready, and set the state to {@link #EXERCISE_READY}
     *
     * @param view The parent view where the click event was received.
     */
    public void onClickPlay(@SuppressWarnings("UnusedParameters") View view) {
        MediaFragment mediaFragment = (MediaFragment) getFragmentManager().findFragmentById(R.id.media);
        if(mediaFragment != null) {
            if(exerciseState == EXERCISE_CONTINUE) {
                prepareExercise(true);
                //mediaFragment.requestPlayback();
                exerciseState = EXERCISE_READY;
            } else {
                mediaFragment.clickPlay();
                exerciseState = (exerciseState == EXERCISE_READY_NOTPLAYED ? EXERCISE_READY : exerciseState);
            }
        } else
            Log.d("DEBUG", "MediaFragment is null");
    }

    /**
     * Handler for click events in the ButtonGridFragment
     * <p>
     * Depending on the state of the Activity, handle user input, see code for details.
     *
     * @param position The position of the Button in the ButtonGridFragment that was clicked.
     */
    public void onAnswerSelected(int position) {

        if(practiceMode) {
            playSelectedAnswer(position);
        } else if(exerciseState == EXERCISE_READY) {
            // Ready to receive answer
            if(position == currentExercise) {
                // Correct answer, register with statistics, give UI feedback and change state.
                mod.registerAnswer(currentExercise, true);
                setFeedbackText((String) this.getResources().getText(R.string.feedback_correct));
                updateFeedbackStatistics();
                mod.saveState();
                exerciseState = EXERCISE_CONTINUE;
            } else {
                // Wrong answer, register with statistics and give UI feedback
                mod.registerAnswer(currentExercise, false);
                fadeButton(position);
                setFeedbackText((String) this.getResources().getText(R.string.feedback_incorrect));
                updateFeedbackStatistics();
                mod.saveState();
            }
        } else if(exerciseState == EXERCISE_CONTINUE) {
            // Prepare the next exercise
            prepareExercise(false);
        }
        // Activity not ready to receive answer events, discard input.
    }

    /**
     * In practice mode, when a user selects an answer it is to be played immediately by
     * the mediaFragment.
     * @param pos is the index of the button clicked on the grid, which translates directly
     *            to an Exercise returned by mod.getExercise(pos)
     */
    private void playSelectedAnswer(int pos) {
        // Get an exercise for the user selection
        Exercise ex = mod.getExercise(pos);
        MediaFragment mediaFragment = (MediaFragment) getFragmentManager().findFragmentById(R.id.media);
        if(mediaFragment != null) {
            mediaFragment.playPractice(ex);
        } else
            Log.d("DEBUG", "MediaFragment is null");
    }

    /**
     * Set the feedback text in {@link pk.contender.earmouse.FeedbackBarFragment}
     * @param text The text to set in the FeedbackBarFragment text field.
     */
    private void setFeedbackText(String text) {
        FeedbackBarFragment feedbackFragment = (FeedbackBarFragment) getFragmentManager().findFragmentById(R.id.feedbackbar);
        if(feedbackFragment != null) {
            feedbackFragment.setFeedback(text);
        } else {
            Log.d("DEBUG", "FeedbackBarFragment is null");
        }
    }

    /**
     * Update {@link pk.contender.earmouse.FeedbackBarFragment} statistics
     */
    public void updateFeedbackStatistics() {
        FeedbackBarFragment feedbackFragment = (FeedbackBarFragment) getFragmentManager().findFragmentById(R.id.feedbackbar);
        if(feedbackFragment != null) {
            feedbackFragment.setStatistics(mod.getSuccessRate());
        } else {
            Log.d("DEBUG", "FeedbackBarFragment is null");
        }
    }

    /**
     * Fade out the Button in {@link pk.contender.earmouse.ButtonGridFragment} at the given position
     * @param position The position of the Button in {@link pk.contender.earmouse.ButtonGridFragment} to fade out.
     */
    private void fadeButton(int position) {
        ButtonGridFragment buttonFragment = (ButtonGridFragment) getFragmentManager().findFragmentById(R.id.buttongrid);
        if(buttonFragment != null) {
            buttonFragment.fadeButton(position);
        } else
            Log.d("DEBUG", "ButtonGridFragment is null");
    }

    public void onPracticeModeToggle() {
        practiceMode = !practiceMode;
        Log.d("debug", "practiceMode is set to: " + (practiceMode ? "true" : "false"));
    }
}
