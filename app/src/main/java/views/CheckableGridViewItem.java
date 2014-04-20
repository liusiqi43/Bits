package views;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.Checkable;
import android.widget.LinearLayout;

/**
 * Created by me on 4/20/14.
 */
public class CheckableGridViewItem extends LinearLayout implements Checkable {
    public CheckableGridViewItem(Context context) {
        super(context);
    }

    public CheckableGridViewItem(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

//    public CheckableGridViewItem(Context context, AttributeSet attrs, int defStyle) {
//        super(context, attrs, defStyle);
//    }

    @Override
    public void setChecked(boolean b) {

    }

    @Override
    public boolean isChecked() {
        return false;
    }

    @Override
    public void toggle() {

    }
}
