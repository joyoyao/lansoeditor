package com.example.lansongeditordemo;



import java.io.IOException;

import com.example.lansongeditordemo.GPUImageFilterTools.FilterAdjuster;
import com.example.lansongeditordemo.GPUImageFilterTools.OnGpuImageFilterChosenListener;
import com.example.lansongeditordemo.view.FilterView;
import com.lansoeditor.demo.R;
import com.lansosdk.box.onFilterViewSizeChangedListener;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.SDKDir;
import com.lansosdk.videoeditor.SDKFileUtils;
import com.lansosdk.videoeditor.VideoEditor;

import jp.co.cyberagent.lansongsdk.gpuimage.GPUImageFilter;



import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.Surface;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.SeekBar.OnSeekBarChangeListener;

/**
 * 
 *  演示单独使用FilterView来做视频滤镜处理.
 *  FilterView 是单独用来视频滤镜的
 */
public class FilterRealTimeActivity extends Activity {
    private static final String TAG = "VideoActivity";

    private String mVideoPath;

    private FilterView mFilterView;
    private MediaPlayer mPlayer;
    
    private boolean mBackPressed;
    private SeekBar skbarFilterAdjuster;
    private String editTmpPath=null;
    private String dstPath=null;
    

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
		 Thread.setDefaultUncaughtExceptionHandler(new snoCrashHandler());
        setContentView(R.layout.filter_player_layout);
        
        mVideoPath = getIntent().getStringExtra("videopath");
        mFilterView = (FilterView) findViewById(R.id.video_view);
        
        skbarFilterAdjuster=(SeekBar)findViewById(R.id.id_player_seekbar1);
        
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
        
        findViewById(R.id.id_player_btnselectfilter).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				selectFilter();
			}
		});
        
        findViewById(R.id.id_filter_saveplay).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				 if(SDKFileUtils.fileExist(dstPath)){
		   			 	Intent intent=new Intent(FilterRealTimeActivity.this,VideoPlayerActivity.class);
			    	    	intent.putExtra("videopath", dstPath);
			    	    	startActivity(intent);
		   		 }else{
		   			 Toast.makeText(FilterRealTimeActivity.this, "目标文件不存在", Toast.LENGTH_SHORT).show();
		   		 }
			}
		});

        //在手机的/sdcard/lansongBox/路径下创建一个文件名,用来保存生成的视频文件,(在onDestroy中删除)
        editTmpPath=SDKFileUtils.newMp4PathInBox();
        dstPath=SDKFileUtils.newMp4PathInBox();
     
        //增加提示缩放到480的文字.
        DemoUtils.showScale480HintDialog(FilterRealTimeActivity.this);
    }
    boolean isStart;
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
		}, 1000);
    }
    private void startPlayVideo()
    {
    	  if (mVideoPath != null){
    		  mPlayer=new MediaPlayer();
        	  try {
        		  mPlayer.setDataSource(mVideoPath);
				
			}  catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
        	  mPlayer.setOnPreparedListener(new OnPreparedListener() {
				
				@Override
				public void onPrepared(MediaPlayer mp) {
					// TODO Auto-generated method stub
					start(mp);
				}
			});
        	  mPlayer.setOnCompletionListener(new OnCompletionListener() {
				
				@Override
				public void onCompletion(MediaPlayer mp) {
					// TODO Auto-generated method stub
					if(mFilterView.isRunning()){
						mFilterView.stop();
						
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
			});
        	  mPlayer.prepareAsync();
          }
          else {
              Log.e(TAG, "Null Data Source\n");
              finish();
              return;
          }
    }
    private void toastStop()
    {
    	Toast.makeText(getApplicationContext(), "录制已停止!!", Toast.LENGTH_SHORT).show();
    }
    
    private void start(MediaPlayer mp)
    {
    	MediaInfo info=new MediaInfo(mVideoPath);
    	info.prepare();
    	
    	if(DemoCfg.ENCODE){
    		mFilterView.setRealEncodeEnable(1000*1000,(int)info.vFrameRate,editTmpPath);
    	}
    	
    	//这里设置为等比例滤镜,实际int glwidth,int glheight的值可任意设置, 短视频一般是480x480的居多.
    	mFilterView.setFilterRenderSize(480,480,mp.getVideoWidth(),mp.getVideoHeight(),new onFilterViewSizeChangedListener() {
			
			@Override
			public void onSizeChanged(int viewWidth, int viewHeight) {
				// TODO Auto-generated method stub
				mFilterView.start();
				
				mPlayer.setSurface(mFilterView.getSurface());
				mPlayer.start();
			}
		});
    }
    @Override
    public void onBackPressed() {
        mBackPressed = true;
        super.onBackPressed();
    }
    @Override
    protected void onStop() {
        super.onStop();
        if (mBackPressed) {
            mFilterView.stop();
            
            if(mPlayer!=null){
        		mPlayer.stop();
            	mPlayer.release();
            	mPlayer=null;
        	}
            
        }
       
    }
    private FilterAdjuster mFilterAdjuster;
    private void selectFilter()
    {
    	GPUImageFilterTools.showDialog(this, new OnGpuImageFilterChosenListener() {

            @Override
            public void onGpuImageFilterChosenListener(final GPUImageFilter filter) 
            {
            	
         	   if(mFilterView.switchFilterTo(filter))//<----------------------//在这里切换滤镜
         	   {
         		   mFilterAdjuster = new FilterAdjuster(filter);

         		    findViewById(R.id.id_player_seekbar1).setVisibility(
         		            mFilterAdjuster.canAdjust() ? View.VISIBLE : View.GONE);
         	   }
            }
        });
    }
}
