package com.example.lansongeditordemo;

import java.util.ArrayList;

import com.lansoeditor.demo.R;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.view.PagerAdapter;
import android.support.v4.view.PagerTitleStrip;
import android.support.v4.view.ViewPager;
import android.support.v4.view.ViewPager.OnPageChangeListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.TranslateAnimation;
import android.widget.ImageView;

/**
 * UI代码完全公开
短视频APP在于个性化,您可以任意修改举例的UI代码,来实现您APP的个性
并且我们升级的新功能,尽量不涉及UI部分.


永久升级.

像所有的正版软件一样,您购买一次授权,就可以永久享用我们SDK后期所有的新功能.
 *
 */
public class IntroductionActivity extends Activity {
    /** Called when the activity is first created. */
	private ViewPager mViewPager;	
	private PagerTitleStrip mPagerTitleStrip;
	private ImageView mPageImg;// 动画图片
	private int currIndex = 0;
	private ImageView mPage0;
	private ImageView mPage1;
	private ImageView mPage2;
	private ImageView mPage3;
	private ImageView mPage4;
	private ImageView mPage5;
	private ImageView mPage_pdf;
	
	
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.viewpager);
        mViewPager = (ViewPager)findViewById(R.id.viewpager);        
        mViewPager.setOnPageChangeListener(new MyOnPageChangeListener());
        mPagerTitleStrip = (PagerTitleStrip)findViewById(R.id.pagertitle);
        
        mPageImg = (ImageView) findViewById(R.id.page_now);    
        
        mPage0 = (ImageView)findViewById(R.id.page0);
        mPage1 = (ImageView)findViewById(R.id.page1);
        mPage2 = (ImageView)findViewById(R.id.page2);
        mPage3 = (ImageView)findViewById(R.id.page3);
        mPage4 = (ImageView)findViewById(R.id.page4);
        mPage_pdf = (ImageView)findViewById(R.id.page_pdf);
        mPage5 = (ImageView)findViewById(R.id.page5);
        
        
        //将要分页显示的View装入数组中
        LayoutInflater mLi = LayoutInflater.from(this);
        View view1 = mLi.inflate(R.layout.view1, null);
        View view2 = mLi.inflate(R.layout.view2, null);
        View view3 = mLi.inflate(R.layout.view3, null);
        View view4 = mLi.inflate(R.layout.view4, null);
        View view5 = mLi.inflate(R.layout.view5, null);
        View view6 = mLi.inflate(R.layout.view6, null);
        View view_pdf = mLi.inflate(R.layout.view_pdf, null);
        	    
        //每个页面的view数据
        final ArrayList<View> views = new ArrayList<View>();
        views.add(view1);
        views.add(view2);
        views.add(view3);
        views.add(view4);
        views.add(view5);
        views.add(view_pdf);
        views.add(view6);
        
        final ArrayList<String> titles = new ArrayList<String>();
        titles.add("①");
        titles.add("②");
        titles.add("③");
        titles.add("④");
        titles.add("⑤");
        titles.add("⑥");
        titles.add("7");
        
        PagerAdapter mPagerAdapter = new PagerAdapter() {
			
			@Override
			public boolean isViewFromObject(View arg0, Object arg1) {
				return arg0 == arg1;
			}
			
			@Override
			public int getCount() {
				return views.size();
			}

			@Override
			public void destroyItem(View container, int position, Object object) {
				((ViewPager)container).removeView(views.get(position));
			}
			
			@Override
			public CharSequence getPageTitle(int position) {
				return titles.get(position);
			}
			
			@Override
			public Object instantiateItem(View container, int position) {
				((ViewPager)container).addView(views.get(position));
				return views.get(position);
			}
		};
		
		mViewPager.setAdapter(mPagerAdapter);
    }
    
    public class MyOnPageChangeListener implements OnPageChangeListener {

		@Override
		public void onPageSelected(int arg0) {
			Animation animation = null;
			switch (arg0) {
			case 0:				
				mPage0.setImageDrawable(getResources().getDrawable(R.drawable.page_now));
				mPage1.setImageDrawable(getResources().getDrawable(R.drawable.page));
				if (currIndex == arg0+1) {
					animation = new TranslateAnimation(20*(arg0+1), 20*arg0, 0, 0);
				} /*else if (currIndex == 2) {
					animation = new TranslateAnimation(20*2, 0, 0, 0);
				}*/
				break;
			case 1:
				mPage1.setImageDrawable(getResources().getDrawable(R.drawable.page_now));
				mPage0.setImageDrawable(getResources().getDrawable(R.drawable.page));
				mPage2.setImageDrawable(getResources().getDrawable(R.drawable.page));
				if (currIndex == arg0-1) {
					animation = new TranslateAnimation(20*(arg0-1), 20*arg0, 0, 0);	
					
				} else if (currIndex == arg0+1) {
					animation = new TranslateAnimation(20*(arg0+1), 20*arg0, 0, 0);
				}
				break;
			case 2:
				mPage2.setImageDrawable(getResources().getDrawable(R.drawable.page_now));
				mPage1.setImageDrawable(getResources().getDrawable(R.drawable.page));
				mPage3.setImageDrawable(getResources().getDrawable(R.drawable.page));
				if (currIndex == arg0-1) {
					animation = new TranslateAnimation(20*(arg0-1), 20*arg0, 0, 0);
				} else if (currIndex == arg0+1) {
					animation = new TranslateAnimation(20*(arg0+1), 20*arg0, 0, 0);
				}
				break;
			case 3:
				mPage3.setImageDrawable(getResources().getDrawable(R.drawable.page_now));
				mPage4.setImageDrawable(getResources().getDrawable(R.drawable.page));
				mPage2.setImageDrawable(getResources().getDrawable(R.drawable.page));
				if (currIndex == arg0-1) {
					animation = new TranslateAnimation(20*(arg0-1), 20*arg0, 0, 0);
					
				} else if (currIndex == arg0+1) {
					animation = new TranslateAnimation(20*(arg0+1), 20*arg0, 0, 0);
				}
				break;
			case 4:
				mPage4.setImageDrawable(getResources().getDrawable(R.drawable.page_now));
				mPage3.setImageDrawable(getResources().getDrawable(R.drawable.page));
				mPage_pdf.setImageDrawable(getResources().getDrawable(R.drawable.page));
				if (currIndex == arg0-1) {
					animation = new TranslateAnimation(20*(arg0-1), 20*arg0, 0, 0);
				} else if (currIndex == arg0+1) {
					animation = new TranslateAnimation(20*(arg0+1), 20*arg0, 0, 0);
				}
				break;
			case 5:
				mPage_pdf.setImageDrawable(getResources().getDrawable(R.drawable.page_now));
				mPage4.setImageDrawable(getResources().getDrawable(R.drawable.page));
				mPage5.setImageDrawable(getResources().getDrawable(R.drawable.page));
				if (currIndex == arg0-1) {
					animation = new TranslateAnimation(20*(arg0-1), 20*arg0, 0, 0);
				} else if (currIndex == arg0+1) {
					animation = new TranslateAnimation(20*(arg0+1), 20*arg0, 0, 0);
				}
				break;
			case 6:
				mPage5.setImageDrawable(getResources().getDrawable(R.drawable.page_now));
				mPage_pdf.setImageDrawable(getResources().getDrawable(R.drawable.page));
				mPage0.setImageDrawable(getResources().getDrawable(R.drawable.page));
				if (currIndex == arg0-1) {
					animation = new TranslateAnimation(20*(arg0-1), 20*arg0, 0, 0);
				} else if (currIndex == arg0+1) {
					animation = new TranslateAnimation(20*(arg0+1), 20*arg0, 0, 0);
				}
				break;
			
			}
			currIndex = arg0;
			animation.setFillAfter(true);// True:图片停在动画结束位置
			animation.setDuration(300);
			//mPageImg.startAnimation(animation);
		}
		@Override
		public void onPageScrolled(int arg0, float arg1, int arg2) {
		}

		@Override
		public void onPageScrollStateChanged(int arg0) {
		}
	}
    
    public void startbutton(View v) {  
    	Intent intent=new Intent(IntroductionActivity.this,MainActivity.class);
		startActivity(intent);
		IntroductionActivity.this.finish();
      }  
   
}
