package com.example.lansongeditordemo;

import org.insta.IFRiseFilter;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.lansoeditor.demo.R;
import com.lansosdk.box.BitmapSprite;
import com.lansosdk.box.FilterExecute;
import com.lansosdk.box.ScaleExecute;
import com.lansosdk.box.onFilterExecuteCompletedListener;
import com.lansosdk.box.onFilterExecuteProssListener;
import com.lansosdk.box.onScaleCompletedListener;
import com.lansosdk.box.onScaleProgressListener;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.SDKDir;
import com.lansosdk.videoeditor.SDKFileUtils;
import com.lansosdk.videoeditor.VideoEditor;

/**
 * 此演示功能:在后台单独处理视频滤镜.适用在当不需要UI界面做滤镜预览的场合.比如不经过滤镜预览或没有预览完毕,就可以做视频滤镜处理,这样的场合使用!
 * 
 * 演示流程是: 直接调用FilterExecute来对视频做滤镜处理,可以在滤镜过程中,实时的保存.
 *
 */
public class FilterExecuteActivity extends Activity{

	String videoPath=null;
	ProgressDialog  mProgressDialog;
	int videoDuration;
	boolean isRuned=false;
	MediaInfo   mMediaInfo;
	TextView tvProgressHint;
	private String editTmpPath=null;
	private String dstPath=null;
	 TextView tvHint;
	 
	private static final String TAG="FilterExecuteActivity";
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
		 
		 tvHint.setText(R.string.filterexecute_demo_hint);
   
		 tvProgressHint=(TextView)findViewById(R.id.id_video_edit_progress_hint);
		 
       findViewById(R.id.id_video_edit_btn).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if(mMediaInfo.vDuration>60*1000){//大于60秒
					showHintDialog();
				}else{
					testFilterExecute();
				}
			}
		});
       
       findViewById(R.id.id_video_edit_btn2).setEnabled(false);
       findViewById(R.id.id_video_edit_btn2).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(SDKFileUtils.fileExist(dstPath)){
					Intent intent=new Intent(FilterExecuteActivity.this,VideoPlayerActivity.class);
	    	    	intent.putExtra("videopath", dstPath);
	    	    	startActivity(intent);
				}else{
					 Toast.makeText(FilterExecuteActivity.this, "目标文件不存在", Toast.LENGTH_SHORT).show();
				}
			}
		});
       
       //在手机的/sdcard/lansongBox/路径下创建一个文件名,用来保存生成的视频文件,(在onDestroy中删除)
       editTmpPath=SDKFileUtils.newMp4PathInBox();
       dstPath=SDKFileUtils.newMp4PathInBox();
	}
	VideoEditor mVideoEditer;
	BitmapSprite bitmapSprite=null;
	private void showHintDialog()
	{
		new AlertDialog.Builder(this)
		.setTitle("提示")
		.setMessage("视频过大,可能会需要一段时间,您确定要处理吗?")
        .setPositiveButton("确定", new DialogInterface.OnClickListener() {
			
			@Override
			public void onClick(DialogInterface dialog, int which) {
				// TODO Auto-generated method stub
				testFilterExecute();
			}
		})
		.setNegativeButton("取消", null)
        .show();
	}

	private boolean isExecuting=false;
	/**
	 * 举例滤镜后台处理
	 */
	private void testFilterExecute()
	{
		if(isExecuting)
			return ;
		
		isExecuting=true;
		
		 /**
	     * 创建一个视频后台处理对象 
	     * @param ctx
	     * @param path 需要做滤镜处理的视频路径
	     * @param glW 设置渲染线程opengl的宽度  这里设置为统一高度和宽度, FilterExecute检测到和源视频宽高不一致时,会自动的缩放视频,并在多余的地方增加黑边,来达到设置的视频宽高.
	     * @param glH  设置渲染线程opengl的高度.
	     * @param encBr  渲染线程后保存文件的码率.
	     * @param dstPath  渲染线程后保存文件路径
	     */
		FilterExecute vEdit=new FilterExecute(FilterExecuteActivity.this,videoPath,480,480,1000000,editTmpPath);
		
		//填入要处理的滤镜对象
		vEdit.switchFilterTo(new IFRiseFilter(getBaseContext()));
		
		//设置处理进度监听
		vEdit.setOnProgessListener(new onFilterExecuteProssListener() {
			
			@Override
			public void onProgress(FilterExecute v, long currentTimeUS) {  //每处理完一帧后的时间戳.单位微秒.
				// TODO Auto-generated method stub
				tvProgressHint.setText(String.valueOf(currentTimeUS));
			}
		});
		//设置处理完成后的监听
		vEdit.setOnCompletedListener(new onFilterExecuteCompletedListener() {
			
			@Override
			public void onCompleted(FilterExecute v) {
				// TODO Auto-generated method stub
				tvProgressHint.setText("Completed!!!FilterExecute");
				isExecuting=false;
				if(SDKFileUtils.fileExist(editTmpPath)){
					//增加音频信息.
					boolean ret=VideoEditor.encoderAddAudio(videoPath, editTmpPath,SDKDir.TMP_DIR, dstPath);
					if(!ret){
						dstPath=editTmpPath;
					}
				}
				findViewById(R.id.id_video_edit_btn2).setEnabled(true);
			}
		});
		//开始处理.
		vEdit.start();
	}
	@Override
	protected void onDestroy() {
		// TODO Auto-generated method stub
		super.onDestroy();
		 if(SDKFileUtils.fileExist(dstPath)){
			 SDKFileUtils.deleteFile(dstPath);
	       }
	       if(SDKFileUtils.fileExist(editTmpPath)){
	    	   SDKFileUtils.deleteFile(editTmpPath);
	       } 
	}
}	
