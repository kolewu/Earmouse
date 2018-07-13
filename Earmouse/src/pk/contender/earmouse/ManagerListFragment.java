package pk.contender.earmouse;

import android.app.Activity;
import android.app.ListFragment;
import android.view.View;
import android.widget.ListView;

/**
 * Used for display of remote installable Modules.
 * @author Paul Klinkenberg <pklinken.development@gmail.com>
 */
public class ManagerListFragment extends ListFragment {

	private OnModuleSelectedListener listener;

	/**
	 * Listener interface for ListView clicks, any activity that attaches this Fragment must implement this.
	 */
	public interface OnModuleSelectedListener {
		public void onModuleSelected(int position);
	}

   	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		listener.onModuleSelected(position);
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof OnModuleSelectedListener) {
			listener = (OnModuleSelectedListener) activity;
		} else {
			throw new ClassCastException(activity.toString()
					+ " must implement ManagerListFragment.OnModuleSelectedListener");
		}
        //ModuleManagerActivity.shownModuleList = new ArrayList<>();
        ModuleListAdapter arrayAdap = new ModuleListAdapter(getActivity(), ModuleManagerActivity.shownModuleList, ModuleListAdapter.TARGET_MANAGERACTIVITY);
        setListAdapter(arrayAdap);
    }
}
