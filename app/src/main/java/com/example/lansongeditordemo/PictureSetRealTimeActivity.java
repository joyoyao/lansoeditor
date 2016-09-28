package com.example.lansongeditordemo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;

import jp.co.cyberagent.lansongsdk.gpuimage.GPUImageFilter;

import com.example.lansongeditordemo.view.MediaPoolView;
import com.example.lansongeditordemo.view.MediaPoolView.onViewAvailable;
import com.lansoeditor.demo.R;
import com.lansosdk.box.MediaPool;
import com.lansosdk.box.MediaPoolUpdateMode;
import com.lansosdk.box.VideoSprite;
import com.lansosdk.box.ViewSprite;
import com.lansosdk.box.ISprite;
import com.lansosdk.box.onMediaPoolCompletedListener;
import com.lansosdk.box.onMediaPoolProgressListener;
import com.lansosdk.box.onMediaPoolSizeChangedListener;
import com.lansosdk.videoeditor.CopyFileFromAssets;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.SDKDir;
import com.lansosdk.videoeditor.SDKFileUtils;
import com.lansosdk.videoeditor.VideoEditor;
import com.lansosdk.videoeditor.player.IMediaPlayer;
import com.lansosdk.videoeditor.player.IMediaPlayer.OnPlayerPreparedListener;
import com.lansosdk.videoeditor.player.VPlayer;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnInfoListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

/**
 *  演示:  图片合成视频的同时保存成文件.
 *  流程: 把MediaPoolView设置为自动刷新模式, 然后一次性获取多个BitmapSprite,根据画面走动的时间戳来
 *  操作每个BitmapSprite是否移动,是否显示.
 *  
 *  这里仅仅演示移动的属性, 您实际中可以移动,缩放,旋转,RGBA值调节来混合使用,因为BitmapSprite继承自ISprite,故有这些特性.
 *  
 *  比如你根据时间戳来调节图片的RGBA中的A值(alpha透明度),则实现图片的淡入淡出效果.
 *  
 *  使用移动+缩放+RGBA调节,则实现一些缓慢照片变化的效果,浪漫文艺范的效果.
 *  
 *  视频标记就是一个典型的BitmapSprite的使用场景.
 *  
 */
public class PictureSetRealTimeActivity extends Activity{
    private static final String TAG = "VideoActivity";


    private MediaPoolView mPlayView;
    
    
    private ArrayList<SlideEffect>  slideEffectArray;
    
    private String dstPath=null;
    
    

