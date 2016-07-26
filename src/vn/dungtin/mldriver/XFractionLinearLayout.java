package vn.dungtin.mldriver;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.LinearLayout;

/**
 * Created by buumb on 6/12/2016.
 */
public class XFractionLinearLayout extends LinearLayout
{
    public XFractionLinearLayout(Context context)
    {
        super(context);
    }

    public XFractionLinearLayout(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public XFractionLinearLayout(Context context, AttributeSet attrs, int defStyleAttr)
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
