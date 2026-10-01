package com.jerry.micbridge;

import android.content.Context;
import android.graphics.*;
import android.view.View;

/** Live level history only: no invented waveform and no stored microphone samples. */
public final class MeterView extends View {
    private final float[] history=new float[36];
    private final Paint paint=new Paint(3);
    private final RectF rect=new RectF();
    public MeterView(Context context) { super(context); setContentDescription("Live microphone level history"); }
    public void push(float peak) {
        System.arraycopy(history,1,history,0,history.length-1);
        history[history.length-1]=Math.max(0,Math.min(1,peak)); invalidate();
    }
    public void clear() { java.util.Arrays.fill(history,0); invalidate(); }
    @Override protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        float width=getWidth(), height=getHeight(), step=width/history.length;
        for(int i=0;i<history.length;i++) {
            float db=history[i]>0?(float)(20*Math.log10(history[i])):-60;
            float normal=Math.max(0,Math.min(1,(db+60)/60));
            float h=Math.max(4,normal*(height-10));
            paint.setColor(history[i]>0.98f?Color.rgb(252,136,116):Color.rgb(98,220,193));
            paint.setAlpha(75+(int)(180*i/(float)history.length));
            rect.set(i*step+step*0.2f,(height-h)/2,i*step+step*0.8f,(height+h)/2);
            canvas.drawRoundRect(rect,step/3,step/3,paint);
        }
    }
}
