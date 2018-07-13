package pk.contender.earmouse;

import android.app.Activity;
import android.app.ListFragment;
import android.os.Bundle;
import android.view.View;
import android.widget.ListView;

import java.util.List;

/**
 * Fragment for the setup and display of a ListView of Module objects.
 * 
 * @author Paul Klinkenberg <pklinken.development@gmail.com>
 */
public class ModuleListFragment extends ListFragment {

	private OnModuleSelectedListener moduleSelectedListener;

	/**
	 * Listener interface for ListView clicks, any activity that attaches this Fragment must implement this.
	 */
	public interface OnModuleSelectedListener {
		public void onModuleSelected(View view, int position);

	}

 	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
        moduleSelectedListener.onModuleSelected(v, position);
	}

	/**
	 * Checks whether attaching Activity implements the OnModuleSelectedListener interface
     * and sets up the list adapter.
	 */
	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		if (activity instanceof OnModuleSelectedListener) {
			moduleSelectedListener = (OnModuleSelectedListener) activity;
		} else {
			throw new ClassCastException(activity.toString()
					+ " must implement ModuleListFragment.OnModuleSelectedListener");
		}

        List<Module> moduleList = Main.getModuleList();
        ModuleListAdapter arrayAdap = new ModuleListAdapter(getActivity(), moduleList, ModuleListAdapter.TARGET_MAINACTIVITY);
        setListAdapter(arrayAdap);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
        setEmptyText(this.getString(R.string.list_no_modules_installed));
		super.onActivityCreated(savedInstanceState);
	}
}
