package com.example.lansongeditordemo.view;


import android.annotation.TargetApi;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.TextureView;
import android.view.TextureView.SurfaceTextureListener;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.MediaController;
import android.widget.TableLayout;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jp.co.cyberagent.lansongsdk.gpuimage.GPUImageFilter;

import com.lansosdk.box.BitmapSprite;
import com.lansosdk.box.VideoFilterSprite;
import com.lansosdk.box.ISprite;
import com.lansosdk.box.MediaPoolUpdateMode;
import com.lansosdk.box.MediaPoolViewRender;
import com.lansosdk.box.VideoSprite;
import com.lansosdk.box.ViewSprite;
import com.lansosdk.box.onMediaPoolCompletedListener;
import com.lansosdk.box.onMediaPoolProgressListener;
import com.lansosdk.box.onMediaPoolSizeChangedListener;


/**
 *  视频处理预览和实时保存的View, 继承自FrameLayout.
 *  
 *   适用在增加到UI界面中, 一边预览,一边实时保存的场合.
 *
 */
public class MediaPoolView extends FrameLayout {
	
	private static final String TAG="MediaPoolView";
	private static final boolean VERBOSE = false;  
  
    private int mVideoRotationDegree;

    private TextureRenderView mTextureRenderView;
 	
    private MediaPoolViewRender  renderer;
 	
 	private SurfaceTexture mSurfaceTexture=null;
 	

 	
 	private int encWidth,encHeight,encBitRate,encFrameRate;
 	/**
 	 *  经过宽度对齐到手机的边缘后, 缩放后的宽高,作为opengl的宽高. 
 	 */
 	private int viewWidth,viewHeight;  
 	
 	  
    public MediaPoolView(Context context) {
        super(context);
        initVideoView(context);
    }

    public MediaPoolView(Context context, AttributeSet attrs) {
        super(context, attrs);
        initVideoView(context);
    }

    public MediaPoolView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initVideoView(context);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    public MediaPoolView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
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
    	
    	mTextureRenderView.setDispalyRatio(AR_ASPECT_FIT_PARENT);
        
