package pk.contender.earmouse;

import android.animation.ValueAnimator;
import android.animation.ValueAnimator.AnimatorUpdateListener;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.TextView;

/**
 * Fragment that implements two TextViews to give the user feedback on his actions and display
 * a success rate on how the user is doing.
 * @author Paul Klinkenberg <pklinken.development@gmail.com>
 */
public class FeedbackBarFragment extends Fragment {

    private Context mCtx;
    /** The currently displayed success rate, used by the ValueAnimator */
    private int currentSuccessRate = 0;

    /* SharedPreferences constants */
    private static final String PREFERENCES_FEEDBACKTEXT = "preferences_feedbacktext";
    private static final String PREFERENCES_FEEDBACKNUMBER = "preferences_feedbacknumber";


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feedbackbar, container, false);

        mCtx = getActivity();
        if(mCtx == null)
            Log.d("DEBUG", "Context is null in FeedbackBarFragment onCreateView()");

        return view;
    }

    /**
     * Restore fragment state.
     */
    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SharedPreferences settings = mCtx.getSharedPreferences(Main.PREFS_NAME, Activity.MODE_PRIVATE);
        View v = getView();
        TextView textView = (TextView) (v != null ? v.findViewById(R.id.feedbackbar_text) : null);
        if(textView != null) {
            textView.setText(settings.getString(PREFERENCES_FEEDBACKTEXT, ""));
        }
        textView = (TextView) (v != null ? v.findViewById(R.id.feedbackbar_stats) : null);
        if(textView != null){
            settings.edit().putString(PREFERENCES_FEEDBACKNUMBER, (String) textView.getText()).apply();
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {

        View view = getView();
        SharedPreferences settings = mCtx.getSharedPreferences(Main.PREFS_NAME, Activity.MODE_PRIVATE);
        TextView textView = (TextView) (view != null ? view.findViewById(R.id.feedbackbar_text) : null);
        if(textView != null){
            settings.edit().putString(PREFERENCES_FEEDBACKTEXT, (String) textView.getText()).apply();
        }
        textView = (TextView) (view != null ? view.findViewById(R.id.feedbackbar_stats) : null);
        if(textView != null){
            settings.edit().putString(PREFERENCES_FEEDBACKNUMBER, (String) textView.getText()).apply();
        }
        super.onSaveInstanceState(outState);
    }


    /**
     * Displays and animates the given text.
     * @param feedback The feedback text to display
     */
    public void setFeedback(final String feedback) {
        View v = getView();
        final TextView textView = (TextView) (v != null ? v.findViewById(R.id.feedbackbar_text) : null);
        if(textView != null){
            textView.animate().setDuration(200).translationX(-300).withEndAction(new Runnable() {
                @Override
                public void run() {
                    textView.setText(feedback);
                    textView.setTranslationX(300);
                    textView.animate().setDuration(200).translationX(0);
                }
            });
        }
    }

    /**
     * Displays and animates the given success rate.
     * @param successRate The user's success rate in the current Module
     */
    public void setStatistics(int successRate) {
        View v = getView();
        final TextView textView = (TextView) (v != null ? v.findViewById(R.id.feedbackbar_stats) : null);
        if(textView != null) {
            final Resources res = mCtx.getResources();
            if(successRate >= 0) {
                ValueAnimator animValue = ValueAnimator.ofInt(currentSuccessRate, successRate);
                animValue.setInterpolator(new DecelerateInterpolator());
                animValue.setDuration(1000);
                animValue.addUpdateListener(new AnimatorUpdateListener() {

                    @Override
                    public void onAnimationUpdate(ValueAnimator animation) {
                        textView.setText((String) res.getText(R.string.feedback_successrate_value) + animation.getAnimatedValue() + "%");
                    }
                });
                animValue.start();
                currentSuccessRate = successRate;
            } else
                textView.setText(res.getText(R.string.feedback_successrate_nodata));
        }
    }

    /**
     * Set both TextViews in this fragment to display no text.
     */
    public void setEmpty() {
        View v = getView();
        final TextView textView = (TextView) (v != null ? v.findViewById(R.id.feedbackbar_stats) : null);
        if (textView != null) {
            textView.setText("");
        }
    }
}
