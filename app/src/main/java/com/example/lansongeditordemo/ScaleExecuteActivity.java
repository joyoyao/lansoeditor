package com.example.lansongeditordemo;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;

import com.lansoeditor.demo.R;
import com.lansosdk.box.BitmapSprite;
import com.lansosdk.box.ScaleExecute;
import com.lansosdk.box.onMediaPoolCompletedListener;
import com.lansosdk.box.onMediaPoolProgressListener;
import com.lansosdk.box.onScaleCompletedListener;
import com.lansosdk.box.onScaleProgressListener;
import com.lansosdk.videoeditor.MediaInfo;
import com.lansosdk.videoeditor.SDKDir;
import com.lansosdk.videoeditor.SDKFileUtils;
import com.lansosdk.videoeditor.VideoEditor;

/**
 * 对视频画面进行缩放, 用ffmpeg也可以完成, 但手机cpu的限制,ffmpeg用软件代码的形式来对一帧像素进行处理, 太慢了,在手机上完全无法使用. 
 * 基于此,我们推出了 用OpenGL来完成的硬缩放的方式,极大的提升了视频缩放的速度.
 * 
 *  演示: ScaleExecute类的使用. 这个是在后台进行缩放,在OpenGL线程中运行.
 * 流程是: 硬件解码---->OpenGL处缩放--->硬件编码.
 */
public class ScaleExecuteActivity extends Activity{

	String videoPath=null;
	ProgressDialog  mProgressDialog;
	int videoDuration;
	boolean isRuned=false;
	MediaInfo   mInfo;
	TextView tvProgressHint;
	
	private String editTmpPath=null;
	private String dstPath=null;
	 TextView tvHint;
	 
	private static final String TAG="MediaPoolExecuteActivity";
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		
		super.onCreate(savedInstanceState);
		Thread.setDefaultUncaughtExceptionHandler(new snoCrashHandler());
		 
		 videoPath=getIntent().getStringExtra("videopath");
		 
		 mInfo=new MediaInfo(videoPath,false);
		 mInfo.prepare();
		 
		 Log.i(TAG,mInfo.toString());
		 
		 setContentView(R.layout.video_edit_demo_layout);
		 tvHint=(TextView)findViewById(R.id.id_video_editor_hint);
		 
		 tvHint.setText(R.string.scaleexecute_demo_hint);
   
		 tvProgressHint=(TextView)findViewById(R.id.id_video_edit_progress_hint);
		 
       findViewById(R.id.id_video_edit_btn).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				
				if(mInfo.vDuration>=60*1000){//大于60秒
					showHintDialog();
				}else{
					testScaleEdit();
				}
			}
		});
       
       findViewById(R.id.id_video_edit_btn2).setEnabled(false);
       findViewById(R.id.id_video_edit_btn2).setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				if(SDKFileUtils.fileExist(dstPath)){
					Intent intent=new Intent(ScaleExecuteActivity.this,VideoPlayerActivity.class);
	    	    	intent.putExtra("videopath", dstPath);
	    	    	startActivity(intent);
				}else{
					 Toast.makeText(ScaleExecuteActivity.this, "目标文件不存在", Toast.LENGTH_SHORT).show();
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
				testScaleEdit();
			}
		})
		.setNegativeButton("取消", null)
        .show();
	}
	private boolean isExecuting=false;
	private void testScaleEdit()
	{
		if(isExecuting)
			return ;
		
		isExecuting=true;
		/**
		 * 创建ScaleExecute类.
		 */
		ScaleExecute  vScale=new ScaleExecute(ScaleExecuteActivity.this,videoPath);  //videoPath是路径
		//设置缩放后输出文件路径.
		vScale.setOutputPath(editTmpPath);
		
		
		//计算等比例缩放的压缩码率.
		 int scaleWidth=mInfo.vCodecWidth/2;  //这里是把视频的宽度和高度压缩一半.
		 int scaleHeight=mInfo.vCodecHeight/2;
		 
		 if(mInfo.vRotateAngle==90 || mInfo.vRotateAngle==270)  //如果垂直,则更换宽高.
		 {
			 int tmp=scaleWidth;
			 scaleWidth=scaleHeight;
			 scaleHeight=tmp;
		 }
		 
		 
	  	 int max=Math.max(mInfo.vCodecWidth,mInfo.vCodecHeight);  
	  	 int scaleMax=Math.max(scaleWidth, scaleHeight);
	  	 float scaleBitRate=(float)mInfo.vBitRate*1.3f;
	  	 
	  	 float ratio= (float)scaleMax / (float)max;  //得到比例.
	  	 scaleBitRate*=ratio;  //得到恒定码率的等比例值.
		
	  	 
	  	vScale.setModifyAngle(false);//当没有设置或设置为false时,,如果原来视频旋转了270或90度,则缩放后的视频也旋转270或90度. 当设置为true时, 把原来旋转的视频角度值去掉,进行缩放.默认是false或不设置.
	  	 
	  	 
		//设置缩放的宽度,高度和缩放后保存文件的码率.
		vScale.setScaleSize(scaleWidth,scaleHeight,(int)scaleBitRate);
		
		//设置缩放进度监听.currentTimeUS当前处理的视频帧时间戳.
		vScale.setScaleProgessListener(new onScaleProgressListener() {
			
			@Override
			public void onProgress(ScaleExecute v, long currentTimeUS) {
				// TODO Auto-generated method stub
				tvProgressHint.setText(String.valueOf(currentTimeUS));
			}
		});
		
		//设置缩放进度完成后的监听.
		vScale.setScaleCompletedListener(new onScaleCompletedListener() {
			
			@Override
			public void onCompleted(ScaleExecute v) {
				// TODO Auto-generated method stub
				tvProgressHint.setText("Completed!!!");
				isExecuting=false;
				if(SDKFileUtils.fileExist(editTmpPath)){
					//增加音频信息
					boolean ret=VideoEditor.encoderAddAudio(videoPath, editTmpPath,SDKDir.TMP_DIR, dstPath);
					if(!ret){
						dstPath=editTmpPath;
					}
				}
				findViewById(R.id.id_video_edit_btn2).setEnabled(true);
			}
		});
		vScale.start();
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
