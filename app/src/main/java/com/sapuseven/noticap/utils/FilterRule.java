package com.sapuseven.noticap.utils;

import android.content.Context;
import android.renderscript.ScriptGroup;

import androidx.annotation.NonNull;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.zip.DataFormatException;

public class FilterRule {
	@NonNull
	private String name = "";
	@NonNull
	private String[] packageNames = new String[0];
	@NonNull
	private Boolean useDaytime = false;
	private String from;
	private String to;
	@NonNull
	private Long identityID = 0L;
	@NonNull
	private String exec_ssh = "";
	private String mqtt_payload = "";
	private String mqtt_topic = "";
	private String exec_type = "";
	private int minNotiDelay = 10000;

	public FilterRule() {
	}

	public FilterRule(@NonNull JSONObject rule) throws JSONException {
		name = rule.getString("name");
		ArrayList<String> packageNames = new ArrayList<>();
		for (int i = 0; i < rule.getJSONArray("packageNames").length(); i++) {
			try {
				packageNames.add(rule.getJSONArray("packageNames").getString(i));
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		this.packageNames = packageNames.toArray(this.packageNames);
		useDaytime = rule.getBoolean("useDaytime");
		if (useDaytime) {
			from = rule.getString("from");
			to = rule.getString("to");
		}
		mqtt_payload = rule.getString("mqtt_payload");
		mqtt_topic = rule.getString("mqtt_topic");
		exec_type = rule.getString("exec_type");
		minNotiDelay = rule.getInt("minNotiDelay");
		identityID = rule.getLong("identityID");
		exec_ssh = rule.getString("exec_ssh");
	}

	public static void saveRules(JSONObject data, File f) throws IOException
	{
		Compressor.writeFile(f, data.toString().getBytes("UTF-8"));
	}

	public static void saveRules(JSONObject data, OutputStream os) throws IOException
	{
		os.write(data.toString().getBytes("UTF-8"));
	}

	public static void saveRules(Context context, JSONObject data) throws IOException {
		File f = context.getFileStreamPath("rules");
		saveRules(data, f);
	}

	public static JSONObject loadSavedFilterRules(byte[] content, boolean overwrite) throws IOException, JSONException, DataFormatException {
		if (!overwrite && content.length > 0) {
			String data = new String(Compressor.decompress(content), "UTF-8");
			return new JSONObject(data);
		} else
			return new JSONObject().put("rules", new JSONArray());
	}

	public static JSONObject loadSavedFilterRules(File file, boolean overwrite) throws IOException, JSONException, DataFormatException {
		if (!file.exists())
			//noinspection ResultOfMethodCallIgnored
			file.createNewFile();

		byte[] content = Compressor.readFile(file);
		return loadSavedFilterRules(content, overwrite);
	}

	public static JSONObject loadSavedFilterRules(InputStream is, boolean overwrite) throws IOException, JSONException, DataFormatException {

		byte[] content = Compressor.readStream(is);
		if (!overwrite && content.length > 0) {
			String data = new String(content, "UTF-8");
			return new JSONObject(data);
		} else
			return new JSONObject().put("rules", new JSONArray());
	}

	public static JSONObject loadSavedFilterRules(Context context, boolean overwrite) throws IOException, DataFormatException, JSONException {
		File file = context.getFileStreamPath("rules");
		return loadSavedFilterRules(file, overwrite);
	}

	@Override
	public boolean equals(Object obj){
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		FilterRule other = (FilterRule)obj;
		return this.name.equals(other.name);
	}

	@NonNull
	public String getName() {
		return name;
	}

	public void setName(@NonNull String name) {
		this.name = name;
	}

	public JSONObject toJSONObject() throws JSONException {
		JSONObject result = new JSONObject();
		result.put("name", name);
		result.put("packageNames", new JSONArray(packageNames));
		result.put("useDaytime", useDaytime);
		result.put("minNotiDelay", minNotiDelay);
		if (useDaytime) {
			result.put("from", from);
			result.put("to", to);
		}
		result.put("mqtt_payload", mqtt_payload);
		result.put("mqtt_topic", mqtt_topic);
		result.put("exec_type", exec_type);
		result.put("identityID", identityID);
		result.put("exec_ssh", exec_ssh);
		return result;
	}

	public int getMinNotiDelay(){
		return minNotiDelay;
	}

	@NonNull
	public String[] getPackageNames() {
		return packageNames;
	}

	public void setMinNotiDelay(int timeDiff){
		this.minNotiDelay = timeDiff;
	}

	public void setPackageNames(@NonNull String[] packageNames) {
		this.packageNames = packageNames;
	}

	@NonNull
	public Boolean useDaytime() {
		return useDaytime;
	}

	public void setUseDaytime(@NonNull Boolean useDaytime) {
		this.useDaytime = useDaytime;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
	}

	public String getTo() {
		return to;
	}

	public void setTo(String to) {
		this.to = to;
	}

	@NonNull
	public Long getIdentityID() {
		return identityID;
	}

	public void setIdentityID(@NonNull Long identityID) {
		this.identityID = identityID;
	}

	@NonNull
	public String getExec_ssh() {
		return exec_ssh;
	}

	public void setExec_ssh(@NonNull String exec_ssh) {
		this.exec_ssh = exec_ssh;
	}

	public String getMqtt_payload() {
		return mqtt_payload;
	}

	public void setMqtt_payload(String mqtt_payload) {
		this.mqtt_payload = mqtt_payload;
	}

	public String getMqtt_topic() {
		return mqtt_topic;
	}

	public void setMqtt_topic(String mqtt_topic) {
		this.mqtt_topic = mqtt_topic;
	}

	public String getExec_type() {
		return exec_type;
	}

	public void setExec_type(String exec_type) {
		this.exec_type = exec_type;
	}
}