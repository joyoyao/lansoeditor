package com.example.lansongeditordemo;


import java.io.IOException;
import java.util.Locale;

import jp.co.cyberagent.lansongsdk.gpuimage.GPUImageFilter;
import jp.co.cyberagent.lansongsdk.gpuimage.GPUImageSepiaFilter;

import com.example.lansongeditordemo.view.MediaPoolView;
import com.lansoeditor.demo.R;
import com.lansosdk.box.AudioEncodeDecode;
import com.lansosdk.box.MediaPool;
import com.lansosdk.box.MediaPoolUpdateMode;
import com.lansosdk.box.AudioMixManager;
import com.lansosdk.box.VideoSprite;
import com.lansosdk.box.ViewSprite;
import com.lansosdk.box.ISprite;
import com.lansosdk.box.onMediaPoolCompletedListener;
import com.lansosdk.box.onMediaPoolProgressListener;
import com.lansosdk.box.onMediaPoolSizeChangedListener;
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
 * 演示: 使用MediaPool来实现 视频和视频的实时叠加.
 * 
 * 流程是: 
 * 先创建一个MediaPool,获取主VideoSprite,在播放过程中,再次获取一个VideoSprite然后可以调节SeekBar来对
 * Sprite的每个参数进行调节.
 * 
 * 可以调节的有:平移,旋转,缩放,RGBA值,显示/不显示(闪烁)效果.
 * 实际使用中, 可用这些属性来扩展一些功能.
 * 
 * 比如 调节另一个视频的RGBA中的A值来实现透明叠加效果,类似MV的效果.
 * 
 * 比如 调节另一个视频的平移,缩放,旋转来实现贴纸的效果.
 * 
 */
public class VideoVideoRealTimeActivity extends Activity implements OnSeekBarChangeListener {
    private static final String TAG = "VideoActivity";

    private String mVideoPath;

    private MediaPoolView mPlayView;
    
    private MediaPlayer mplayer=null;
    private VPlayer  vplayer=null;
    
    private MediaPlayer mplayer2=null;
    
    private VideoSprite subVideoSprite=null;
    private ISprite  mSpriteMain=null;
    
    private String editTmpPath=null;
    private String dstPath=null;

    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
		 Thread.setDefaultUncaughtExceptionHandler(new snoCrashHandler());
        setContentView(R.layout.mediapool_layout);
        
        
        mVideoPath = getIntent().getStringExtra("videopath");
        mPlayView = (MediaPoolView) findViewById(R.id.mediapool_view);
        
        initSeekBar(R.id.id_mediapool_skbar_rotate,360); //角度是旋转360度.
        initSeekBar(R.id.id_mediapool_skbar_move,100);   //move的百分比暂时没有用到,举例而已.
        initSeekBar(R.id.id_mediapool_skbar_scale,800);   //这里设置最大可放大8倍
        
        initSeekBar(R.id.id_mediapool_skbar_red,100);  //red最大为100
        initSeekBar(R.id.id_mediapool_skbar_green,100);
        initSeekBar(R.id.id_mediapool_skbar_blue,100);
        initSeekBar(R.id.id_mediapool_skbar_alpha,100);
        
