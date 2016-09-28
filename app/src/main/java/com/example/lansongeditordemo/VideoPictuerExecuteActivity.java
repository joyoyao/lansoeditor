package com.example.lansongeditordemo;

import org.insta.IFRiseFilter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.example.lansongeditordemo.VideoEditDemoActivity.SubAsyncTask;
import com.example.lansongeditordemo.view.GLLinearLayout;
import com.lansoeditor.demo.R;
import com.lansosdk.box.BitmapSprite;
import com.lansosdk.box.MediaPool;
import com.lansosdk.box.MediaPoolVideoExecute;
import com.lansosdk.box.VideoSprite;
import com.lansosdk.box.ViewSprite;
import com.lansosdk.box.FilterExecute;
import com.lansosdk.box.ScaleExecute;
import com.lansosdk.box.onFilterExecuteCompletedListener;
import com.lansosdk.box.onFilterExecuteProssListener;
import com.lansosdk.box.onMediaPoolCompletedListener;
import com.lansosdk.box.onMediaPoolProgressListener;
import com.lansosdk.box.onScaleCompletedListener;
import com.lansosdk.box.onScaleProgressListener;
import com.lansosdk.videoeditor.LanSoEditor;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.SDKDir;
import com.lansosdk.videoeditor.SDKFileUtils;
import com.lansosdk.videoeditor.VideoEditor;
import com.lansosdk.videoeditor.onVideoEditorProgressListener;

/**
 * 演示: 使用MediaPool在后台执行视频和视频的叠加处理.
 * 
 * 适用在 一些UI界面需要用户手动操作UI界面,比如旋转叠加的视频等,增加图片后旋转图片等,这些UI交互完成后, 
 * 记录下用户的操作信息,但需要统一处理时,通过此类来在后台执行.
 * 
 * 流程:通过MediaPoolVideoExecute来实现视频的编辑处理,
 * 效果:建立一个MediaPool后,获取VideoSprite让其播放,在播放过程中,向里面增加两个图片和一个UI,
 * 其中给一个图片移动位置,并在3秒处放大一倍,在6秒处消失,处理中实时的形成视频等
 * 
 *
 */
public class VideoPictuerExecuteActivity extends Activity{

	String videoPath=null;
	ProgressDialog  mProgressDialog;
	int videoDuration;
	boolean isRuned=false;
	MediaInfo   mMediaInfo;
	TextView tvProgressHint;
	 TextView tvHint;
	    private String editTmpPath=null;
	    private String dstPath=null;
	    
	    
	private static final String TAG="MediaPoolExecuteActivity";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		
		super.onCreate(savedInstanceState);
		Thread.setDefaultUncaughtExceptionHandler(new snoCrashHandler());
		 
		 videoPath=getIntent().getStringExtra("videopath");
		 mMediaInfo=new MediaInfo(videoPath);
		 mMediaInfo.prepare();
		 
		 setContentView(R.layout.video_edit_demo_layout);
		 tvHint=(TextView)findViewById(R.id.id_video_editor_hint);
		 
		 tvHint.setText(R.string.mediapoolexecute_demo_hint);
   
		 tvProgressHint=(TextView)findViewById(R.id.id_video_edit_progress_hint);
		 
	       findViewById(R.id.id_video_edit_btn).setOnClickListener(new OnClickListener() {
				
				@Override
				public void onClick(View v) {
					
					if(mMediaInfo.vDuration>=60*1000){//大于60秒
						showHintDialog();
					}else{
						testMediaPoolExecute();
					}
				}
			});
       
