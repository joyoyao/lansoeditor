package com.example.lansongeditordemo;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.atomic.AtomicInteger;



import jp.co.cyberagent.lansongsdk.gpuimage.GPUImageFilter;

import com.example.lansong.animview.JumpingBeans;
import com.example.lansong.animview.MatchTextView;
import com.example.lansong.animview.NumberProgressBar;
import com.example.lansong.animview.OnProgressBarListener;
import com.example.lansong.animview.PaintConstants;
import com.example.lansongeditordemo.view.GLRelativeLayout;
import com.example.lansongeditordemo.view.MediaPoolView;
import com.lansoeditor.demo.R;
import com.lansosdk.box.ViewSprite;
import com.lansosdk.box.ISprite;
import com.lansosdk.box.onMediaPoolSizeChangedListener;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.SDKDir;
import com.lansosdk.videoeditor.SDKFileUtils;
import com.lansosdk.videoeditor.VideoEditor;

import android.app.ActionBar.LayoutParams;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.opengl.GLSurfaceView;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.ViewPager;
import android.text.Layout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.View.OnClickListener;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

/**
 *  演示: 视频和UI界面的 实时叠加.
 *  
 *  演示UI设计中经常使用的TextView,Button,ImageView,进度条等在视频中的叠加,
 *  可以实时交互并实时保存到视频中.
 * 
 *  您实际项目中可任意发挥,构建炫酷或浪漫的视频.
 *  
 *  流程是: 建立一个MediaPool,从中获取视频VideoSprite,
 *  然后获取一个ViewSprite设置到GLRelativelayout中,实时的把GLRelativeLayout中的画面绘制到
 *  OpenGL中.
 *
 */
public class VViewCommonWidgetActivity extends Activity{
    private static final String TAG = "VideoActivity";

    private String mVideoPath;

    private MediaPoolView mPlayView;
    
    private MediaPlayer mplayer=null;
    private MediaPlayer mplayer2=null;
    
    private ISprite  mSpriteMain=null;
    private ViewSprite mViewSprite=null;
    
//    
    private String editTmpPath=null;
    private String dstPath=null;

