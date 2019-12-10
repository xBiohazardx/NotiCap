package com.sapuseven.noticap.service;

import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import com.sapuseven.noticap.utils.FilterRule;
import com.sapuseven.noticap.utils.SSHClient;
import com.sapuseven.noticap.utils.SSHIdentity;
import com.sapuseven.noticap.utils.notiDelayList;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.zip.DataFormatException;

public class NotificationListenerService extends android.service.notification.NotificationListenerService {
	MqttAndroidClient mqttAndroidClient;

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		SharedPreferences prefs = androidx.preference.PreferenceManager.getDefaultSharedPreferences(this);
		mqttAndroidClient = new MqttAndroidClient(getApplicationContext(), prefs.getString("server_uri", ""), "NotiCapClient");
		mqttAndroidClient.setCallback(new MqttCallbackExtended() {
			@Override
			public void connectComplete(boolean reconnect, String serverURI) {
				Log.d(TAG, "Connected");
			}

			@Override
			public void connectionLost(Throwable cause) {
				Log.d(TAG, "Connection lost");
			}

			@Override
			public void messageArrived(String topic, MqttMessage message) throws Exception {

			}

			@Override
			public void deliveryComplete(IMqttDeliveryToken token) {
				Log.d(TAG, "Delivery complete");
			}
		});
		MqttConnectOptions mqttConnectOptions = new MqttConnectOptions();
		mqttConnectOptions.setAutomaticReconnect(true);
		mqttConnectOptions.setCleanSession(false);

		try {
			mqttAndroidClient.connect(mqttConnectOptions, null, new IMqttActionListener() {
				@Override
				public void onSuccess(IMqttToken asyncActionToken) {
					Log.d(TAG, "Successfully connected");
					DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
					disconnectedBufferOptions.setBufferEnabled(true);
					disconnectedBufferOptions.setBufferSize(100);
					disconnectedBufferOptions.setPersistBuffer(false);
					disconnectedBufferOptions.setDeleteOldestMessages(false);
					mqttAndroidClient.setBufferOpts(disconnectedBufferOptions);
				}

				@Override
				public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
					Log.d(TAG, "Error while connecting");
				}
			});
		} catch (MqttException ex) {
			ex.printStackTrace();
		}


		return START_STICKY;
	}

	private final String TAG = this.getClass().getSimpleName();
	notiDelayList notiDelayList = new notiDelayList();

	private static boolean isTimeBetween(String fromTime, String toTime, String nowTime) throws ParseException {
		String reg = "^([0-1][0-9]|2[0-3]):([0-5][0-9])$";
		if (fromTime.matches(reg) && toTime.matches(reg) && nowTime.matches(reg)) {
			boolean valid;

			Date startTime = new SimpleDateFormat("HH:mm", Locale.US).parse(fromTime);
			Calendar startCalendar = Calendar.getInstance();
			startCalendar.setTime(startTime);

			Date currentTime = new SimpleDateFormat("HH:mm", Locale.US).parse(nowTime);
			Calendar currentCalendar = Calendar.getInstance();
			currentCalendar.setTime(currentTime);

			Date endTime = new SimpleDateFormat("HH:mm", Locale.US).parse(toTime);
			Calendar endCalendar = Calendar.getInstance();
			endCalendar.setTime(endTime);

			if (currentTime.compareTo(endTime) < 0) {
				currentCalendar.add(Calendar.DATE, 1);
				currentTime = currentCalendar.getTime();
			}

			if (startTime.compareTo(endTime) < 0) {
				startCalendar.add(Calendar.DATE, 1);
				startTime = startCalendar.getTime();
			}

			if (currentTime.before(startTime)) {
				valid = false;
			} else {
				if (currentTime.after(endTime)) {
					endCalendar.add(Calendar.DATE, 1);
					endTime = endCalendar.getTime();
				}

				valid = currentTime.before(endTime);
			}
			return valid;
		} else {
			throw new IllegalArgumentException("Not a valid time, expecting HH:mm format");
		}
	}

	@Override
	public void onNotificationPosted(StatusBarNotification notification) {
		Log.i(TAG, "*** NOTIFICATION POSTED **********");
		Log.i(TAG, "* ID      : " + notification.getId());
		Log.i(TAG, "* TEXT    : " + notification.getNotification().tickerText);
		Log.i(TAG, "* PACKAGE : " + notification.getPackageName());
		Log.i(TAG, "**********************************");

		if (!PreferenceManager.getDefaultSharedPreferences(this).getBoolean("debug_master_toggle", true)) {
			Log.i(TAG, "* ACTIONS DISABLED IN PREFERENCES");
			Log.i(TAG, "**********************************");
			return;
		}

		try {
			JSONArray savedRules = FilterRule.loadSavedFilterRules(this, false).getJSONArray("rules");

			for (int i = 0; i < savedRules.length(); i++) {
				JSONObject ruleObj = savedRules.getJSONObject(i);
				if (ruleObj == null)
					continue;
				JSONArray packages = ruleObj.getJSONArray("packageNames");
				for (int j = 0; j < packages.length(); j++) {
					if (packages.getString(j).equals(notification.getPackageName())) {
						FilterRule rule = new FilterRule(ruleObj);
						if(notiDelayList.isInTimeout(rule)) {
							notiDelayList.Update(rule);
							Log.i(TAG, "Execution prevented by timeout");
							return;
						}
						notiDelayList.Update(rule);
						Log.i(TAG, "Executing");
						String currentTime = new SimpleDateFormat("HH:mm", Locale.US).format(Calendar.getInstance().getTime());
						if (!rule.useDaytime() || isTimeBetween(rule.getFrom(), rule.getTo(), currentTime)) {
							SSHIdentity identity = SSHIdentity.fromID(this, rule.getIdentityID());
							new SSHClient.RemoteCommand(rule.getExec(), identity).execute();
						}
					}
				}
			}
		} catch (IOException | DataFormatException | JSONException | ParseException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void onNotificationRemoved(StatusBarNotification notification) {
		Log.i(TAG, "*** NOTIFICATION REMOVED *********");
		Log.i(TAG, "* ID      : " + notification.getId());
		Log.i(TAG, "* TEXT    : " + notification.getNotification().tickerText);
		Log.i(TAG, "* PACKAGE : " + notification.getPackageName());
		Log.i(TAG, "**********************************");

		// TODO: Add actions for removed Notifications
	}
}