       findViewById(R.id.id_video_edit_btn2).setEnabled(false);
       findViewById(R.id.id_video_edit_btn2).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(SDKFileUtils.fileExist(dstPath)){
					Intent intent=new Intent(VideoPictuerExecuteActivity.this,VideoPlayerActivity.class);
	    	    	intent.putExtra("videopath", dstPath);
	    	    	startActivity(intent);
				}else{
					 Toast.makeText(VideoPictuerExecuteActivity.this, "目标文件不存在", Toast.LENGTH_SHORT).show();
				}
			}
		});

       //在手机的/sdcard/lansongBox/路径下创建一个文件名,用来保存生成的视频文件,(在onDestroy中删除)
       editTmpPath=SDKFileUtils.newMp4PathInBox();
       dstPath=SDKFileUtils.newMp4PathInBox();
	}
   @Override
    protected void onDestroy() {
    	// TODO Auto-generated method stub
    	super.onDestroy();
    	
    	if(vMediaPool!=null){
    		vMediaPool.release();
    		vMediaPool=null;
    	}
    	   if(SDKFileUtils.fileExist(dstPath)){
    		   SDKFileUtils.deleteFile(dstPath);
           }
           if(SDKFileUtils.fileExist(editTmpPath)){
        	   SDKFileUtils.deleteFile(editTmpPath);
           } 
    }
	   
	VideoEditor mVideoEditer;
	BitmapSprite bitmapSprite=null;
	private ViewSprite mViewSprite=null;
	MediaPoolVideoExecute  vMediaPool=null;
	private boolean isExecuting=false;
	private void showHintDialog()
	{
		new AlertDialog.Builder(this)
		.setTitle("提示")
		.setMessage("视频过大,可能会需要一段时间,您确定要处理吗?")
        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				testMediaPoolExecute();
			}
		})
		.setNegativeButton("取消", null)
        .show();
	}
	
	private void testMediaPoolExecute()
	{
		if(isExecuting)
			return ;
		
		isExecuting=true;
		//创建在后台调用MediaPool来处理视频的构造方法.
		 vMediaPool=new MediaPoolVideoExecute(VideoPictuerExecuteActivity.this,videoPath,480,480,25,1000000,editTmpPath);
		
		 
		 vMediaPool.setUseMainVideoPts(true); //使用主视频的pts作为目标视频的pts
		 
		 //设置MediaPool处理的进度监听, 回传的currentTimeUs单位是微秒.
		vMediaPool.setMediaPoolProgressListener(new onMediaPoolProgressListener() {
			
			@Override
			public void onProgress(MediaPool v, long currentTimeUs) {
				// TODO Auto-generated method stub
				tvProgressHint.setText(String.valueOf(currentTimeUs));
			
				//6秒后消失
				if(currentTimeUs>6000000 && bitmapSprite!=null)  
					v.removeSprite(bitmapSprite);
				
				//3秒的时候,放大一倍.
				if(currentTimeUs>3000000 && bitmapSprite!=null)  
					bitmapSprite.setScale(2.0f);
			}
		});
		//设置MediaPool完成后的监听.
		vMediaPool.setMediaPoolCompletedListener(new onMediaPoolCompletedListener() {
			
			@Override
			public void onCompleted(MediaPool v) {
				// TODO Auto-generated method stub
				tvProgressHint.setText("MediaPoolExecute Completed!!!");
				
				isExecuting=false;
				
				if(SDKFileUtils.fileExist(editTmpPath)){
					boolean ret=VideoEditor.encoderAddAudio(videoPath, editTmpPath,SDKDir.TMP_DIR,dstPath);
					if(ret==false){
						dstPath=editTmpPath; //没有声音的时候,临时文件为目标文件.
					}
				}
				  findViewById(R.id.id_video_edit_btn2).setEnabled(true);
			}
		});
		vMediaPool.startMediaPool();
		
		//一下是在处理过程中, 增加的几个sprite, 来实现视频在播放过程中叠加别的一些媒体, 像图片, 文字等.
		
//		VideoSprite sprite=vMediaPool.obtainVideoSprite("/sdcard/lanso/editscenecat.mp4",480,480);//临时调试使用.
//		sprite.setScale(30);
		
		//向其中增加一个图片
		bitmapSprite=vMediaPool.obtainBitmapSprite(BitmapFactory.decodeResource(getResources(), R.drawable.ic_launcher));
		bitmapSprite.setPosition(300, 200);
		
		//增加一个笑脸
		vMediaPool.obtainBitmapSprite(BitmapFactory.decodeResource(getResources(), R.drawable.xiaolian));	
		
//		mCanvasSprite=vMediaPool.obtainViewSprite();
//        mGLLinearLayout.setViewSprite(mCanvasSprite);
//        mGLLinearLayout.invalidate();
        
	}
}	