package pk.contender.earmouse;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

/**
 * Fragment for the display and modification of the title and description of a {@link pk.contender.earmouse.Module}.
 *
 * @author Paul Klinkenberg <pklinken.development@gmail.com>
 */
public class ModuleDetailsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_moduledetail, container, false);
    }

    /**
     * Sets the module title in the fragment.
     *
     * @param title The title to display
     */
    public void setTitle(String title) {
        View v = getView();
        if(v != null) {
            TextView view = (TextView) v.findViewById(R.id.module_title);
            view.setText(title);
        }
    }

    /**
     * Sets the module description in the fragment
     *
     * @param description The description to display
     */
    public void setDescription(String description) {
        View v = getView();
        if(v != null) {
            TextView view = (TextView) v.findViewById(R.id.module_description);
            view.setText(description);
        }
    }
}
