package fr.lite.launcher;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ScrollView;

/**
 * ScrollView that never scrolls by itself to reveal the focused tile.
 * MainActivity scrolls row by row instead, so rows always land at the same place
 * and no tile is left half cut at the top.
 */
public class LiteScrollView extends ScrollView {

    public LiteScrollView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected int computeScrollDeltaToGetChildRectOnScreen(Rect rect) {
        return 0;
    }
}
