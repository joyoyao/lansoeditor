package com.example.lansongeditordemo.view;

import com.lansosdk.box.ViewSprite;

import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.WebView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

/**
 * 使用在ViewSprite中, 用来演示ViewSprite的使用.
 */
public class GLRelativeLayout extends RelativeLayout {

    private ViewSprite mSprite;

    // default constructors

    public GLRelativeLayout(Context context) {
        super(context);
    }

    public GLRelativeLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public GLRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    @Override
    public void draw(Canvas canvas) {
    	//拿到Canvas
    	if(mSprite!=null)
    	{
    			Canvas glAttachedCanvas = mSprite.onDrawViewBegin();
    			
    	        if(glAttachedCanvas != null) {
    	            //prescale canvas to make sure content fits
    	        	
    	        	//TextView tv=(TextView)this.getChildAt(0);
    	        	//Log.i("testview","this.getChildCount():"+this.getChildCount()+ "+  "+tv.getText());
    	        	
    	            float xScale = glAttachedCanvas.getWidth() / (float)canvas.getWidth();
//    	            Log.i("testview","glAttachedCanvas getWidth:"+glAttachedCanvas.getWidth()+" canvas getWidth "+canvas.getWidth());
//    	            Log.i("testview","glAttachedCanvas getHeight:"+glAttachedCanvas.getHeight()+" canvas getHeight "+canvas.getHeight());
    	            
    	            glAttachedCanvas.scale(xScale, xScale);
    	            //draw the view to provided canvas
    	            super.draw(glAttachedCanvas);  
    	            }
    	        // notify the canvas is updated
    	        mSprite.onDrawViewEnd();
    	}
    }

    public void setViewSprite(ViewSprite sprite){
        mSprite = sprite;
    }
}
