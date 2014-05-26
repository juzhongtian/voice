package com.example.loadpicture;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.ActionBar;
import android.app.Fragment;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.JsonReader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.os.Build;

public class MainActivity extends Activity {

	private TextView text;
	private String urlstring;
	private EditText input;
	private Button button;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		text = (TextView) findViewById(R.id.textView1);
		input = (EditText)findViewById(R.id.editText1);
        button = (Button)findViewById(R.id.button1);
        
        button.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View arg0) {
				// TODO Auto-generated method stub
				byte[] data = {'l'};
				try {
					urlstring = new String(data,"UTF-8");     //如何设置呢？？？？
				} catch (UnsupportedEncodingException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
				urlstring = getResources().getString(R.string.tran_URL) + "?client_id="
						+ getResources().getString(R.string.client_id) + "&q=";
				CharSequence text_input =  input.getText();//     要不要检测
				Toast.makeText(getApplicationContext(), text_input, Toast.LENGTH_SHORT).show();
		        urlstring = urlstring.concat(text_input.toString());
		        urlstring = urlstring.concat("&from=auto&to=auto");
				try {
					TranTask task = new TranTask();
					task.execute(new URL(urlstring));
				} catch (MalformedURLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
		
	}

	public class TranTask extends AsyncTask<URL, Integer, String> {

		@Override
		protected String doInBackground(URL... arg0) {
			// TODO Auto-generated method stub
			List messages = new ArrayList();
			String temp;
			try {
				HttpURLConnection conn = (HttpURLConnection) arg0[0]
						.openConnection();
				
				conn.setConnectTimeout(10000);
				conn.setDoInput(true);
				conn.setRequestMethod("GET");
				conn.connect();
				int response = conn.getResponseCode();
				Log.d("DEBUG_TAG", "The response is: " + response);
				InputStream is = conn.getInputStream();

				InputStreamReader read = new InputStreamReader(is, "UTF-8");
				ArrayList T = new ArrayList();
				char[] buffer = new char[1000];
				read.read(buffer);
				temp = new String(buffer); 
				Log.d("MESSAGE", temp);    //显示收到服务器的消息.
				try {
					JSONObject obj = new JSONObject(temp);
					JSONArray arr = obj.getJSONArray("trans_result");
					JSONObject obj_in = arr.getJSONObject(0);
					messages.add(obj_in.getString("dst")); // 翻译结果第一行
				} catch (JSONException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			return (String) messages.get(0);
		}

		@Override
		protected void onPostExecute(String result) {
			// TODO Auto-generated method stub
			text.setText(result);
			super.onPostExecute(result);
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
