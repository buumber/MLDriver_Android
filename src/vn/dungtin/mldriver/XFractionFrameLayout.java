package vn.dungtin.mldriver;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.FrameLayout;

/**
 * Created by buumb on 6/12/2016.
 */
public class XFractionFrameLayout extends FrameLayout
{
    public XFractionFrameLayout(Context context)
    {
        super(context);
    }

    public XFractionFrameLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public XFractionFrameLayout(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    public float getXFraction()
    {
        if (getWidth() == 0)
            return 0;
        return getX() / getWidth();
    }

    public void setXFraction(float xFraction)
    {
        final int width = getWidth();
        setX((width > 0) ? (xFraction * width) : -9999);
    }
}