    private Context mContext=null;
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
		 Thread.setDefaultUncaughtExceptionHandler(new snoCrashHandler());
        setContentView(R.layout.picture_set_layout);
        
        
        mPlayView = (MediaPoolView) findViewById(R.id.mediapool_view);
        
        
        findViewById(R.id.id_mediapool_saveplay).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				 if(SDKFileUtils.fileExist(dstPath)){
		   			 	Intent intent=new Intent(PictureSetRealTimeActivity.this,VideoPlayerActivity.class);
			    	    	intent.putExtra("videopath", dstPath);
			    	    	startActivity(intent);
		   		 }else{
		   			 Toast.makeText(PictureSetRealTimeActivity.this, "目标文件不存在", Toast.LENGTH_SHORT).show();
		   		 }
			}
		});
        findViewById(R.id.id_mediapool_saveplay).setVisibility(View.GONE);

        //在手机的/sdcard/lansongBox/路径下创建一个文件名,用来保存生成的视频文件,(在onDestroy中删除)
        dstPath=SDKFileUtils.newMp4PathInBox();
        
        mContext=getApplicationContext();
    }
    @Override
    protected void onResume() {
    	// TODO Auto-generated method stub
    	super.onResume();
    	new Handler().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				start();
			}
		}, 100);
    }
    private void start()
    {
		//设置为自动刷新模式, 帧率为25
    	mPlayView.setUpdateMode(MediaPoolUpdateMode.AUTO_FLUSH,25);
    	//使能实时录制,并设置录制后视频的宽度和高度, 码率, 帧率,保存路径.
    	mPlayView.setRealEncodeEnable(480,480,1000000,(int)25,dstPath);
    	
    	//设置MediaPool的宽高, 这里设置为480x480,如果您已经在xml中固定大小,则不需要再次设置,
    	//可以直接调用startMediaPool来开始录制.
    	mPlayView.setMediaPoolSize(480,480,new onMediaPoolSizeChangedListener() {
			
			@Override
			public void onSizeChanged(int viewWidth, int viewHeight) {
				// TODO Auto-generated method stub
				mPlayView.startMediaPool(new MediaPoolProgressListener(),new MediaPoolCompleted());
				
					isStarted=true;
				
				   DisplayMetrics dm = new DisplayMetrics();// 获取屏幕密度（方法2）
			       dm = getResources().getDisplayMetrics();
			        
			           
			      int screenWidth  = dm.widthPixels;	
			      String picPath=SDKDir.TMP_DIR+"/"+"picname.jpg";   
			      if(screenWidth>=1080){
			    	  CopyFileFromAssets.copy(mContext, "pic1080x1080u2.jpg", SDKDir.TMP_DIR, "picname.jpg");
			      }  
			      else{
			    	  CopyFileFromAssets.copy(mContext, "pic720x720.jpg", SDKDir.TMP_DIR, "picname.jpg");
			      }
			      //先 获取第一张Bitmap的Sprite, 因为是第一张,放在MediaPool中维护的数组的最下面, 认为是背景图片.
			      mPlayView.obtainBitmapSprite(BitmapFactory.decodeFile(picPath));
			      
			      slideEffectArray=new ArrayList<SlideEffect>();
			      
					//这里同时获取多个,只是不显示出来.
			      getFifthSprite(R.drawable.pic1,0,5000);  		//1--5秒.
			      getFifthSprite(R.drawable.pic2,5000,10000);  //5--10秒.
			      getFifthSprite(R.drawable.pic3,10000,15000);	//10---15秒 
			      getFifthSprite(R.drawable.pic4,15000,20000);  //15---20秒
			      getFifthSprite(R.drawable.pic5,20000,25000);  //20---25秒
			}
		});
    	
    	//这里仅仅是举例,当界面再次返回的时候,依旧显示图片更新的动画效果,即重新开始MediaPool
    	mPlayView.setOnViewAvailable(new onViewAvailable() {
			
			@Override
			public void viewAvailable(MediaPoolView v) {
				// TODO Auto-generated method stub
				Log.i(TAG,"is started==============>"+isStarted);
				if(isStarted){
				    
				      String picPath=SDKDir.TMP_DIR+"/"+"picname.jpg";   
				      mPlayView.startMediaPool(new MediaPoolProgressListener(),new MediaPoolCompleted());
					  mPlayView.obtainBitmapSprite(BitmapFactory.decodeFile(picPath));
				      
				      slideEffectArray=new ArrayList<SlideEffect>();
				      
						//这里同时获取多个,只是不显示出来.
				      getFifthSprite(R.drawable.pic1,0,5000);  		//1--5秒.
				      getFifthSprite(R.drawable.pic2,5000,10000);  //5--10秒.
				      getFifthSprite(R.drawable.pic3,10000,15000);	//10---15秒 
				      getFifthSprite(R.drawable.pic4,15000,20000);  //15---20秒
				      getFifthSprite(R.drawable.pic5,20000,25000);  //20---25秒
				}
			}
		});
		
    }
    private boolean isStarted=false; //是否已经播放过了.
    private void getFifthSprite(int resId,long startMS,long endMS)
    {
    	ISprite item=mPlayView.obtainBitmapSprite(BitmapFactory.decodeResource(getResources(), resId));
		SlideEffect  slide=new SlideEffect(item, 25, startMS, endMS, true);
		slideEffectArray.add(slide);
		
    }
    //MediaPool完成时的回调.
    private class MediaPoolCompleted implements onMediaPoolCompletedListener
    {

		@Override
		public void onCompleted(MediaPool v) {
			// TODO Auto-generated method stub
			
			if(isDestorying==false){
				if(SDKFileUtils.fileExist(dstPath)){
			    	findViewById(R.id.id_mediapool_saveplay).setVisibility(View.VISIBLE);
				}
				toastStop();
			}
		}
    }
    //MediaPool进度回调.
    private class MediaPoolProgressListener implements onMediaPoolProgressListener
    {

		@Override
		public void onProgress(MediaPool v, long currentTimeUs) {  //单位是微妙
			// TODO Auto-generated method stub
//			  Log.i(TAG,"MediaPoolProgressListener: us:"+currentTimeUs);
			
			  if(currentTimeUs>=26*1000*1000)  //26秒.多出一秒,让图片走完.
			  {
				  mPlayView.stopMediaPool();
			  }
			  
//			  Log.i(TAG,"current time Us "+currentTimeUs);
			  
			  if(slideEffectArray!=null && slideEffectArray.size()>0){
				  for(SlideEffect item: slideEffectArray){
					  item.run(currentTimeUs/1000);
				  }
			  }
		}
    }
    private void toastStop()
    {
    	Toast.makeText(getApplicationContext(), "录制已停止!!", Toast.LENGTH_SHORT).show();
    }
    
    boolean isDestorying=false;  //是否正在销毁, 因为销毁会停止MediaPool
    @Override
    protected void onDestroy() {
    	// TODO Auto-generated method stub
    	super.onDestroy();
    	
    	
    	isDestorying=true;
    	if(slideEffectArray!=null){
	   		 slideEffectArray.clear();
	   		 slideEffectArray=null;
    	}
    	
    	if(mPlayView!=null){
    		mPlayView.stopMediaPool();
    		mPlayView=null;        		   
    	}
    	
    	if(SDKFileUtils.fileExist(dstPath)){
    		SDKFileUtils.deleteFile(dstPath);
        }
    }
}