    	View renderUIView = mTextureRenderView.getView();
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT,
                Gravity.CENTER);
        renderUIView.setLayoutParams(lp);
        addView(renderUIView);
        mTextureRenderView.setVideoRotation(mVideoRotationDegree);
    }
    private String encodeOutput=null; //编码输出路径
    
    private MediaPoolUpdateMode mUpdateMode=MediaPoolUpdateMode.ALL_VIDEO_READY;
    private int mAutoFlushFps=0;
    
    /**
     * 设置MediaPool的刷新模式,默认 {@link MediaPool.UpdateMode#ALL_VIDEO_READY};
     * 
     * @param mode
     * @param autofps  //自动刷新的参数,每秒钟刷新几次(即视频帧率).当自动刷新的时候有用, 不是自动,则不起作用.
     */
    public void setUpdateMode(MediaPoolUpdateMode mode,int autofps)
    {
    	mAutoFlushFps=autofps;
    	
    	mUpdateMode=mode;
    	
    	if(renderer!=null)
    	{
    		 renderer.setUpdateMode(mUpdateMode,mAutoFlushFps);
    	}
    }
    /**
     * 获取当前View的 宽度
     * @return
     */
    public int getViewWidth(){
    	return viewWidth;
    }
    /**
     * 获得当前View的高度.
     * @return
     */
    public int getViewHeight(){
    	return viewHeight;
    }
    
  
    public interface onViewAvailable {	    
        void viewAvailable(MediaPoolView v);
    }
	private onViewAvailable mViewAvailable=null;
	  /**
     * 此回调仅仅是作为演示: 当跳入到别的Activity后的返回时,会再次预览当前画面的功能.
     * 你完全可以重新按照你的界面需求来修改这个MediaPoolView类.
     *
     */
	public void setOnViewAvailable(onViewAvailable listener)
	{
		mViewAvailable=listener;
	}
	
	
	private class SurfaceCallback implements SurfaceTextureListener {
    			
				/**
				 *  Invoked when a {@link TextureView}'s SurfaceTexture is ready for use.
				 *   当画面呈现出来的时候, 会调用这个回调.
				 *   
				 *  当Activity跳入到别的界面后,这时会调用{@link #onSurfaceTextureDestroyed(SurfaceTexture)} 销毁这个Texture,
				 *  如果您想在再次返回到当前Activity时,再次显示预览画面, 可以在这个方法里重新设置一遍MediaPool,并再次startMediaPool 
				 */
				
    	        @Override
    	        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {

    	            mSurfaceTexture = surface;
    	            viewHeight=height;
    	            viewWidth=width;
    	            if(mViewAvailable!=null){
    	            	mViewAvailable.viewAvailable(null);
    	            }	
    	        }
    	        
    	        /**
    	         * Invoked when the {@link SurfaceTexture}'s buffers size changed.
    	         * 当创建的TextureView的大小改变后, 会调用回调.
    	         * 
    	         * 当您本来设置的大小是480x480,而MediaPool会自动的缩放到父view的宽度时,会调用这个回调,提示大小已经改变, 这时您可以开始startMediaPool
    	         * 如果你设置的大小更好等于当前Texture的大小,则不会调用这个, 详细的注释见 {@link MediaPoolView#startMediaPool(onMediaPoolProgressListener, onMediaPoolCompletedListener)}
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
    	         *  Invoked when the specified {@link SurfaceTexture} is about to be destroyed.
    	         *  
    	         *  当您跳入到别的Activity的时候, 会调用这个,销毁当前Texture;
    	         *  
    	         */
    	        @Override
    	        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
    	            mSurfaceTexture = null;
    	            viewHeight=0;
    	            viewWidth=0;
    	            return false;
    	        }
    	
    	        /**
    	         * 
    	         * Invoked when the specified {@link SurfaceTexture} is updated through
    	         * {@link SurfaceTexture#updateTexImage()}.
    	         * 
    	         *每帧视频如果更新了, 则会调用这个!!!! 
    	         */
    	        @Override
    	        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
    	        }
    }
		/**
		 * 
		 * 设置使能 实时保存, 即把正在MediaPool中呈现的画面实时的保存下来,实现所见即所得的模式
		 * 
		 *  如果实时保存的宽高和原视频的宽高不成比例,则会先等比例缩放原视频,然后在多出的部分出增加黑边的形式呈现,比如原视频是16:9,设置的宽高是480x480,则会先把原视频按照宽度进行16:9的比例缩放.
		 *  在缩放后,在视频的上下增加黑边的形式来实现480x480, 从而不会让视频变形.
		 *  
		 *  如果视频在拍摄时有角度, 比如手机相机拍照,会有90度或270, 则会自动的识别拍摄的角度,并在缩放时,自动判断应该左右加黑边还是上下加黑边.
		 *  
		 *  因有缩放的特性, 您可以直接向MediaPool中投入一个视频,然后把宽高比设置一致,这样可实现一个视频压缩的功能;您也可以把宽高比设置一下,这样可实现视频加黑边的压缩功能.
		 *  或者您完全不进行缩放, 仅仅想把视频码率减低一下, 也可以把其他参数设置为和源视频一致, 仅仅调试encBr这个参数,来实现视频压缩的功能.
		 *  
		 * @param encW  录制视频的宽度
		 * @param encH  录制视频的高度
		 * @param encBr 录制视频的bitrate,
		 * @param encFr 录制视频的 帧率
		 * @param outPath  录制视频的保存路径.
		 */
	   public void setRealEncodeEnable(int encW,int encH,int encBr,int encFr,String outPath)
	    {
	    	if(outPath!=null && encW>0 && encH>0 && encBr>0 && encFr>0){
	    			encWidth=encW;
			        encHeight=encH;
			        encBitRate=encBr;
			        encFrameRate=encFr;
			        encodeOutput=outPath;
	    	}else{
	    		Log.w(TAG,"enable real encode is error,may be outpath is null");
	    		encodeOutput=null;
	    	}
	    }
	
	   private onMediaPoolSizeChangedListener mSizeChangedCB=null; 
	   /**
	    * 设置当前MediaPool的宽度和高度,并把宽度自动缩放到父view的宽度,然后等比例调整高度.
	    * 
	    * 如果在父view中已经预设好了希望的宽高,则可以不调用这个方法,直接 {@link #startMediaPool(onMediaPoolProgressListener, onMediaPoolCompletedListener)}
	    * 可以通过 {@link #getViewHeight()} 和 {@link #getViewWidth()}来得到当前view的宽度和高度.
	    * 
	    * 
	    * 注意: 这里的宽度和高度,会根据手机屏幕的宽度来做调整,默认是宽度对齐到手机的左右两边, 然后调整高度, 把调整后的高度作为mediaPool渲染线程的真正宽度和高度.
	    * 注意: 此方法需要在 {@link #startMediaPool(onMediaPoolProgressListener, onMediaPoolCompletedListener)} 前调用.
	    * 比如设置的宽度和高度是480,480, 而父view的宽度是等于手机分辨率是1080x1920,则mediaPool默认对齐到手机宽度1080,然后把高度也按照比例缩放到1080.
	    * 
	    * @param width  MediaPool宽度
	    * @param height MediaPool高度 
	    * @param cb   设置好后的回调, 注意:如果预设值的宽度和高度经过调整后 已经和父view的宽度和高度一致,则不会触发此回调(当然如果已经是希望的宽高,您也不需要调用此方法).
	    */
	public void setMediaPoolSize(int width,int height,onMediaPoolSizeChangedListener cb){
		
		if (width != 0 && height != 0 && cb!=null) {
            if (mTextureRenderView != null) {
            	
                mTextureRenderView.setVideoSize(width, height);
                mTextureRenderView.setVideoSampleAspectRatio(1,1);
                mSizeChangedCB=cb;
            }
            requestLayout();
        }
	}

	/**
	 * 开始mediaPool的渲染线程. 
	 * 此方法可以在 {@link onMediaPoolSizeChangedListener} 完成后调用.
	 * 可以先通过 {@link #setMediaPoolSize(int, int, onMediaPoolSizeChangedListener)}来设置宽高,然后在回调中执行此方法.
	 * 如果您已经在xml中固定了view的宽高,则可以直接调用这里, 无需再设置MediaPoolSize
	 * @param progresslistener
	 * @param completedListener
	 */
	public void startMediaPool(onMediaPoolProgressListener progresslistener,onMediaPoolCompletedListener completedListener)
	{
		 if( mSurfaceTexture!=null)
         {
 			renderer=new MediaPoolViewRender(getContext(), viewWidth, viewHeight);  //<----从这里去建立MediaPool线程.
 			if(renderer!=null){
 				//因为要预览,这里设置显示的Surface,当然如果您有特殊情况需求,也可以不用设置,但displayersurface和EncoderEnable要设置一个,MediaPoolRender才可以工作.
 				renderer.setDisplaySurface(new Surface(mSurfaceTexture));
 				
 				renderer.setEncoderEnable(encWidth,encHeight,encBitRate,encFrameRate,encodeOutput);
 				
 				renderer.setUpdateMode(mUpdateMode,mAutoFlushFps);
 				
 				 //设置MediaPool处理的进度监听, 回传的currentTimeUs单位是微秒.
 				renderer.setMediaPoolProgressListener(progresslistener);
 				renderer.setMediaPoolCompletedListener(completedListener);
 				
 				renderer.startMediaPool();
 				
 			}
         }
	}
	/**
	 * 暂停MediaPool的画面更新.
	 * 在一些场景里,您需要开启MediaPool后,暂停下, 然后obtain各种Sprite后,安排好各种事宜后,再让其画面更新,
	 * 则用到这个方法.
	 */
	public void pauseMediaPool()
	{
		if(renderer!=null){
			renderer.pauseUpdateMediaPool();
		}
	}
	/**
	 * 恢复之前暂停的MediaPool,让其继续画面更新. 与{@link #pauseMediaPool()}配对使用.
	 */
	public void resumeMediaPool()
	{
		if(renderer!=null){
			renderer.resumeUpdateMediaPool();
		}
	}
	/**
    * 设置是否使用主视频的时间戳为录制视频的时间戳, 默认第一次获取到的VideoSprite或FilterSprite为主视频.
    * 如果您传递过来的是一个完整的视频, 只是需要在此视频上做一些操作, 操作完成后,时长等于源视频的时长, 则建议使用主视频的时间戳, 如果视频是从中间截取一般开始的
    * 则不建议使用, 默认是这里为false;
    * 
    * 注意:需要在MediaPool开始前使用.
    */
    public void setUseMainVideoPts(boolean use)
    {
    	if(renderer!=null){
    		renderer.setUseMainVideoPts(use);
    	}
    }
	/**
	 * 当前MediaPool是否在工作.
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
	 * 停止MediaPool的渲染线程
	 * 因为视频的来源是外接驱动的, MediaPoolViewRender不会自动停止, 故需要外部在视频播放外的时候, 调用此方法来停止MediaPool渲染线程.
	 */
	public void stopMediaPool()
	{
			if (renderer != null){
	        	renderer.release();
	        	renderer=null;
	        }
	}
	/**
	 * 作用同 {@link #setMediaPoolSize(int, int, onMediaPoolSizeChangedListener)}, 只是有采样宽高比, 如用我们的VideoPlayer则使用此方法,
	 * 建议用 {@link #setMediaPoolSize(int, int, onMediaPoolSizeChangedListener)}
	 * @param width
	 * @param height
	 * @param sarnum  如mediaplayer设置后,可以为1,
	 * @param sarden  如mediaplayer设置后,可以为1,
	 * @param cb
	 */
	public void setMediaPoolSize(int width,int height,int sarnum,int sarden,onMediaPoolSizeChangedListener cb)
	{
		if (width != 0 && height != 0) {
            if (mTextureRenderView != null) {
                mTextureRenderView.setVideoSize(width, height);
                mTextureRenderView.setVideoSampleAspectRatio(sarnum,sarden);
            }
            mSizeChangedCB=cb;
            requestLayout();
        }
	}
	/**
	 * 获取一个主视频的 VideoSprite
	 * @param width 主视频的画面宽度  建议用 {@link MediaInfo#vWidth}来赋值
	 * @param height  主视频的画面高度 
	 * @return
	 */
	public VideoSprite obtainMainVideoSprite(int width, int height)
    {
		VideoSprite ret=null;
	    
		if(renderer!=null)
			ret=renderer.obtainMainVideoSprite(width, height);
		else{
			Log.e(TAG,"setMainVideoSprite error render is not avalid");
		}
		return ret;
    }
	/**
	 * 为视频获取一个滤镜效果的Sprite(FilterSprite), 这样可以投递到MediaPool中的视频做滤镜处理,比如美颜,小清新等.
	 * 
	 * 注意:此方法一定在 startMediaPool之后,在stopMediaPool之前调用.
	 * 
	 * @param width  视频的宽度.
	 * @param height 视频的高度
	 * @param filter  滤镜对象.如果您需要在MediaPool中切换滤镜, 可以通过{@link #switchFilterTo(FilterSprite, GPUImageFilter)}来完成.
	 * @return  返回创建好的FilterSprite对象.
	 */
	public VideoFilterSprite obtainFilterSprite(int width, int height,GPUImageFilter filter)
    {
		VideoFilterSprite ret=null;
	    
		if(renderer!=null)
			ret=renderer.obtainVideoFilterSprite(width, height,filter);
		else{
			Log.e(TAG,"obtainFilterSprite error render is not avalid");
		}
		return ret;
    }
	/**
	 * 获取一个VideoSprite,从中获取surface {@link VideoSprite#getSurface()}来设置到视频播放器中,
	 * 用视频播放器提供的画面,来作为MediaPool的画面输入源.
	 * 
	 * 注意:此方法一定在 startMediaPool之后,在stopMediaPool之前调用.
	 * 
	 * @param width  视频的宽度
	 * @param height 视频的高度
	 * @return  VideoSprite对象
	 */
	public VideoSprite obtainSubVideoSprite(int width, int height)
	{
		if(renderer!=null)
			return renderer.obtainVideoSprite(width,  height);
		else{
			Log.e(TAG,"obtainSubVideoSprite error render is not avalid");
			return null;
		}
	}
	/**
	 * 获取一个BitmapSprite
	 * 注意:此方法一定在 startMediaPool之后,在stopMediaPool之前调用.
	 * @param bmp  图片的bitmap对象,可以来自png或jpg等类型,这里是通过BitmapFactory.decodeXXX的方法转换后的bitmap对象.
	 * @return 一个BitmapSprite对象
	 */
	public BitmapSprite obtainBitmapSprite(Bitmap bmp)
	{
		Log.i(TAG,"imgBitmapSprite:"+bmp.getWidth()+" height:"+bmp.getHeight());
    	
		if(renderer!=null)
			return renderer.obtainBitmapSprite(bmp);
		else{
			Log.e(TAG,"obtainBitmapSprite error render is not avalid");
			return null;
		}
	}
	 
	/**
	 * 获得一个 ViewSprite,您可以在获取后,仿照我们的例子,来为视频增加各种UI空间,比如增加一个玫瑰心的动画等等.
	 * 注意:此方法一定在 startMediaPool之后,在stopMediaPool之前调用.
	 * @return 返回ViewSprite对象.
	 */
	 public ViewSprite obtainViewSprite()
	 {
			if(renderer!=null)
				return renderer.obtainViewSprite();
			else{
				Log.e(TAG,"obtainViewSprite error render is not avalid");
				return null;
			}
	 }
	 /**
	  * 从渲染线程列表中移除并销毁这个Sprite;
	  * 注意:此方法一定在 startMediaPool之后,在stopMediaPool之前调用.
	  * @param sprite
	  */
	public void removeSprite(ISprite sprite)
	{
		if(sprite!=null)
		{
			if(renderer!=null)
				renderer.removeSprite(sprite);
			else{
				Log.w(TAG,"removeSprite error render is not avalid");
			}
		}
	}
	/**
	 * 为已经创建好的FilterSprite对象切换滤镜效果
	 * @param sprite  已经创建好的FilterSprite对象
	 * @param filter  要切换到的滤镜对象.
	 * @return 切换成功,返回true; 失败返回false
	 */
	   public boolean  switchFilterTo(VideoFilterSprite sprite, GPUImageFilter filter) {
	    	if(renderer!=null){
	    		return renderer.switchFilterTo(sprite, filter);
	    	}
    		return false;
	    }
}
