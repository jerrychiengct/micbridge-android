package com.jerry.micbridge;

import android.graphics.*;
import android.graphics.drawable.Drawable;

/** Small original outline icons; density independent and usable with Android accessibility labels. */
public final class StudioIcon extends Drawable {
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final int kind;
    public StudioIcon(int kind,int color){this.kind=kind;paint.setColor(color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(1.7f);paint.setStrokeCap(Paint.Cap.ROUND);paint.setStrokeJoin(Paint.Join.ROUND);}
    @Override public void draw(Canvas canvas){
        int saved=canvas.save();Rect bounds=getBounds();canvas.translate(bounds.left,bounds.top);canvas.scale(bounds.width()/24f,bounds.height()/24f);
        switch(kind){
            case 0:for(int i=0;i<5;i++){float x=4+i*4,h=new float[]{5,12,18,10,5}[i];canvas.drawLine(x,12-h/2,x,12+h/2,paint);}break;
            case 1:for(int i=0;i<3;i++){float x=5+i*7,y=new float[]{8,16,10}[i];canvas.drawLine(x,3,x,y-3,paint);canvas.drawLine(x,y+3,x,21,paint);canvas.drawCircle(x,y,2.6f,paint);}break;
            case 2:canvas.drawRoundRect(5,2,19,22,3,3,paint);canvas.drawCircle(12,15,3.6f,paint);canvas.drawCircle(12,6.5f,1,paint);break;
            case 3:canvas.drawCircle(12,12,9,paint);canvas.drawLine(12,11,12,17,paint);canvas.drawPoint(12,7,paint);break;
            case 4:Path triangle=new Path();triangle.moveTo(8,4);triangle.lineTo(19,12);triangle.lineTo(8,20);triangle.close();canvas.drawPath(triangle,paint);break;
            case 5:canvas.drawRoundRect(5,5,19,19,2,2,paint);break;
            case 6:canvas.drawRoundRect(9,3,15,14,3,3,paint);canvas.drawArc(6,7,18,18,0,180,false,paint);canvas.drawLine(12,18,12,22,paint);canvas.drawLine(8,22,16,22,paint);break;
            case 7:canvas.drawArc(4,4,20,20,35,285,false,paint);canvas.drawLine(20,4,20,10,paint);canvas.drawLine(20,10,14,10,paint);break;
            case 9:canvas.drawLine(6,9,12,15,paint);canvas.drawLine(12,15,18,9,paint);break;
            default:canvas.drawLine(12,3,12,21,paint);canvas.drawLine(12,3,18,9,paint);canvas.drawLine(18,9,6,18,paint);canvas.drawLine(6,6,18,15,paint);canvas.drawLine(18,15,12,21,paint);
        }
        canvas.restoreToCount(saved);
    }
    @Override public void setAlpha(int alpha){paint.setAlpha(alpha);invalidateSelf();}
    @Override public void setColorFilter(ColorFilter filter){paint.setColorFilter(filter);invalidateSelf();}
    @Override public int getOpacity(){return PixelFormat.TRANSLUCENT;}
}
