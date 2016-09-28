package com.example.lansongeditordemo.view;


import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.SurfaceTexture;
import android.media.MediaPlayer;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.Surface;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.widget.FrameLayout;


import jp.co.cyberagent.lansongsdk.gpuimage.GPUImageFilter;

import com.lansosdk.box.FilterViewRender;
import com.lansosdk.box.onFilterViewSizeChangedListener;
import com.lansosdk.box.onMediaPoolCompletedListener;
import com.lansosdk.box.onMediaPoolProgressListener;


/**
 * 为视频滤镜封装的 view, 继承自FrameLayout.
 * 此类仅仅为了视频滤镜使用, 和MediaPool是相对独立的两个架构,没有关系.
 * 如果您仅仅做视频滤镜的操作,而不需要增加另外的功能, 可以使用这个做. 
 * 如果您需要在视频滤镜的过程中, 增加另外一些图片, 文字等效果, 请使用 {@link MediaPoolView}
 *
 */
public class FilterView extends FrameLayout {
    private String TAG = "LanSoSDK";
  
    private int mVideoRotationDegree;

    private TextureRenderView mTextureRenderView;
 	private FilterViewRender  renderer;
 	private SurfaceTexture mSurfaceTexture=null;
 	
 	private int viewWidth,viewHeight;  //setGlWidth/height经过textureview手机宽度对齐调整后的宽度和高度.
 	private int videoWidth,videoHeight;  //视频本身的宽度和高度.
 	
 	private int glWidth,glHeight;  //设置的opengl的宽度和高度.
 	private int encBitRate,encFrameRate;
    private String encOutputPath=null; //编码输出路径
 	
 	  
    public FilterView(Context context) {
        super(context);
        initVideoView(context);
    }

