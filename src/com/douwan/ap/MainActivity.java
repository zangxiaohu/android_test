package com.douwan.ap;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpMessage;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.params.HttpClientParams;
import org.apache.http.conn.ConnectTimeoutException;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.net.Uri;
import android.net.wifi.ScanResult;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.StrictMode;
import android.os.SystemClock;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.AbsListView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.douwan.ap.Wifi;

public class MainActivity extends Activity {
	private Wifi wifi = null;	
	private List<ScanResult> wifi_list = null;
	
	private Button open_wifi = null;
	private Button close_wifi = null;
	private Button scan_wifi = null;
	private EditText mac_input = null;
	public static TextView remain_count = null;
	private EditText timeout_input = null;
	private TextView portal_time = null;
	private EditText count_input = null;
	private TextView succeed_count = null;
	private TextView min_time = null;
	private TextView max_time = null;
	private TextView average_time= null;
	private TextView fail_count = null;
	private TextView dhcp_fail_count = null;
	private TextView response_fail_count = null;
	private TextView portal_fail_count = null;
	
	private String string_mac_input = null;
	private String string_count_input = null;
	private String string_timeout_input = null;
	private String string_remain_count = null;
	
	private int string_succeed_count = 0;
	private int string_fail_count = 0;
	private long old_time = 0;
	private long new_time = 0;
	private long time_all[]= new long [100];
	private long long_min_time = 0;
	private long long_max_time = 0;
	private float long_average_time = 0;
	private long sum_time = 0;
	
	private int int_dhcp_fail_count = 0;
	private int int_response_fail_count = 0;
	private int int_portal_fail_count = 0;
	
	private final int DHCP_FAIL = 1;
	private final int CAN_NETWORK = 2;
	private final int UPDATE_LISTVIEW = 3;
	private final int PORTAL_FAIL = 4;

	private Handler myHandler = null;
	
	private ListView listView = null;
	private AlertDialog.Builder alertDialog = null;
	private ClipboardManager copy= null;
	private String string_copy = null;
	private ArrayAdapter<String> arrayAdapter = null;
	private List<String> list = null;
	
	private WifiService wifiService = null;
	private MsgReceiver msgReceiver = null; 
	
	@SuppressLint("HandlerLeak")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		wifi = new Wifi(MainActivity.this);
		
		findById();
		startBindService();
		receiveBroadcast();
			
		mac_input.addTextChangedListener(new Watcher());
		count_input.addTextChangedListener(new Watcher());
		timeout_input.addTextChangedListener(new Watcher());
		
		open_wifi.setOnClickListener(new MyOnClickListener());
		close_wifi.setOnClickListener(new MyOnClickListener());
		scan_wifi.setOnClickListener(new MyOnClickListener());
		
		listView.setOnScrollListener(new OnScrollListener() {
			
			@Override
			public void onScrollStateChanged(AbsListView view, int scrollState) {
				// TODO Auto-generated method stub
				if(scrollState == SCROLL_STATE_TOUCH_SCROLL)
				{		
					arrayAdapter.notifyDataSetChanged();
				}
			}

			@Override
			public void onScroll(AbsListView arg0, int arg1, int arg2, int arg3) {
				// TODO Auto-generated method stub
				
			}
			
		});
		
