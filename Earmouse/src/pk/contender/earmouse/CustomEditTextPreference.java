package pk.contender.earmouse;

import android.content.Context;
import android.preference.EditTextPreference;
import android.util.AttributeSet;

/**
 * Custom EditTextPreference that updates its own summary when its value is changed.
 * Created by Paul on 25-06-2016.
 */
public class CustomEditTextPreference extends EditTextPreference {
    public CustomEditTextPreference(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public CustomEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomEditTextPreference(Context context) {
        super(context);

    }

    @Override
    public void setText(String text) {
        super.setText(text);
        setSummary(text);
    }
}
