package pk.contender.earmouse;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

/**
 * Adapter used for displaying a List of Modules in a ListView
 *
 * @author Paul Klinkenberg <pklinken.development@gmail.com>
 */
public class ModuleListAdapter extends ArrayAdapter<Module> {

	private final Context context;
	private final List<Module> moduleList;
	/**
	 * Set to determine from which activity this class is used.
	 */
	private final int target;
	/**
	 * Constant used if the target ListView is in the Main activity
	 */
	public static final int TARGET_MAINACTIVITY = 0;
	/**
	 * Constant used if the target ListView is in the Manager activity
	 */
	public static final int TARGET_MANAGERACTIVITY = 1;

  	public ModuleListAdapter(Context context, List<Module> moduleList, int target) {
		super(context, R.layout.rowlayout, moduleList);
		this.context = context;
		this.moduleList = moduleList;
		this.target = target;
	}

    @Override
    public int getCount() {
        return moduleList != null ? moduleList.size() : 0;
    }

    @Override
	public View getView(int position, View convertView, ViewGroup parent) {

		LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);

		if(target == TARGET_MAINACTIVITY) {
			// this ListView is in the main activity

			View rowView = inflater.inflate(R.layout.rowlayout, parent, false);

			TextView textView = (TextView) rowView.findViewById(R.id.label);
			ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
			textView.setText(moduleList.get(position).getTitle());

			int successRate = moduleList.get(position).getSuccessRate();
			if(successRate > 0) {
				Resources res = context.getResources();
				TextView statsText = (TextView) rowView.findViewById(R.id.stats);
				int exercisesCompleted = moduleList.get(position).getExercisesCompleted();
                statsText.setVisibility(View.VISIBLE);
                String text = String.format(res.getString(R.string.mainlist_successrate), successRate, exercisesCompleted)
                        + " " + res.getQuantityString(R.plurals.plural_exercise, exercisesCompleted);
                statsText.setText(text);
				//statsText.setText(successRate + "% " + res.getText(R.string.mainlist_successrate)
				//		+ " after " + exercisesCompleted + " " + res.getQuantityString(R.plurals.plural_exercise, exercisesCompleted));
			} else {
                TextView statsText = (TextView) rowView.findViewById(R.id.stats);
                if(statsText != null)
                    statsText.setVisibility(View.GONE);
            }

			// Set the difficulty icon
			Module mod = moduleList.get(position);
			if (mod.getDifficulty() == Module.DIFF_BEGINNER) {
				imageView.setImageResource(R.drawable.ic_difficulty1);
			} else if (mod.getDifficulty() == Module.DIFF_AMATEUR) {
				imageView.setImageResource(R.drawable.ic_difficulty2);
			} else if (mod.getDifficulty() == Module.DIFF_INTERMEDIATE) {
				imageView.setImageResource(R.drawable.ic_difficulty3);
			} else if (mod.getDifficulty() == Module.DIFF_EXPERT) {
				imageView.setImageResource(R.drawable.ic_difficulty4);
			}

            TextView shortDescriptionTextView = (TextView) rowView.findViewById(R.id.short_description);
            shortDescriptionTextView.setText(moduleList.get(position).getShortDescription());

            return rowView;
		} else if(target == TARGET_MANAGERACTIVITY) {
			// this ListView is in the manager activity

			View rowView = inflater.inflate(R.layout.manager_rowlayout, parent, false);

			TextView textView = (TextView) rowView.findViewById(R.id.label);
			TextView shortDescriptionTextView = (TextView) rowView.findViewById(R.id.short_description);
			ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);

			textView.setText(moduleList.get(position).getTitle());

			shortDescriptionTextView.setText(moduleList.get(position).getShortDescription());

			// Set the difficulty icon
			Module mod = moduleList.get(position);
			if (mod.getDifficulty() == Module.DIFF_BEGINNER) {
				imageView.setImageResource(R.drawable.ic_difficulty1);
			} else if (mod.getDifficulty() == Module.DIFF_AMATEUR) {
				imageView.setImageResource(R.drawable.ic_difficulty2);
			} else if (mod.getDifficulty() == Module.DIFF_INTERMEDIATE) {
				imageView.setImageResource(R.drawable.ic_difficulty3);
			} else if (mod.getDifficulty() == Module.DIFF_EXPERT) {
				imageView.setImageResource(R.drawable.ic_difficulty4);
			}

			return rowView;
		} else
			return null;
	}
}