    /**
     * 自定义的RelativeLayout,重载了其中的onDraw方法,用来把这个layout中的画面绘制到MediaPool中.
     * 
     */
    private GLRelativeLayout mGLRelativeLayout;
    private TextView  tvGl,tvGl2;
    private JumpingBeans jumpingBeans1, jumpingBeans2;
    private MatchTextView mMatchTextView;
    private boolean matchShowing=true;
    @Override
    protected void onCreate(Bundle savedInstanceState) 
    {
        super.onCreate(savedInstanceState);
		 Thread.setDefaultUncaughtExceptionHandler(new snoCrashHandler());
        setContentView(R.layout.vvcommon_widget_layout);
        
        
        mVideoPath = getIntent().getStringExtra("videopath");
        mPlayView = (MediaPoolView) findViewById(R.id.id_vview_realtime_mediapool_view);
        
        mGLRelativeLayout=(GLRelativeLayout)findViewById(R.id.id_vview_realtime_gllayout);
        tvGl=(TextView)findViewById(R.id.id_gllayout_tv);
        tvGl2=(TextView)findViewById(R.id.id_gllayout_tv2);
        mMatchTextView = (MatchTextView) findViewById(R.id.mMatchTextView);
        
        
        findViewById(R.id.id_gl_testbtn).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				if(matchShowing){
					mMatchTextView.hide();
					matchShowing=false;
				}else {
					mMatchTextView.show();
					matchShowing=true;
				}
			}
		});
        findViewById(R.id.id_vview_realtime_saveplay).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				 if(SDKFileUtils.fileExist(dstPath)){
		   			 	Intent intent=new Intent(VViewCommonWidgetActivity.this,VideoPlayerActivity.class);
			    	    	intent.putExtra("videopath", dstPath);
			    	    	startActivity(intent);
		   		 }else{
		   			 Toast.makeText(VViewCommonWidgetActivity.this, "目标文件不存在", Toast.LENGTH_SHORT).show();
		   		 }
			}
		});
    	findViewById(R.id.id_vview_realtime_saveplay).setVisibility(View.GONE);
    	

        //在手机的/sdcard/lansongBox/路径下创建一个文件名,用来保存生成的视频文件,(在onDestroy中删除)
        editTmpPath=SDKFileUtils.newMp4PathInBox();
            dstPath=SDKFileUtils.newMp4PathInBox();
            
	    jumpingBeans2 = JumpingBeans.with(tvGl2)
	            .makeTextJump(0, tvGl2.getText().toString().indexOf(' '))
	            .setIsWave(false)
	            .setLoopDuration(1000)
	            .build();
		
	    mNumberBar = (NumberProgressBar)findViewById(R.id.numberbar1);
        mNumberBar.setOnProgressBarListener(new OnProgressBarListener() {
			
			@Override
			public void onProgressChange(int current, int max) {
				// TODO Auto-generated method stub
				 if(current == max) {
			            mNumberBar.setProgress(0);
			     }
			}
		});
        startNumberBar();
        
        //增加提示缩放到480的文字.
        DemoUtils.showScale480HintDialog(VViewCommonWidgetActivity.this);
    }
    private void startNumberBar()
    {
    	numberBarTimer = new Timer();
        numberBarTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mNumberBar.incrementProgressBy(1);
                    }
                });
            }
        }, 1000, 100);
    }
    private Timer numberBarTimer;
    private NumberProgressBar mNumberBar;
    
    @Override
    protected void onResume() {
    	// TODO Auto-generated method stub
    	super.onResume();
    	findViewById(R.id.id_vview_realtime_saveplay).setVisibility(View.GONE);
    	new Handler().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				 startPlayVideo();
			}
		}, 100);
    }
    /**
     * 这里采用Android原生的MediaPlayer作为视频源,实际您可以使用任意的带有可以设置Surface视频播放器
     * 做为VideoSprite的画面输入源.
     */
    private void startPlayVideo()
    {
          if (mVideoPath != null)
          {
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
					
					Log.i(TAG,"media player is completion!!!!");
					if(mPlayView!=null && mPlayView.isRunning()){
						//播放完成后,停止MediaPool的操作.
						mPlayView.stopMediaPool();
						
						toastStop();
						
						if(SDKFileUtils.fileExist(editTmpPath)){
							boolean ret=VideoEditor.encoderAddAudio(mVideoPath,editTmpPath,SDKDir.TMP_DIR, dstPath);
							if(!ret){
								dstPath=editTmpPath;
							}else{
								SDKFileUtils.deleteFile(editTmpPath);	
							}
							
					    	findViewById(R.id.id_vview_realtime_saveplay).setVisibility(View.VISIBLE);
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
    	
    	//建立MediaPool,病设置录制的各种参数, 宽度和高度,码率,帧率,保存路径等.
    	if(DemoCfg.ENCODE){
    		mPlayView.setRealEncodeEnable(480,480,1000000,(int)info.vFrameRate,editTmpPath);
    	}
    	//设置MediaPool的各种参数.
    	mPlayView.setMediaPoolSize(480,480,new onMediaPoolSizeChangedListener() {
			
			@Override
			public void onSizeChanged(int viewWidth, int viewHeight) {
				// TODO Auto-generated method stub
				mPlayView.startMediaPool(null,null); //这里为了代码清晰,省略了进度和完成监听,如您想监听进度和结果,可以参考VideoPictureRealTimeActivity.java
				
				mSpriteMain=mPlayView.obtainMainVideoSprite(mplayer.getVideoWidth(),mplayer.getVideoHeight());
				if(mSpriteMain!=null){
					mplayer.setSurface(new Surface(mSpriteMain.getVideoTexture()));
				}
				mplayer.start();
				addViewSprite();
//				startPlayer2();
			}
		});
    }
    
    private int testCount=0;
    private Handler  testHandler=new Handler();
    private Runnable testRunnable=new Runnable() {
		
		@Override
		public void run() {
			// TODO Auto-generated method stub
			testCount++;
			if(tvGl!=null){
				tvGl.setText("我是TextView,我演示文字变化"+String.valueOf(testCount));
				mGLRelativeLayout.invalidate();
			}	
			testHandler.postDelayed(testRunnable, 1000);
		}
	};
    /**
     * 从MediaPool中获取一个ViewSprite,然后设置到GLRelativeLayout中.
     */
    private void addViewSprite()
    {
    	mViewSprite=mPlayView.obtainViewSprite();
        mGLRelativeLayout.setViewSprite(mViewSprite);
        mGLRelativeLayout.invalidate(); //更新下Relativelayout
        
        ViewGroup.LayoutParams  params=mGLRelativeLayout.getLayoutParams();
        params.height=mViewSprite.getHeight();  //因为布局时, 宽度一致, 这里调整高度,让他们一致.
        
        mGLRelativeLayout.setLayoutParams(params);
        new Handler().postDelayed(new Runnable() {
			
			@Override
			public void run() {
				// TODO Auto-generated method stub
				testHandler.postDelayed(testRunnable, 1000);
			}
		}, 2000);
    }
    private void toastStop()
    {
    	Toast.makeText(getApplicationContext(), "录制已停止!!", Toast.LENGTH_SHORT).show();
    }
    @Override
    protected void onDestroy() {
    	// TODO Auto-generated method stub
    	super.onDestroy();
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
          if(numberBarTimer!=null){
        	  numberBarTimer.cancel();
        	  numberBarTimer=null;
          }
    }
	
}