        findViewById(R.id.id_mediapool_saveplay).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				 if(SDKFileUtils.fileExist(dstPath)){
		   			 	Intent intent=new Intent(VideoVideoRealTimeActivity.this,VideoPlayerActivity.class);
			    	    	intent.putExtra("videopath", dstPath);
			    	    	startActivity(intent);
		   		 }else{
		   			 Toast.makeText(VideoVideoRealTimeActivity.this, "目标文件不存在", Toast.LENGTH_SHORT).show();
		   		 }
			}
		});
        
    	findViewById(R.id.id_mediapool_saveplay).setVisibility(View.GONE);

        //在手机的/sdcard/lansongBox/路径下创建一个文件名,用来保存生成的视频文件,(在onDestroy中删除)
        editTmpPath=SDKFileUtils.newMp4PathInBox();
        dstPath=SDKFileUtils.newMp4PathInBox();
        
        //增加提示缩放到480的文字.
        DemoUtils.showScale480HintDialog(VideoVideoRealTimeActivity.this);
    }
    private void initSeekBar(int resId,int maxvalue)
    {
    	SeekBar   skbar=(SeekBar)findViewById(resId);
           skbar.setOnSeekBarChangeListener(this);
           skbar.setMax(maxvalue);
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
					if(mPlayView!=null && mPlayView.isRunning()){
						mPlayView.stopMediaPool();
						
						toastStop();
						
						if(SDKFileUtils.fileExist(editTmpPath)){
							boolean ret=VideoEditor.encoderAddAudio(mVideoPath,editTmpPath,SDKDir.TMP_DIR,dstPath);
							if(!ret){
								dstPath=editTmpPath;
							}else
								SDKFileUtils.deleteFile(editTmpPath);
							
							findViewById(R.id.id_mediapool_saveplay).setVisibility(View.VISIBLE);
						}
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
    	MediaInfo info=new MediaInfo(mVideoPath,false);
    	info.prepare();
		
    	// 设置MediaPool的刷新模式,默认 {@link MediaPool.UpdateMode#ALL_VIDEO_READY};
    	mPlayView.setUpdateMode(MediaPoolUpdateMode.ALL_VIDEO_READY,25);
//    	mPlayView.setUpdateMode(MediaPoolUpdateMode.FIRST_VIDEO_READY, 25);
    	
    	
    	if(DemoCfg.ENCODE){
    		//设置使能 实时录制, 即把正在MediaPool中呈现的画面实时的保存下来,起到所见即所得的模式
    		mPlayView.setRealEncodeEnable(480,480,1000000,(int)info.vFrameRate,editTmpPath);
    	}
    	//设置当前MediaPool的宽度和高度,并把宽度自动缩放到父view的宽度,然后等比例调整高度.
    	mPlayView.setMediaPoolSize(480,480,new onMediaPoolSizeChangedListener() {
			
			@Override
			public void onSizeChanged(int viewWidth, int viewHeight) {
				// TODO Auto-generated method stub
				
				// 开始mediaPool的渲染线程. 
//				mPlayView.startMediaPool(new MediaPoolProgressListener(),new MediaPoolCompleted());
				mPlayView.startMediaPool(null,null); //这里为了演示方便, 设置为null,当然你也可以使用上面这句,然后把当前行屏蔽, 这样会调用两个回调,在回调中实现可以根据时间戳的关系来设计各种效果.
				
				//获取一个主视频的 VideoSprite
				mSpriteMain=mPlayView.obtainMainVideoSprite(mplayer.getVideoWidth(),mplayer.getVideoHeight());
				if(mSpriteMain!=null){
					mplayer.setSurface(new Surface(mSpriteMain.getVideoTexture()));
				}
				mplayer.start();
//				startVPlayer();
				startPlayer2();
			}
		});
    }
    private class MediaPoolCompleted implements onMediaPoolCompletedListener
    {

		@Override
		public void onCompleted(MediaPool v) {
			// TODO Auto-generated method stub
			if(isDestorying==false){
				
			}
		}
    }
    boolean isFirstRemove=false;
    private class MediaPoolProgressListener implements onMediaPoolProgressListener
    {

		@Override
		public void onProgress(MediaPool v, long currentTimeUs) {
			// TODO Auto-generated method stub
//			  Log.i(TAG,"MediaPoolProgressListener: us:"+currentTimeUs);
			
			//这里根据每帧的效果,设置当大于10秒时, 删除第二个视频画面.
			  if(currentTimeUs>=3*1000*1000 && subVideoSprite!=null && isFirstRemove==false)  
			  {
					  if(mPlayView!=null){
						  mPlayView.removeSprite(subVideoSprite);
						  subVideoSprite=null;
							
							if(mplayer2!=null){
								mplayer2.stop();
								mplayer2.release();
								mplayer2=null;
							}
					  }
					  isFirstRemove=true;
					  Log.i(TAG,"subVideoSprite removed: !!!!!!!!!:");
			  }
			  
			  if(currentTimeUs>=6*1000*1000&& mplayer2==null)  
			  {
					  if(mPlayView!=null){
						  startPlayer2();
						  Log.i(TAG,"subVideoSprite restart!!!!!!!!:");
					  }
			  }
			  
		}
    }
    
    private void startPlayer2()
    {
    	mplayer2=new MediaPlayer();
    	try {
    		mplayer2.setDataSource(mVideoPath);
		}catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	mplayer2.setOnPreparedListener(new OnPreparedListener() {
			
			@Override
			public void onPrepared(MediaPlayer mp) {
				// TODO Auto-generated method stub
				
				// 获取一个VideoSprite
				subVideoSprite=mPlayView.obtainSubVideoSprite(mp.getVideoWidth(),mp.getVideoHeight());
				Log.i(TAG,"sub video sprite ....obtain..");
				if(subVideoSprite!=null){
					mplayer2.setSurface(new Surface(subVideoSprite.getVideoTexture()));	
					subVideoSprite.setScale(0.5f);  //这里先缩小一半
				}
				mplayer2.start();
			}
		});
    	mplayer2.setOnErrorListener(new OnErrorListener() {
			
			@Override
			public boolean onError(MediaPlayer mp, int what, int extra) {
				// TODO Auto-generated method stub
				return false;
			}
		});
    	mplayer2.setOnInfoListener( new OnInfoListener() {
			
			@Override
			public boolean onInfo(MediaPlayer mp, int what, int extra) {
				// TODO Auto-generated method stub
				return false;
			}
		});
		mplayer2.prepareAsync();
    }
    private void startVPlayer()
    {
    	vplayer=new VPlayer(this);
    	vplayer.setVideoPath(mVideoPath);
    	vplayer.setOnPreparedListener(new OnPlayerPreparedListener() {
			
			@Override
			public void onPrepared(IMediaPlayer mp) {
				// TODO Auto-generated method stub
				
				
				subVideoSprite=mPlayView.obtainSubVideoSprite(mp.getVideoWidth(),mp.getVideoHeight());
				if(subVideoSprite!=null){
					vplayer.setSurface(new Surface(subVideoSprite.getVideoTexture()));	
					subVideoSprite.setScale(0.5f);
				}
				vplayer.start();
			}
		});
    	vplayer.prepareAsync();
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
			
			if(mplayer2!=null){
				mplayer2.stop();
				mplayer2.release();
				mplayer2=null;
			}
			
			if(vplayer!=null){
				vplayer.stop();
				vplayer.release();
				vplayer=null;
			}
			
			if(mPlayView!=null){
				mPlayView.stopMediaPool();
				mPlayView=null;        		   
			}
			if(SDKFileUtils.fileExist(dstPath)){
				SDKFileUtils.deleteFile(dstPath);
		    }
		    if(SDKFileUtils.fileExist(editTmpPath)){
		    	SDKFileUtils.deleteFile(editTmpPath);
		    } 
	}
    private float xpos=0,ypos=0;
    
    /**
     * 提示:实际使用中没有主次之分, 只要是继承自ISprite的对象(FilterSprite除外),都可以调节,这里仅仅是举例
     */
	@Override
	public void onProgressChanged(SeekBar seekBar, int progress,
			boolean fromUser) {
		// TODO Auto-generated method stub
		switch (seekBar.getId()) {
			case R.id.id_mediapool_skbar_rotate:
				if(subVideoSprite!=null){
					subVideoSprite.setRotate(progress);
				}
				break;
			case R.id.id_mediapool_skbar_move:
					if(subVideoSprite!=null){
						 xpos+=10;
						 ypos+=10;
						 
						 if(xpos>mPlayView.getViewWidth())
							 xpos=0;
						 if(ypos>mPlayView.getViewWidth())
							 ypos=0;
						 subVideoSprite.setPosition(xpos, ypos);
					}
				break;				
			case R.id.id_mediapool_skbar_scale:
				if(subVideoSprite!=null){
					float scale=(float)progress/100;
					subVideoSprite.setScale(scale);
				}
			break;		
			case R.id.id_mediapool_skbar_red:
					if(subVideoSprite!=null){
						subVideoSprite.setRedPercent(progress);  //设置每个RGBA的比例,默认是1
					}
				break;

			case R.id.id_mediapool_skbar_green:
					if(subVideoSprite!=null){
						subVideoSprite.setGreenPercent(progress);
					}
				break;

			case R.id.id_mediapool_skbar_blue:
					if(subVideoSprite!=null){
						subVideoSprite.setBluePercent(progress);
					}
				break;

			case R.id.id_mediapool_skbar_alpha:
					if(subVideoSprite!=null){
						subVideoSprite.setAlphaPercent(progress);
					}
				break;
				
			default:
				break;
		}
	}
	@Override
	public void onStartTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}
	@Override
	public void onStopTrackingTouch(SeekBar seekBar) {
		// TODO Auto-generated method stub
		
	}
}
