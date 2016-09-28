package com.example.lansongeditordemo;


import java.io.IOException;
import java.util.Calendar;
import java.util.Locale;

import jp.co.cyberagent.lansongsdk.gpuimage.GPUImageFilter;
import jp.co.cyberagent.lansongsdk.gpuimage.GPUImageSepiaFilter;

import com.example.lansongeditordemo.GPUImageFilterTools.FilterAdjuster;
import com.example.lansongeditordemo.GPUImageFilterTools.OnGpuImageFilterChosenListener;
import com.example.lansongeditordemo.view.MediaPoolView;
import com.lansoeditor.demo.R;
import com.lansosdk.box.AudioEncodeDecode;
import com.lansosdk.box.LanSongBoxVersion;
import com.lansosdk.box.MediaPool;
import com.lansosdk.box.MediaPoolUpdateMode;
import com.lansosdk.box.AudioMixManager;
import com.lansosdk.box.VideoFilterSprite;
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
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
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
 * 演示滤镜模块的 FilterSprite的使用, 可以在播放过程中切换滤镜,
 * 在滤镜处理过程中, 增加其他的sprite,比如增加一个BitmapSprite和 ViewSprite等等.
 * 
 * 流程: 从layout中得到MediaPoolView,并从MediaPoolView中获取多个sprite,
 * 并对sprite进行滤镜, 缩放等操作.
 *
 *
 */
public class FilterSpriteRealTimeActivity extends Activity {
    private static final String TAG = "VideoActivity";

    private String mVideoPath;

    private MediaPoolView mMediaPoolView;
    
    private MediaPlayer mplayer=null;
    
    private VideoSprite subVideoSprite=null;
    private VideoFilterSprite  filterSprite=null;
    
    private SeekBar skbarFilterAdjuster;
    
    private String editTmpPath=null;
    private String dstPath=null;

    
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
		 Thread.setDefaultUncaughtExceptionHandler(new snoCrashHandler());
        setContentView(R.layout.filter_sprite_demo_layout);
        
        
        mVideoPath = getIntent().getStringExtra("videopath");
        mMediaPoolView = (MediaPoolView) findViewById(R.id.id_filtersprite_demo_view);
        
        skbarFilterAdjuster=(SeekBar)findViewById(R.id.id_filtersprite_demo_seek1);
        
        skbarFilterAdjuster.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
			
			@Override
			public void onStopTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onStartTrackingTouch(SeekBar seekBar) {
				// TODO Auto-generated method stub
				
			}
			
