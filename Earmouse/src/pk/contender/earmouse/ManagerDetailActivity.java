package pk.contender.earmouse;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

/**
 * Activity for installing an item selected in {@link pk.contender.earmouse.ModuleManagerActivity}
 * @author Paul Klinkenberg <pklinken.development@gmail.com>
 */
public class ManagerDetailActivity extends Activity {

   	/** Intent extra value referring the ID of the module selected in the ListView
	 */
	public static final String EXTRA_MODULE_ID = "EXTRA_MODULE_ID";

    /**
     * Get the user selection from the Intent and set up the UI.
     *
     * @param savedInstanceState The saved instance state.
     */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_detail_manager);

		Bundle extras = getIntent().getExtras();
		if(extras != null) {
			// Read the Intent payload and set up the UI
			ManagerDetailsFragment detailFragment = (ManagerDetailsFragment) getFragmentManager().findFragmentById(R.id.fragmentDetailManager);
			if(detailFragment != null && detailFragment.isInLayout()) {
				detailFragment.setId(extras.getInt(EXTRA_MODULE_ID));
				detailFragment.update();
			} else {
				Log.d("DEBUG", "ManagerDetailsFragment is null");
			}
		}
	}

    /**
     * Defer an onClick event to the {@link pk.contender.earmouse.ManagerDetailsFragment}, as soon as control returns to this function
     * the selected Module is installed (or an error occurred) and we can finish this activity.
     * @param v The view that was clicked.
     */
	public void onButtonClick(View v) {
		ManagerDetailsFragment detailFragment = (ManagerDetailsFragment) getFragmentManager().findFragmentById(R.id.fragmentDetailManager);
		if(detailFragment != null && detailFragment.isInLayout()) {
			detailFragment.onButtonClick(v);
			this.finish();
		} else
			Log.d("DEBUG", "Could not relay click event");
	}
}
