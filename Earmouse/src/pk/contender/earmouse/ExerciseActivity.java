package pk.contender.earmouse;

import android.app.Activity;
import android.media.AudioManager;
import android.os.Bundle;
import android.view.View;

/**
 * Activity for doing the exercises of a given {@link pk.contender.earmouse.Module}
 * <p>
 * Activity is always called with a position in the getExtras() of the calling Intent.
 * This position is an index for the return value of {@link Main#loadModulesList}
 *
 * @author Paul Klinkenberg <pklinken.development@gmail.com>
 */
public class ExerciseActivity extends Activity implements ButtonGridFragment.AnswerSelectedListener, ButtonGridFragment.PracticeModeToggleListener {

    /**
     * Constant used for getting the value of the selected Module's position from Intent payload.
     */
    public static final String EXTRA_POSITION = "EXTRA_POSITION";
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_exercise);

        // Set the in-app volume control to always control the stream we use for playback.
        setVolumeControlStream(AudioManager.STREAM_MUSIC);
	}

    /**
     * Defers click event on Play button to the ExerciseFragment
     * @param view The view that was clicked.
     */
	public void onClickPlay(View view) {
		ExerciseFragment fragment = (ExerciseFragment) getFragmentManager().findFragmentById(R.id.fragment_exercise);
		if(fragment != null && fragment.isInLayout()) {
			fragment.onClickPlay(view);
		}
	}

    /**
     * Defers click event on the ButtonGrid to the ExerciseFragment
     * @param position Position of the button that was clicked.
     */
    @Override
	public void onAnswerSelected(int position) {
		ExerciseFragment fragment = (ExerciseFragment) getFragmentManager().findFragmentById(R.id.fragment_exercise);
		if(fragment != null && fragment.isInLayout()) {
			fragment.onAnswerSelected(position);
		}
	}

	@Override
	public void onPracticeModeToggle() {
		ExerciseFragment fragment = (ExerciseFragment) getFragmentManager().findFragmentById(R.id.fragment_exercise);
		if(fragment != null && fragment.isInLayout()) {
			fragment.onPracticeModeToggle();
		}
	}

}