			@Override
			public void onProgressChanged(SeekBar seekBar, int progress,
					boolean fromUser) {
				// TODO Auto-generated method stub
				  if (mFilterAdjuster != null) {
			            mFilterAdjuster.adjust(progress);
			        }
			}
		});
        
        
        skbarFilterAdjuster.setMax(100);
        
        findViewById(R.id.id_filtersprite_demo_selectbtn).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				selectFilter();
			}
		});
        
        findViewById(R.id.id_filtersprite_saveplay).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				 if(SDKFileUtils.fileExist(dstPath)){
		   			 	Intent intent=new Intent(FilterSpriteRealTimeActivity.this,VideoPlayerActivity.class);
			    	    	intent.putExtra("videopath", dstPath);
			    	    	startActivity(intent);
		   		 }else{
		   			 Toast.makeText(FilterSpriteRealTimeActivity.this, "目标文件不存在", Toast.LENGTH_SHORT).show();
		   		 }
			}
		});

        //在手机的/sdcard/lansongBox/路径下创建一个文件名,用来保存生成的视频文件,(在onDestroy中删除)
        editTmpPath=SDKFileUtils.newMp4PathInBox();
        dstPath=SDKFileUtils.newMp4PathInBox();
        
        //增加提示缩放到480的文字.
        DemoUtils.showScale480HintDialog(FilterSpriteRealTimeActivity.this);
    }
    @Override
    protected void onResume() {
    	// TODO Auto-generated method stub
    	super.onResume();
    	new Handler().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				 startPlayVideo();
			}
		}, 100);
    }
    private FilterAdjuster mFilterAdjuster;
    
    /**
     * 选择滤镜效果, 
     */
    private void selectFilter()
    {
    	GPUImageFilterTools.showDialog(this, new OnGpuImageFilterChosenListener() {

            @Override
            public void onGpuImageFilterChosenListener(final GPUImageFilter filter) {
            	
            	//在这里通过MediaPool线程去切换 filterSprite的滤镜
	         	   if(mMediaPoolView.switchFilterTo(filterSprite,filter)){
	         		   mFilterAdjuster = new FilterAdjuster(filter);
	
	         		   //如果这个滤镜 可调, 显示可调节进度条.
	         		    findViewById(R.id.id_filtersprite_demo_seek1).setVisibility(
	         		            mFilterAdjuster.canAdjust() ? View.VISIBLE : View.GONE);
	         	   }
            }
        });
    }
    private void startPlayVideo()
    {
          if (mVideoPath != null){
        	  mplayer=new MediaPlayer();
        	  try {
				mplayer.setDataSource(mVideoPath);
				
			}  catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	  mplayer.setOnPreparedListener(new OnPreparedListener() {
				
				@Override
				public void onPrepared(MediaPlayer mp) {
					// TODO Auto-generated method stub
					start(mp);
				}
			});
        	  mplayer.setOnCompletionListener(new OnCompletionListener() {
				
				@Override
				public void onCompletion(MediaPlayer mp) {
					// TODO Auto-generated method stub
					if(mMediaPoolView!=null && mMediaPoolView.isRunning()){
						mMediaPoolView.stopMediaPool();
					}
				}
			});
        	  mplayer.prepareAsync();
          }
          else {
              Log.e(TAG, "Null Data Source\n");
              finish();
              return;
          }
    }
    private void start(MediaPlayer mp)
    {
    	MediaInfo info=new MediaInfo(mVideoPath);
    	info.prepare();
		
    	mMediaPoolView.setUpdateMode(MediaPoolUpdateMode.ALL_VIDEO_READY,25);
    	
    	if(DemoCfg.ENCODE){
//    		设置使能 实时保存, 即把正在MediaPool中呈现的画面实时的保存下来,实现所见即所得的模式
    		mMediaPoolView.setRealEncodeEnable(480,480,1000000,(int)info.vFrameRate,editTmpPath);
    	}
    	
    	//设置当前MediaPool的宽度和高度,并把宽度自动缩放到父view的宽度,然后等比例调整高度.
    	mMediaPoolView.setMediaPoolSize(480,480,new onMediaPoolSizeChangedListener() {
			
			@Override
			public void onSizeChanged(int viewWidth, int viewHeight) {
				// TODO Auto-generated method stub
				mMediaPoolView.startMediaPool(new MediaPoolProgressListener(),new MediaPoolCompleted());
				
				//先增加一个背景
//				addBackgroundBitmap();
			      
				/**
				 * 这里获取一个FilterSprite, 并把设置滤镜效果为GPUImageSepiaFilter滤镜.
				 */
				filterSprite=mMediaPoolView.obtainFilterSprite(mplayer.getVideoWidth(),mplayer.getVideoHeight(),new GPUImageFilter());
				
				if(filterSprite!=null){
					mplayer.setSurface(new Surface(filterSprite.getVideoTexture()));
				}
				mplayer.start();
			}
		});
    }
    private void addBackgroundBitmap()
    {
    	  DisplayMetrics dm = new DisplayMetrics();// 获取屏幕密度（方法2）
	       dm = getResources().getDisplayMetrics();
	        
	           
	      int screenWidth  = dm.widthPixels;	
	      String picPath=SDKDir.TMP_DIR+"/"+"picname.jpg";   
	      if(screenWidth>=1080){
	    	  CopyFileFromAssets.copy(getApplicationContext(), "pic1080x1080u2.jpg", SDKDir.TMP_DIR, "picname.jpg");
	      }  
	      else{
	    	  CopyFileFromAssets.copy(getApplicationContext(), "pic720x720.jpg", SDKDir.TMP_DIR, "picname.jpg");
	      }
	      //先 获取第一张Bitmap的Sprite, 因为是第一张,放在MediaPool中维护的数组的最下面, 认为是背景图片.
	      mMediaPoolView.obtainBitmapSprite(BitmapFactory.decodeFile(picPath));
    }
    //MediaPool完成后的回调.
    private class MediaPoolCompleted implements onMediaPoolCompletedListener
    {

		@Override
		public void onCompleted(MediaPool v) {
			// TODO Auto-generated method stub
			
			if(isDestorying==false){
					toastStop();
					
					if(SDKFileUtils.fileExist(editTmpPath)){
						boolean ret=VideoEditor.encoderAddAudio(mVideoPath,editTmpPath,SDKDir.TMP_DIR,dstPath);
						if(!ret){
							dstPath=editTmpPath;
						}else{
							SDKFileUtils.deleteFile(editTmpPath);	
						}
					}
			}
		}
    }
    //MediaPool进度回调.每一帧都返回一个回调.
    private class MediaPoolProgressListener implements onMediaPoolProgressListener
    {

		@Override
		public void onProgress(MediaPool v, long currentTimeUs) {
			// TODO Auto-generated method stub
//			  Log.i(TAG,"MediaPoolProgressListener: us:"+currentTimeUs);
			  if(currentTimeUs>=26*1000*1000)  //26秒.多出一秒,让图片走完.
			  {
				  mMediaPoolView.stopMediaPool();
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
		if(mplayer!=null){
			mplayer.stop();
			mplayer.release();
			mplayer=null;
		}
		if(mMediaPoolView!=null){
			mMediaPoolView.stopMediaPool();
			mMediaPoolView=null;        		   
		}
		 if(SDKFileUtils.fileExist(dstPath)){
			 SDKFileUtils.deleteFile(dstPath);
	     }
	     if(SDKFileUtils.fileExist(editTmpPath)){
	    	 SDKFileUtils.deleteFile(editTmpPath);
	     } 
    }

}
