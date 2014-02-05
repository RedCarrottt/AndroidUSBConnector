package com.example.usbconnectortestserver;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends Activity implements USBMessageListener {
	private static final String TAG = "MainActivity";
	private static final int DEFAULT_SELF_PORT = 30012;
	private USBConnector mUSBConnector;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		this.mUSBConnector = USBConnector.server(DEFAULT_SELF_PORT);
		this.mUSBConnector.addListener(this);
		this.mUSBConnector.start();

		Button sendMsgButton = (Button) this.findViewById(R.id.button1);
		sendMsgButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View arg0) {
				String msg = "THIS MESSAGE IS CAME FROM TARGET DEVICE";
				mUSBConnector.sendMessage(msg);
				Log.d(TAG, "Sent this message: " + msg);
			}
		});
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public void onUSBMessage(String messages) {
		final String finalMsg = messages;
		final TextView textView = (TextView) this.findViewById(R.id.textView1);
		this.runOnUiThread(new Runnable() {
			@Override
			public void run() {
				textView.setText(finalMsg);
				Log.d(TAG, "Listen successful!!: " + finalMsg);
			}
		});
	}
}
