package com.pl2303;

import java.io.IOException;
import tw.com.prolific.pl2303multilib.PL2303MultiLib;
import android.os.Bundle;
import android.os.Handler;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Color;
import android.graphics.Typeface;
import android.hardware.usb.UsbManager;
import android.text.Html;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemSelectedListener;

class UARTSettingInfo {
	public int iPortIndex = 0;
	public PL2303MultiLib.BaudRate mBaudrate = PL2303MultiLib.BaudRate.B9600;//jash modify at 2015/10/26
	public PL2303MultiLib.DataBits mDataBits = PL2303MultiLib.DataBits.D8;
	public PL2303MultiLib.Parity mParity = PL2303MultiLib.Parity.NONE;
	public PL2303MultiLib.StopBits mStopBits = PL2303MultiLib.StopBits.S1;
	public PL2303MultiLib.FlowControl mFlowControl = PL2303MultiLib.FlowControl.OFF;		
}//class UARTSettingInfo

public class MainActivity extends Activity {
	private static boolean bDebugMesg = true;
	
	PL2303MultiLib mSerialMulti;
	
    private static enum DeviceOrderIndex {
    	DevOrder1, 
    	DevOrder2,
    	DevOrder3,
    	DevOrder4
    };
    private static final int DeviceIndex1 = 0;
    private static final int DeviceIndex2 = 1;
    private static final int DeviceIndex3 = 2;

    private static final int MAX_DEVICE_COUNT = 4;
    private static final String ACTION_USB_PERMISSION = "com.prolific.pluartmultisimpletest.USB_PERMISSION";
    private UARTSettingInfo gUARTInfoList[];   
    private int iDeviceCount = 0;
    private boolean bDeviceOpened[] = new boolean[MAX_DEVICE_COUNT];
    
    private boolean gThreadStop[] = new boolean[MAX_DEVICE_COUNT];
    private boolean gRunningReadThread[] = new boolean[MAX_DEVICE_COUNT];
    
    //--------------------
    private Button m_btn1;
    private EditText m_et01;
    private TextView m_tv01;
    //---------------------
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//-----------------------------------
		m_btn1=(Button)this.findViewById(R.id.btn1);//UI mapping obj
		m_et01=(EditText)this.findViewById(R.id.et01);
		m_tv01=(TextView)this.findViewById(R.id.tv01);
		m_btn1.setEnabled(false);//禁止使用
		m_et01.setEnabled(false);
		m_tv01.setEnabled(false);
		m_btn1.setOnClickListener(new Button.OnClickListener() {		
			public void onClick(View v) {
				OpenUARTDevice(DeviceIndex1);
				WriteToUARTDevice(DeviceIndex1);
			}
		});
		//-----------------------------------
	    // get service
		mSerialMulti = new PL2303MultiLib((UsbManager) getSystemService(Context.USB_SERVICE),
           	  	this, ACTION_USB_PERMISSION); 

		//if you don't want to use Software Queue, below constructor to be used
		//mSerialMulti = new PL2303MultiLib((UsbManager) getSystemService(Context.USB_SERVICE),
        //   	  	this, ACTION_USB_PERMISSION,false); 						
		
		gUARTInfoList = new UARTSettingInfo[MAX_DEVICE_COUNT];
		