		myHandler = new Handler(){
			@Override
			public void handleMessage(Message msg) {
				// TODO Auto-generated method stub
				switch(msg.what)
				{
				case DHCP_FAIL:
					dhcpFail();
					break;
				case CAN_NETWORK:
					Toast.makeText(MainActivity.this,"���Ի��Ѿ�����,������!", Toast.LENGTH_SHORT).show();
					break;
				case UPDATE_LISTVIEW:
					setList(MainActivity.this,true);
					break;
				case PORTAL_FAIL:
					portalFail();
					break;
				}
				super.handleMessage(msg);
			}	
		};
		
	}
	
	public  void startBindService()
	{
		Intent it = new Intent(MainActivity.this,WifiService.class);
		bindService(it, connect,BIND_AUTO_CREATE);
		Log.i("debug.info", "startService");
	}
	
	ServiceConnection connect = new ServiceConnection() {
		
		@Override
		public void onServiceDisconnected(ComponentName arg0) {
			// TODO Auto-generated method stub
			Log.i("debug.info", "onServiceDisconnected");
			wifiService = null;
		}
		
		@Override
		public void onServiceConnected(ComponentName arg0, IBinder arg1) {
			// TODO Auto-generated method stub
			Log.i("debug.info", "onServiceConnected");
			wifiService = ((WifiService.MyBinder)arg1).getService();
			wifiService.execute();
		}
	};
	
	public void receiveBroadcast()
	{
		msgReceiver = new MsgReceiver();  
        IntentFilter intentFilter = new IntentFilter();  
        intentFilter.addAction("com.douwan.ap.WifiService");  
        registerReceiver(msgReceiver, intentFilter);
	}
	
	public class MsgReceiver extends BroadcastReceiver{  
		  
        @Override  
        public void onReceive(Context context, Intent intent) { 
        	Message message = new Message();
        	switch(intent.getStringExtra("wifiservice"))
        	{
        	case "update_listview":
        	 	Log.i("debug.info", "receive broadCast:update_listview");
        		setList(MainActivity.this,true);
        		break;
        	case "web_back":
        		succeedPort(intent);
    			if(!wifi.wifiManager.isWifiEnabled())
				{
					Log.i("debug.info", "WIFI�Ѿ��رգ����!");
					Toast.makeText(MainActivity.this,"WIFI�Ѿ��رգ����!", Toast.LENGTH_SHORT).show();
					break;
				}
				if(isInputComplete() == false)
				{
					break;
				}
				
				if(canConnectWifi() == false)
				{
					break;
				}
    			break;
        	}
        	
        }  
          
    }
	
	public void succeedPort(Intent data)
	{
		new_time = data.getExtras().getLong("time");
		Log.i("debug.info","����ʱ��: "+(new_time-old_time)+"ms");
		portal_time.setText("����ʱ��: "+(long)((new_time-old_time)/1000)+"s");
		
		string_remain_count = (Long.parseLong(string_remain_count)-1)+"";
		remain_count.setText("ʣ����Դ���: "+string_remain_count);
		
		isTimeOut((long)((new_time-old_time)/1000));
		
		Log.i("debug.info","is equal:"+data.getExtras().getString("redirect").equals("0"));
		if(data.getExtras().getString("redirect").equals("0") == false)
		{
			responseFail();
			Log.i("debug.info","redirect error.....");
		}
	}
	

	public void setList(Context context,boolean yes)
	{
		list = new ArrayList<String>();
		if(yes && wifi.wifiManager.isWifiEnabled())
		{		
			alertDialog = new AlertDialog.Builder(context);
			copy = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
			wifi.wifiManager.startScan();
			wifi_list = wifi.wifiManager.getScanResults();
			
			while(wifi_list.size() == 0 || wifi_list.size() == 1)
			{
				
				wifi_list = wifi.wifiManager.getScanResults();
				if(wifi_list.size() != 0 && wifi_list.size() != 1 )
				{
					break;
				}
			}
			for(int i=0;i<wifi_list.size();i++)
			{
//				Log.i("debug.info","SSID size = " + wifi_list.size());
//				Log.i("debug.info","SSID " + wifi_list.get(i).BSSID);
				list.add(wifi_list.get(i).SSID);
			}
			arrayAdapter = new ArrayAdapter<String>(context,android.R.layout.simple_list_item_1, list);
			listView.setAdapter(arrayAdapter);		
			listView.setOnItemLongClickListener(new OnItemLongClickListener() {
	
				@Override
				public boolean onItemLongClick(AdapterView<?> parent, View view,
						int position, long id) {
					// TODO Auto-generated method stub
					
					for(int i=0;i<wifi_list.size();i++)
	        		{
						if(position == i)
	            		{
//	            			setTitle(wifi_list.get(i).BSSID);
	            			string_copy = wifi_list.get(i).BSSID;
	            			alertDialog.setTitle("MAC��ַ").setMessage(
	            					wifi_list.get(i).BSSID).setPositiveButton("����", new DialogInterface.OnClickListener() 
	            					{ 
	            	                     
	            	                    @SuppressWarnings("deprecation")
										@Override 
	            	                    public void onClick(DialogInterface dialog, int which)
	            	                    { 
	            	                        // TODO Auto-generated method stub  
	            	                    	copy.setText(string_copy);
	            	                    } 
	            	                }).show();
	            			break;
	            		}
	        		} 
					return false;
				}
				
			}
			);
		}
		else {
			listView = (ListView) findViewById(R.id.listView);
			listView.setAdapter(new ArrayAdapter<String>(context,android.R.layout.simple_list_item_1,list));
		}
		
	}
	
	public void findById()
	{
		open_wifi = (Button) findViewById(R.id.open_wifi);
		close_wifi = (Button) findViewById(R.id.close_wifi);
		scan_wifi = (Button) findViewById(R.id.scan_wifi);
		
		mac_input = (EditText) findViewById(R.id.mac_input);
		portal_time = (TextView) findViewById(R.id.portal_time);
		count_input = (EditText) findViewById(R.id.count_input);
		remain_count = (TextView) findViewById(R.id.remain_count);
		timeout_input = (EditText) findViewById(R.id.timeout_input);
		succeed_count = (TextView) findViewById(R.id.succeed_count);
		min_time = (TextView) findViewById(R.id.min_time);
		max_time = (TextView) findViewById(R.id.max_time);
		average_time = (TextView) findViewById(R.id.average_time);
		fail_count = (TextView) findViewById(R.id.fail_count);
		dhcp_fail_count = (TextView) findViewById(R.id.dhcp_fail_count);
		response_fail_count = (TextView) findViewById(R.id.response_fail_count);
		portal_fail_count = (TextView) findViewById(R.id.portal_fail_count);
		
		listView = (ListView) findViewById(R.id.listView);
	}
	
	public void dhcpFail()//ʧ�ܣ��޷���ȡip
	{
		Toast.makeText(MainActivity.this,"IP��ȡʧ�ܣ��ر�WFIF....", Toast.LENGTH_SHORT).show();
		string_remain_count = (Long.parseLong(string_remain_count)-1)+"";
		remain_count.setText("ʣ����Դ���: "+string_remain_count);
		string_fail_count = string_fail_count+1;
		fail_count.setText("ʧ�ܴ���:"+string_fail_count);
		int_dhcp_fail_count = int_dhcp_fail_count+1;
		dhcp_fail_count.setText("dhcpʧ��:"+int_dhcp_fail_count);
//		setList(MainActivity.this, false);
		wifi.closeWifi();
	}
	
	public void responseFail()
	{
		Toast.makeText(MainActivity.this,"��Ӧʧ��....", Toast.LENGTH_SHORT).show();
		string_remain_count = (Long.parseLong(string_remain_count)-1)+"";
		remain_count.setText("ʣ����Դ���: "+string_remain_count);
		string_fail_count = string_fail_count+1;
		fail_count.setText("ʧ�ܴ���:"+string_fail_count);
		int_response_fail_count = int_response_fail_count+1;
		response_fail_count.setText("��Ӧʧ��:"+int_response_fail_count);
//		setList(MainActivity.this, false);
		wifi.closeWifi();
	}
	
	public void portalFail()
	{
		Toast.makeText(MainActivity.this,"����ʧ��....", Toast.LENGTH_SHORT).show();
		string_remain_count = (Long.parseLong(string_remain_count)-1)+"";
		remain_count.setText("ʣ����Դ���: "+string_remain_count);
		string_fail_count = string_fail_count+1;
		fail_count.setText("ʧ�ܴ���:"+string_fail_count);
		int_portal_fail_count = int_portal_fail_count +1;
		portal_fail_count.setText("����ʧ��:"+int_portal_fail_count);
//		wifi.closeWifi();
	}

	public class MyOnClickListener implements OnClickListener
	{

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			switch (v.getId()) {
			case R.id.open_wifi:
				if(wifi.wifiManager.isWifiEnabled())
				{
//					setList(MainActivity.this,true);
					Toast.makeText(MainActivity.this,"WIFI�Ѿ��򿪣���Ҫ���ң�", Toast.LENGTH_SHORT).show();
					break;
				}
				wifi.openWifi();
				while(!wifi.wifiManager.isWifiEnabled());
				Toast.makeText(MainActivity.this,"WIFI���ڴ�...", Toast.LENGTH_SHORT).show();
				portal_time.setText("����ʱ��:0s");
				
//				setList(MainActivity.this,true);
				break;
			case R.id.close_wifi:
				if(!wifi.wifiManager.isWifiEnabled())
				{
					Toast.makeText(MainActivity.this,"WIFI�Ѿ��رգ���Ҫ���ң�", Toast.LENGTH_SHORT).show();
					break;
				}
				setList(MainActivity.this, false);
//				portal_time.setText("����ʱ��:0s");
				Toast.makeText(MainActivity.this,"WIFI���ڹر�...", Toast.LENGTH_SHORT).show();
				wifi.closeWifi();
				break;
			case R.id.scan_wifi:
				
				if(!wifi.wifiManager.isWifiEnabled())
				{
					Log.i("debug.info", "WIFI�Ѿ��رգ����!");
					Toast.makeText(MainActivity.this,"WIFI�Ѿ��رգ����!", Toast.LENGTH_SHORT).show();
					break;
				}
				if(isInputComplete() == false)
				{
					break;
				}
				
				portal_time.setText("����ʱ��:0s");
				if(canConnectWifi() == false)
				{
					break;
				}
			default:
				break;
			}
		}
		
	}
	
	public void sendDhcpFailBroadcast()
	{
		Intent it = new Intent("com.douwan.ap.mainActivity");
		it.putExtra("mainActivity","dhcpfail");
		sendBroadcast(it);
	}
	
	public class MyThread implements Runnable
	{
		@Override
		public void run() {
			wifi.addNetwork(wifi.CreateWifiInfo(string_mac_input,""));
//				old_time = SystemClock.uptimeMillis();
//				while(!wifi.isWifiConnected(MainActivity.this));
			while(true)
			{
				if(wifi.isWifiConnected(MainActivity.this) == 0)
					break;
				else if(wifi.isWifiConnected(MainActivity.this) == -1)
				{
					break;
				}
				else if (wifi.isWifiConnected(MainActivity.this) == 1)
					continue;
			}
			
			if(wifi.getDhcp())
			{
				old_time = SystemClock.uptimeMillis();	
				sendUrl();
			}
			else
			{
				Message message = new Message();
				message.what = DHCP_FAIL;
				myHandler.sendMessage(message);
				Log.i("debug.info","ip ��ȡʧ��");
			}
    
		}
		
		
		
	}
	
	public boolean canConnectWifi()
	{
		if(wifi.isMacValid(string_mac_input))
		{
			MyThread mythread = new MyThread();
			Thread t = new Thread(mythread);
			t.start();
		}
		else
		{
			Log.i("debug.info","MAC��ַ��Ч");
			return false;
		}
		return true;
	}
	
	public boolean isInputComplete()
	{
		if(string_mac_input == null || string_count_input == null || string_timeout_input == null)
		{
			Log.i("debug.info","������ϢΪ��");
			Toast.makeText(MainActivity.this,"������ϢΪ��", Toast.LENGTH_SHORT).show();
			return false;
		}
		else if(string_mac_input.isEmpty() || string_count_input.isEmpty() || string_timeout_input.isEmpty())
		{
			Log.i("debug.info","������Ϣ������");
			Toast.makeText(MainActivity.this,"������Ϣ������", Toast.LENGTH_SHORT).show();
			return false;
		}
		else if(string_remain_count.equals("0"))
		{
			Log.i("debug.info","������������Դ���");
			Toast.makeText(MainActivity.this,"������������Դ���", Toast.LENGTH_SHORT).show();
			return false;
		}
		return true;
	}
	
	public void init_param()
	{
		string_succeed_count = 0;
		long_min_time = 0;
		long_max_time = 0;
		long_average_time = 0;
		sum_time = 0;
		int_dhcp_fail_count = 0;
		int_response_fail_count = 0;
		string_fail_count = 0;
		int_portal_fail_count = 0;
		
		succeed_count.setText("�ɹ�����:0");
		max_time.setText("���ʱ��(s):0");
		min_time.setText("��Сʱ��(s):0");
		average_time.setText("ƽ��ʱ��(s):0");
		
		fail_count.setText("ʧ�ܴ���:0");
		dhcp_fail_count.setText("dhcpʧ��:0");
		response_fail_count.setText("��Ӧʧ��:0");
		portal_fail_count.setText("����ʧ��:0");
	}
	
	public class Watcher implements TextWatcher
	{

		@Override
		public void beforeTextChanged(CharSequence s, int start, int count,
				int after) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void onTextChanged(CharSequence s, int start, int before,
				int count) {
			// TODO Auto-generated method stub
			
		}

		@SuppressLint("DefaultLocale")
		@Override
		public void afterTextChanged(Editable s)
		{
			// TODO Auto-generated method stub
			string_mac_input = mac_input.getText().toString();
			string_mac_input = string_mac_input.toString();
			string_mac_input = string_mac_input.toLowerCase();
			
			init_param();
			time_all = new long [100];
			
			string_count_input = count_input.getText().toString();
			string_remain_count = string_count_input;
			remain_count.setText("ʣ����Դ���:"+string_remain_count);
	
			string_timeout_input = timeout_input.getText().toString();
		}		
		
	}
	//���������
	public boolean startBrows()
	{
		Uri uri = Uri.parse("http://captive.apple.com");
		boolean result = false;
		/*Intent it = new Intent();
		it.setAction("android.intent.action.VIEW");
		it.setData(uri);
		
		startActivity(it);
		result = true;*/
		
		Intent it = new Intent(MainActivity.this,Web.class);
		it.putExtra("uri", uri.toString());
		startActivityForResult(it,1);
		result = true;
		return result;
	}
	//��ѯ����ʱ��(�����С��ƽ��)
	public void query_time(long query_time[])
	{
		long_min_time = query_time[0];
		long_max_time = query_time[0];
		for(int i=0;i<query_time.length;i++)
		{
			if(query_time[i] != 0)
			{
				if(query_time[i]>=long_max_time)
				{
					long_max_time = query_time[i];
				}
				else if(query_time[i]<=long_min_time)
				{
					long_min_time = query_time[i];
				}
				sum_time = sum_time + query_time[i];
//				Log.i("debug.info","time: "+query_time[i]);
			}
			
		}
//		Log.i("debug.info","s:"+sum_time+" c:"+string_succeed_count);
		long_average_time = ((float)sum_time/string_succeed_count);
	}
	
	//������ʱ���
	public void isTimeOut(long time)
	{
//		if(Long.parseLong(string_timeout_input) >= time)
		{
			string_succeed_count = string_succeed_count+1;
			succeed_count.setText("�ɹ�����:"+string_succeed_count);
			time_all[string_succeed_count-1] = time;
			
			sum_time = 0;
			query_time(time_all);
			
			min_time.setText("��Сʱ��(s): "+long_min_time);
			max_time.setText("���ʱ��(s): "+long_max_time);
			average_time.setText("ƽ��ʱ��(s): "+long_average_time);
			
		}
	}
	
	
	//webView ����ҳ��finished��ʱ��
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// TODO Auto-generated method stub
		if(resultCode == 10)
		{
			new_time = data.getExtras().getLong("time");
			Log.i("debug.info","����ʱ��: "+(new_time-old_time)+"ms");
			portal_time.setText("����ʱ��: "+(long)((new_time-old_time)/1000)+"s");
			
			string_remain_count = (Long.parseLong(string_remain_count)-1)+"";
			remain_count.setText("ʣ����Դ���: "+string_remain_count);
			
			isTimeOut((long)((new_time-old_time)/1000));
			
			Log.i("debug.info","is equal:"+data.getExtras().getString("redirect").equals("0"));
			if(data.getExtras().getString("redirect").equals("0") == false)
			{
				responseFail();
				Log.i("debug.info","redirect error.....");
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	public void sendUrl()
	{
		String[] res = {
//				"http://www.thinkdifferent.us",
				"http://captive.apple.com"
//				"http://www.baidu.com"
				};
		long old_portal = 0;
		long new_portal = 0;
		String line = null;
		URL url = null;
		HttpURLConnection connection = null;
		try {
			url = new URL(res[0]);
			
			try {
				connection = (HttpURLConnection) url.openConnection();
				connection.setDoInput(true);
		        connection.setConnectTimeout((int)(Long.parseLong(string_timeout_input)*1000));
		        connection.setReadTimeout((int)(Long.parseLong(string_timeout_input)*1000));
		        connection.setRequestMethod("GET");
		        connection.setUseCaches(false);
		        old_portal = SystemClock.uptimeMillis();
		        connection.connect();
		        if(connection.getResponseCode() != 200)
		        {
		        	throw new IOException("return 200:false");
		        }
		        new_portal = SystemClock.uptimeMillis();
		        BufferedReader reader = new BufferedReader(new InputStreamReader(  
		                connection.getInputStream(), "utf-8"));
		        Log.i("debug.info","connect time="+(new_portal - old_portal));
		        StringBuffer m = new StringBuffer();
		        while ((line = reader.readLine()) != null) 
		        {
		            m.append(line);
//			            Log.i("debug.info",""+m);
		        }
		        reader.close();
			    Log.i("debug.info",""+m);
//		        if(m.toString().contains("<HTML><HEAD><TITLE>Success</TITLE></HEAD><BODY>Success</BODY></HTML>"))
		        if(m.toString().contains("<TITLE>Success</TITLE>"))
				{
					Log.i("debug.info","����IOS��վ����Succeed��");
//					startBrows(Uri.parse("http://www.baidu.com"));
					Message message = new Message();
					message.what = CAN_NETWORK;
					myHandler.sendMessage(message);
					return;
				}
		        
		       else if(m.toString().contains("<title>404</title>"))
		        {
		        	throw new IOException("return 404");
		        }
		        
		        else
		        {
		        	if(startBrows() == true)
		    		{
		    			Log.i("debug.info","start brows...");
		    		}
		        }
		        
			} catch (IOException e) {
				// TODO Auto-generated catch block
				Log.i("debug.info","IOException:"+e.getMessage());
				Log.i("debug.info","connect time="+(new_portal - old_portal));
//				Thread.sleep(Long.parseLong(string_timeout_input)*1000);
				Message message = new Message();
				message.what = PORTAL_FAIL;
				myHandler.sendMessage(message);
				e.printStackTrace();
			  }
			finally{
				  connection.disconnect();
			  }
		}
         catch (MalformedURLException e) {
		// TODO Auto-generated catch block
        	 Log.i("debug.info","MalformedURLException");
        	 Log.i("debug.info","2:"+(new_portal - old_portal));
        	 e.printStackTrace();
		}
	}
	


	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
}
