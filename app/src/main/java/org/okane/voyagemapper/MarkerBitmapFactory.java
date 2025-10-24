package org.okane.voyagemapper;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;

public class MarkerBitmapFactory {
    private final Context ctx;
    private final @LayoutRes int layoutId;

    public MarkerBitmapFactory(Context ctx, @LayoutRes int layoutId) {
        this.ctx = ctx;
        this.layoutId = layoutId;
    }

    public Bitmap make(String title) {
        View root = LayoutInflater.from(ctx).inflate(layoutId, null, false);
        TextView tv = root.findViewById(R.id.title);
        String t = title == null ? "" : title;
        if (t.length() > 22) t = t.substring(0, 21) + "…";
        tv.setText(t);

        int spec = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED);
        root.measure(spec, spec);
        root.layout(0, 0, root.getMeasuredWidth(), root.getMeasuredHeight());

        Bitmap bmp = Bitmap.createBitmap(root.getMeasuredWidth(), root.getMeasuredHeight(), Bitmap.Config.ARGB_8888);
        new Canvas(bmp).drawColor(0x00000000);
        root.draw(new Canvas(bmp));
        return bmp;
    }
}
