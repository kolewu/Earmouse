package pk.contender.earmouse;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.ToggleButton;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Fragment for setting up and handling events for a grid of Button objects that represent the possible
 * answers of an {@link pk.contender.earmouse.Exercise}.
 * 
 * @author Paul Klinkenberg <pklinken.development@gmail.com>
 */
public class ButtonGridFragment extends Fragment {

    /* SharedPreferences constants */
    private static final String PREFERENCES_MFADEDBUTTONLIST = "preferences_mFadedButtonList";
    private static final String PREFERENCES_MANSWERLIST = "preferences_mAnswerList";
    private static AnswerSelectedListener asListener;
	private static PracticeModeToggleListener pmListener;
    /** The maximum width of the button grid */
	private static final int COLUMNS = 3;
	private Context mCtx;
    /** Basic layout that will contain the {@link android.widget.TableLayout} */
    private LinearLayout base;
    /** TableLayout that will contain as many {@link android.widget.TableRow} children as necessary */
	private TableLayout tl;

    /** List of answers to be displayed on the buttons. */
    private List<String> mAnswerList = null;
    /** List of buttons that are currently faded out, used for state management */
    private List<Integer> mFadedButtonList = null;

    @Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mCtx = getActivity();
		if(mCtx == null)
			Log.d("DEBUG", "Context is null in ButtonGrid onCreate()");
	}

    /**
     * On view creation load any saved state and reconstruct the UI how we left it.
     */
	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		View view = inflater.inflate(R.layout.fragment_buttongrid, container, false);
		base = (LinearLayout) view.findViewById(R.id.grid_base);
		ToggleButton toggleButton = (ToggleButton) view.findViewById(R.id.toggleButton);
		toggleButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View view) {
				pmListener.onPracticeModeToggle();
			}
		});

        SharedPreferences settings = mCtx.getSharedPreferences(Main.PREFS_NAME, Activity.MODE_PRIVATE);

        Gson gson = new Gson();
        Type listType = new TypeToken<ArrayList<String>>() {}.getType();
        mAnswerList = gson.fromJson(settings.getString(PREFERENCES_MANSWERLIST, null), listType);
        listType = new TypeToken<ArrayList<Integer>>() {}.getType();
        mFadedButtonList = gson.fromJson(settings.getString(PREFERENCES_MFADEDBUTTONLIST, null), listType);
        if (mAnswerList != null)
            buildGrid();
        if (mFadedButtonList != null && tl != null) {
            for (Integer i : mFadedButtonList) {
                Button b = (Button)tl.findViewById(i);
                if(b != null) {
                    b.setAlpha((float) 0.2);
                    b.setClickable(false);
                }
            }
        }

		// Default is Answer mode, corresponding with ExerciseFragment default practiceMode being false.
		boolean practiceMode = settings.getBoolean(ExerciseFragment.PREFERENCES_PRACTICEMODE, false);
		toggleButton.setChecked(!practiceMode);

		return view;
	}

    /**
     * Override to ensure the Activity we are attached to implements {@link pk.contender.earmouse.ButtonGridFragment.AnswerSelectedListener}
     * @see AnswerSelectedListener
     * @param activity The activity this fragment is attached to.
     */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof AnswerSelectedListener) {
			asListener = (AnswerSelectedListener) activity;
		} else {
			throw new ClassCastException(activity.toString()
					+ " must implement ButtonGridFragment.AnswerSelectedListener");
		}
		if (activity instanceof PracticeModeToggleListener) {
			pmListener = (PracticeModeToggleListener) activity;
		} else {
			throw new ClassCastException(activity.toString()
					+ " must implement ButtonGridFragment.PracticeModeListener");
		}
	}

    @Override
    public void onSaveInstanceState(Bundle outState) {
        SharedPreferences settings = mCtx.getSharedPreferences(Main.PREFS_NAME, Activity.MODE_PRIVATE);
        Gson gson = new Gson();
        settings.edit().putString(PREFERENCES_MANSWERLIST, gson.toJson(mAnswerList))
                .putString(PREFERENCES_MFADEDBUTTONLIST, gson.toJson(mFadedButtonList)).apply();

        super.onSaveInstanceState(outState);
    }

    /**
	 * Listener interface for Button clicks, any Activity that attaches this Fragment must implement this.
	 */
	public interface AnswerSelectedListener {
		public void onAnswerSelected(int position);
	}

	/**
	 * Listener interface for Practice mode toggle button, any Activity that attaches this Fragment must implement this.
	 */
	public interface PracticeModeToggleListener {
		public void onPracticeModeToggle();
	}

	/**
	 * Build a fresh TableLayout for the given List of Strings.
	 * @param answerList The answers to display on the Buttons.
	 */
	public void initGrid(List<String> answerList) {

        mAnswerList = answerList;
        mFadedButtonList = new ArrayList<>();
		clearGrid();
		buildGrid();
	}

    /**
     * Constructs a grid of Button objects from the mAnswerlist field.
     */
    private void buildGrid() {
        tl = new TableLayout(mCtx);
        tl.setStretchAllColumns(true);
        tl.setShrinkAllColumns(true);
        base.addView(tl, new TableLayout.LayoutParams());
        TableRow tr = null;
        if(mAnswerList == null)
            return;

        for(int i = 0;i < mAnswerList.size(); i++) {
            if(i % COLUMNS == 0) {
                tr = new TableRow(mCtx);
                tl.addView(tr);
            }
            Button b = new Button(mCtx);
            b.setTextAppearance(mCtx , R.style.GridButtonStyle);
            b.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.MATCH_PARENT, TableRow.LayoutParams.MATCH_PARENT));
            b.setWidth(0);
            b.setText(mAnswerList.get(i));
            b.setId(i);
            b.setOnClickListener(new GridButtonClickListener(i));
            b.setBackground(getActivity().getResources().getDrawable(R.drawable.btn_default_holo_dark));
            if (tr != null) {
                tr.addView(b);
            }
        }
    }

    /**
     * Clear the TableLayout.
     */
    void clearGrid() {
		if(tl != null)
			tl.removeAllViews();
	}

	private static void answerSelected(int position) {
		asListener.onAnswerSelected(position);
	}

	/**
	 * Fade out the Button at the given position.
     * Fades out the given Button from 1.0 to 0.2 alpha in 500 ms, and stops
     * the button from receiving further click events.
	 * @param position The position of the Button to fade out.
	 */
	public void fadeButton(int position) {
		if(tl != null) {
			Button b = (Button)tl.findViewById(position);
			if(b != null) {
				b.animate().setDuration(500).alpha((float) 0.5);
				b.setClickable(false);
			}
			mFadedButtonList.add(position);
		}
	}

	/**
	 * Undo any Alpha and setClickable changes done by fadeButton().
     * Cancels any running animations on every Button to ensure the Alpha value isn't changed
     * after this function..
	 */
	public void resetGridButtonState() {
		int i = 0;
		Button b;
		if(tl != null) {
			while((b = (Button)tl.findViewById(i)) != null) {
				b.animate().cancel();
				b.setAlpha(1.0f);
				b.setClickable(true);
				i++;
			}
			mFadedButtonList = new ArrayList<>();
		}

	}
	
	/**
	 * Listener class for ButtonGridAdapter click events.
	 * 
	 * @author Paul Klinkenberg <pklinken@gmail.com>
	 */
	public class GridButtonClickListener implements OnClickListener {

		private final int position;
		
		public GridButtonClickListener(int position) {
			this.position = position;
		}
		
		@Override
		public void onClick(View v) {
			ButtonGridFragment.answerSelected(position);
		}

	}
}