		for(int i=0;i<MAX_DEVICE_COUNT;i++) {
			gUARTInfoList[i] = new UARTSettingInfo(); 	
			gUARTInfoList[i].iPortIndex = i;
		    gThreadStop[i] = false;
		    gRunningReadThread[i] = false;	
		    bDeviceOpened[i] = false;
		}
		
	}
	public void onPause() {
    	super.onStart();
	}
	public void onRestart() {
    	//super.onStart();
    	super.onRestart();
	}
   	protected void onStop() {
    	super.onStop();        
    }
    protected void onDestroy() {  
    	if(mSerialMulti!=null) {
    		for(int i=0;i<MAX_DEVICE_COUNT;i++) {
    		    gThreadStop[i] = true;
    		}//First to stop app view-thread
    		if(iDeviceCount>0)
    			unregisterReceiver(PLMultiLibReceiver);
    		mSerialMulti.PL2303Release();
    		mSerialMulti = null;
    	}
    	super.onDestroy();
    }
    public void onStart() {
    	super.onStart();
    }
    public void onResume() {
    	//DumpMsg("Enter onResume"); 
    	super.onResume();
    	String action =  getIntent().getAction();
    	//DumpMsg("onResume:"+action);       	
    	
   		iDeviceCount = mSerialMulti.PL2303Enumerate();
       	//DumpMsg("enumerate Count="+iDeviceCount);
       	if( 0==iDeviceCount ) {
       		//SetEnabledDevControlPanel(DeviceOrderIndex.DevOrder1,false,false);
       		//SetEnabledDevControlPanel(DeviceOrderIndex.DevOrder2,false,false);
       		//SetEnabledDevControlPanel(DeviceOrderIndex.DevOrder3,false,false);
       		Toast.makeText(this, "no more devices found", Toast.LENGTH_SHORT).show();      
       	} else {   
       		//DumpMsg("DevOpen[0]="+bDeviceOpened[DeviceIndex1]);
       		//DumpMsg("DevOpen[1]="+bDeviceOpened[DeviceIndex2]);
       		//DumpMsg("DevOpen[2]="+bDeviceOpened[DeviceIndex3]);
       		if(!bDeviceOpened[DeviceIndex1]) {
       			//DumpMsg("iDeviceCount(=1)="+iDeviceCount);
       			//SetEnabledDevControlPanel(DeviceOrderIndex.DevOrder1, true, false);        			
       			m_btn1.setEnabled(true);
       			m_et01.setEnabled(true);
       			m_tv01.setEnabled(true);
       		}
       		//register receiver for PL2303Multi_USB notification
       		IntentFilter filter = new IntentFilter();
       	    filter.addAction(mSerialMulti.PLUART_MESSAGE); 
       	    registerReceiver(PLMultiLibReceiver, filter);
   			Toast.makeText(this, "The "+iDeviceCount+" devices are attached", Toast.LENGTH_SHORT).show();
       	}//if( 0==iDevCnt )        	
    	//DumpMsg("Leave onResume"); 
    }//public void onResume()
    private final BroadcastReceiver PLMultiLibReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
           if(intent.getAction().equals(mSerialMulti.PLUART_MESSAGE)){
        	   Bundle extras = intent.getExtras();
        	   if(extras!=null) {
        		   String str = (String)extras.get(mSerialMulti.PLUART_DETACHED);
        		   //DumpMsg("receive data:"+str);
        		   int index = Integer.valueOf(str);
        		   if(DeviceIndex1==index) {
               		   //SetEnabledDevControlPanel(DeviceOrderIndex.DevOrder1,false,false);   
               		   //spBaudRate1.setEnabled(false);
               		   bDeviceOpened[DeviceIndex1] = false;
               		   //tvSN.setTextColor(0xffff0000);
               		   //tvSN.setText("");               		   
        		   }
        	   }        	   
           }    
        }//onReceive
     };
     private void OpenUARTDevice(int index) {
     	//DumpMsg("Enter OpenUARTDevice");
     	
    	 	if(mSerialMulti==null)
    	 		return;  	
    	 	
         if(!mSerialMulti.PL2303IsDeviceConnectedByIndex(index)) 
          	return;  	   	 	
     	
 		boolean res;
 		UARTSettingInfo info = gUARTInfoList[index];
 		res = mSerialMulti.PL2303OpenDevByUARTSetting(index, info.mBaudrate, info.mDataBits, info.mStopBits, 
 					info.mParity, info.mFlowControl);
 		if( !res ) {
 			//DumpMsg("fail to setup");
 			Toast.makeText(this, "Can't set UART correctly!", Toast.LENGTH_SHORT).show();
 			return;
 		}              			

 		bDeviceOpened[index] = true;
 		
 		if(!gRunningReadThread[index]) {
 			UpdateDisplayView(index);
 		}
 		
    	 	//DumpMsg("Leave OpenUARTDevice");
    	 	Toast.makeText(this, "Open ["+ mSerialMulti.PL2303getDevicePathByIndex(index) +"] successfully!", Toast.LENGTH_SHORT).show();
     	return;
     }//private void OpenUARTDevice(int index) 
     private void WriteToUARTDevice(int index) {
     	//DumpMsg("Enter WriteToUARTDevice");
     	
    	 	if(mSerialMulti==null)
    	 		return;  	
    	 	
         if(!mSerialMulti.PL2303IsDeviceConnectedByIndex(index)) 
          	return;  	  
         
         String strWrite = null;
         if(DeviceIndex1==index) {
         	strWrite = m_et01.getText().toString();
         }
         //DumpMsg("PL2303Multi Write(" + strWrite.length() + "):" + strWrite);
         
         if( strWrite==null || "".equals(strWrite.trim()) ) { //str is empty
         	//DumpMsg("WriteToUARTDevice: no data to write");
         	return;
         }

         int res = mSerialMulti.PL2303Write(index, strWrite.getBytes());
     	if( res<0 ) {
     		//DumpMsg("w: fail to write: "+ res);
     		return;
     	}         	

        // DumpMsg("Leave WriteToUARTDevice");
     } //private void WriteToUARTDevice(int index)     
     private void UpdateDisplayView(int index) {
     	gThreadStop[index] = false;
 	    gRunningReadThread[index] = true;	 
 	    
     	if( DeviceIndex1==index ) {
     		new Thread(ReadLoop1).start();    		
     	}
     }
     private int ReadLen1;
     private byte[] ReadBuf1 = new byte[4096];    
     Handler mHandler1 = new Handler();
     private Runnable ReadLoop1 = new Runnable() {
         public void run() {
                         
             for (;;) {
             	ReadLen1 = mSerialMulti.PL2303Read(DeviceIndex1, ReadBuf1);
                 if (ReadLen1 > 0) {
                     //ReadBuf1[ReadLen1] = 0;                	
                  	//DumpMsg("Read  Length : " + ReadLen1);
                  	mHandler1.post(new Runnable() {                 		
                  		public void run() {
                  			StringBuffer sbHex=new StringBuffer();
                  	         for (int j = 0; j < ReadLen1; j++) {            	   
                  	        	 sbHex.append((char) (ReadBuf1[j]&0x000000FF));
                              }              
                  	       m_tv01.setText("input:"+sbHex.toString());
                  	         //svReadView1.fullScroll(ScrollView.FOCUS_DOWN);
                  		}//run
                  	});//Handler.post
                 }//if (len > 0)

                 DelayTime(60);

                 if (gThreadStop[DeviceIndex1]) {
                 	gRunningReadThread[DeviceIndex1] = false;
                 	return;
                 }//if                
             }//for(...)
             
         }//run
     };//Runnable
	    /*
	     * Miscellaneous functions
	     * 
	     */
	private void DelayTime(int dwTimeMS) {
		//Thread.yield();
		long StartTime, CheckTime;
				
		if(0==dwTimeMS) {
			Thread.yield();
			return;		
		}
		//Returns milliseconds running in the current thread
		StartTime = System.currentTimeMillis();
		do {
				CheckTime=System.currentTimeMillis();
				Thread.yield();
		 } while( (CheckTime-StartTime)<=dwTimeMS);		
	}     
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

}