    public FilterView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    public FilterView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public FilterView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        initVideoView(context);
    }


    private void initVideoView(Context context) {
        setTextureView();

        setFocusable(true);
        setFocusableInTouchMode(true);
        requestFocus();
    }
    /**
     * 视频画面显示模式：不裁剪直接和父view匹配， 这样如果画面超出父view的尺寸，将会只显示视频画面的
     * 一部分，您可以使用这个来平铺视频画面，通过手动拖拽的形式来截取画面的一部分。类似视频画面区域裁剪的功能。
     */
    static final int AR_ASPECT_FIT_PARENT = 0; // without clip
    /**
     * 视频画面显示模式:裁剪和父view匹配, 当视频画面超过父view大小时,不会缩放,会只显示视频画面的一部分.
     * 超出部分不予显示.
     */
    static final int AR_ASPECT_FILL_PARENT = 1; // may clip
    /**
     * 视频画面显示模式: 自适应大小.当小于画面尺寸时,自动显示.当大于尺寸时,缩放显示.
     */
    static final int AR_ASPECT_WRAP_CONTENT = 2;
    /**
     * 视频画面显示模式:和父view的尺寸对其.完全填充满父view的尺寸
     */
    static final int AR_MATCH_PARENT = 3;
    /**
     * 把画面的宽度等于父view的宽度, 高度按照16:9的形式显示. 大部分的网络视频推荐用这种方式显示.
     */
    static final int AR_16_9_FIT_PARENT = 4;
    /**
     * 把画面的宽度等于父view的宽度, 高度按照4:3的形式显示.
     */
    static final int AR_4_3_FIT_PARENT = 5;

    private void setTextureView() {
    	mTextureRenderView = new TextureRenderView(getContext());
    	mTextureRenderView.setSurfaceTextureListener(new SurfaceCallback());
    	
    	//mTextureRenderView.setAspectRatio(AR_ASPECT_FIT_PARENT);
    	mTextureRenderView.setDispalyRatio(AR_ASPECT_FIT_PARENT);        
    	View renderUIView = mTextureRenderView.getView();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,//<--------------需要调整这里视频的宽度
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        addView(renderUIView);
        
        mTextureRenderView.setVideoRotation(mVideoRotationDegree);
    }
    /**
     * 获取当前渲染线程的宽度
     * @return
     */
    public int getViewWidth(){
    	return viewWidth;
    }
    /**
     * 获取当前渲染线程的高度.
     * @return
     */
    public int getViewHeight(){
    	return viewHeight;
    }
    
	private class SurfaceCallback implements SurfaceTextureListener {
    			
    	        @Override
    	        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

    	            mSurfaceTexture = surface;
    	            viewHeight=height;
    	            viewWidth=width;
    	        }
    	/**
    	 * 注意,这里是当TextureView变化的时候, 才调用这个回调,如果在xml中的宽高已经固定,则不会调用这个回调.
    	 * 相应的就不会相应mSizeChangedCB的回调.
    	 */
    	        @Override
    	        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
    	            mSurfaceTexture = surface;
    	            viewHeight=height;
    	            viewWidth=width;
    	            
    	            if(mSizeChangedCB!=null)
    	            	mSizeChangedCB.onSizeChanged(width, height);
    	        }
    	/**
    	 * 您可以在这里调用 {@link FilterView#stop()}方法,来停止FilterViewRender的工作.
    	 * 这样以免您实际使用中忘记在Activity pause的时候,忘记释放Render
    	 */
    	        @Override
    	        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
    	            mSurfaceTexture = null;
    	            return false;
    	        }
    	      
    	        @Override
    	        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    	        }
    }
	
	/**
	 * 设置使能 实时保存
	 * 视频的宽高是 在setFilterRenderSize方法的glwidth和glheight, 如果设置的宽高和原视频的不同,则会把原视频自动缩放到设置的宽高.
	 * 如果实时保存的宽高和原视频的宽高不成比例,则会先等比例缩放原视频,然后在多出的部分出增加黑边的形式呈现,比如原视频是16:9,设置的宽高是480x480,则会先把原视频按照宽度进行16:9的比例缩放.
	 * 在缩放后,在视频的上下增加黑边的形式来实现480x480, 从而不会让视频变形.
	 * 
	 * @param bitrate  视频保存时编码的码率
	 * @param framerate  帧率
	 * @param outPath  保存的路径
	 */
	   public void setRealEncodeEnable(int bitrate,int framerate,String outPath)
	    {
	    	if(outPath!=null && bitrate>0 && framerate>0){
	    		encOutputPath=outPath;
	    		encBitRate=bitrate;
	    		encFrameRate=framerate;
	    	}else{
	    		Log.w(TAG,"enable real encode is error,may be outpath is null");
	    		encOutputPath=null;
	    	}
	    }
	   /**
	    * 获取渲染线程中创建的Surface, 这个surface用来把外面的视频源通过 {@link MediaPlayer#setSurface(Surface)},把视频画面渲染到此surface上.
	    * @return
	    */
	   public Surface getSurface()
	   {
			if(renderer!=null)
	    		return renderer.getSurface();
	    	else
	    		return null;
	   }
	   /**
	    * 切换 滤镜
	    * 
	    * @param filter  切换后的滤镜对象.
	    * @return 切换成功返回true, 否则返回false
	    */
	   public boolean  switchFilterTo(final GPUImageFilter filter) {
	    	if(renderer!=null)
	    		return renderer.switchFilterTo(filter);
	    	else
	    		return false;
	    }
	   
	   private onFilterViewSizeChangedListener mSizeChangedCB=null; 
	   /**
	    * 设置 滤镜渲染的大小.
	    *  设置当前FilterView的宽度和高度,并把宽度自动缩放到父view的宽度,然后等比例调整高度.
	    * 
	    * 如果在父view中已经预设好了希望的宽高,则可以不调用这个方法,直接 {@link #start()}
	    * 可以通过 {@link #getViewHeight()} 和 {@link #getViewWidth()}来得到当前view的宽度和高度.
	    * 
	    * 比如设置的宽度和高度是480,480, 而父view的宽度是等于手机分辨率是1080x1920,则mediaPool默认对齐到手机宽度1080,然后把高度也按照比例缩放到1080.
	    * 
	    * 如果使能了视频实时保存功能, 则会把源视频缩放到设置的 glwidth,glheight这个宽高比,然后码率按照{@link #setRealEncodeEnable(int, int, String)}的来进行实时保存.
	    * 
	    * @param width  OpenGL预设值的宽度  
	    * @param height OpenGL预设值的高度 
	    * @param cb   设置好后的回调, 注意:如果预设值的宽度和高度经过调整后 已经和父view的宽度和高度一致,则不会触发此回调(当然如果已经是希望的宽高,您也不需要调用此方法).
	    * @param glwidth  渲染线程opengl的预设宽度 ,如果使能了实时保存功能,则认为是视频缩放到的宽度,
	    * @param glheight 渲染线程opengl的预设高度,如果使能了实时保存功能,则认为是视频缩放到的高度.
	    * @param videoW   需要渲染视频的画面宽度
	    * @param videoH   需要渲染视频的画面高度
	    * @param cb     filter的自适应屏幕的宽度调整后的回调.
	    */
	public void setFilterRenderSize(int glwidth,int glheight,int videoW,int videoH,onFilterViewSizeChangedListener cb)
	{
		
		if(videoW>0 && videoH>0)
		{
			videoWidth=videoW;
			videoHeight=videoH;
		}
		if (glwidth != 0 && glheight != 0 && cb!=null) {
			
			glWidth=glwidth;
			glHeight=glheight;
			
            if (mTextureRenderView != null) {
                mTextureRenderView.setVideoSize(glwidth, glheight);
                mTextureRenderView.setVideoSampleAspectRatio(1,1);
                mSizeChangedCB=cb;
            }
            requestLayout();
        }
	}
	/**
	 * 开始filter 渲染线程.
	 */
	public void start()
	{
		 if(renderer==null && mSurfaceTexture!=null && viewHeight>0 && viewWidth>0)
         {
	            if(renderer==null)
	            {
	            	renderer=new FilterViewRender(getContext(), mSurfaceTexture, viewWidth, viewHeight,videoWidth,videoHeight);
	            	if(encOutputPath!=null){
	            		renderer.setEncoderEnable(glWidth, glHeight, encBitRate, encFrameRate, encOutputPath);
	            	}
	            	renderer.start();
	            }
         }
	}
	/**
	 * 当前Render线程是否在工作.
	 * @return
	 */
	public boolean isRunning()
	{
		if(renderer!=null)
			return renderer.isRunning();
		else
			return false;
	}
	/**
	 * 停止当前渲染线程.
	 */
	public void stop()
	{
		 if (renderer != null){
	        	renderer.release();
	        	renderer=null;
	        }
	}
	
}
