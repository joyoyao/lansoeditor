package com.example.lansongeditordemo;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;

import com.lansoeditor.demo.R;

public class DemoUtils {

	 public static void showScale480HintDialog(Context ctx)
	   	{
	   		new AlertDialog.Builder(ctx)
	   		.setTitle("提示")
	   		.setMessage(R.string.scale480_hint)
	           .setPositiveButton("确定", new DialogInterface.OnClickListener() {
	   			
	   			@Override
	   			public void onClick(DialogInterface dialog, int which) {
	   				// TODO Auto-generated method stub
	   				
	   			}
	   		})
	       .show();
	   	}
}
