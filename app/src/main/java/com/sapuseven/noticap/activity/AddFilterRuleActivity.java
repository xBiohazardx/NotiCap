package com.sapuseven.noticap.activity;

import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.SpinnerAdapter;
import android.widget.Switch;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;
import com.sapuseven.noticap.R;
import com.sapuseven.noticap.utils.FilterRule;
import com.sapuseven.noticap.utils.SSHIdentity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Locale;
import java.util.zip.DataFormatException;

public class AddFilterRuleActivity extends AppCompatActivity {
	private final ArrayList<String> identities = new ArrayList<>();
	private EditText tvMinNotiDelay;
	private EditText tvName;
	private EditText tvFilterPackageName;
	private EditText tvMqttTopic;
	private EditText tvMqttPayload;
	private EditText tvExec_ssh;
	private Spinner identitiesDropDown;
	private String from = "06:00";
	private String to = "22:00";
	TabLayout layout;
	private final TimePickerDialog.OnTimeSetListener fromTimePickerListener = (view, selectedHour, selectedMinute) -> {
		from = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute);

		setDaytimeButtons();
	};
	private final TimePickerDialog.OnTimeSetListener toTimePickerListener = (view, selectedHour, selectedMinute) -> {
		to = String.format(Locale.US, "%02d:%02d", selectedHour, selectedMinute);

		setDaytimeButtons();
	};
	private Switch daytimeSwitch;

	private void setDaytimeButtons() {
		((Button) findViewById(R.id.button_from)).setText(from);
		((Button) findViewById(R.id.button_to)).setText(to);
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_add_filter);
		tvName = findViewById(R.id.name);
		tvFilterPackageName = findViewById(R.id.package_name);
		tvExec_ssh = findViewById(R.id.exec_ssh);
		identitiesDropDown = findViewById(R.id.identities);
		daytimeSwitch = findViewById(R.id.daytime_switch);
		tvMinNotiDelay = findViewById(R.id.minNotiDelay);
		tvMqttPayload = findViewById(R.id.mqtt_payload);
		tvMqttTopic = findViewById(R.id.mqtt_topic);

		layout = findViewById(R.id.exec_tablayout);
		layout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
			@Override
			public void onTabSelected(TabLayout.Tab tab) {
				setView(tab.getPosition());
			}

			@Override
			public void onTabUnselected(TabLayout.Tab tab) {

			}

			@Override
			public void onTabReselected(TabLayout.Tab tab) {

			}
		});
		setView(0);

        ActionBar actionBar = getSupportActionBar();
		if (actionBar != null)
			actionBar.setDisplayHomeAsUpEnabled(true);

		findViewById(R.id.add).setOnClickListener(view -> {
			try {
				saveFilter();
			} catch (JSONException | IOException | DataFormatException e) {
				e.printStackTrace();
			}
		});

		daytimeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
			findViewById(R.id.button_from).setEnabled(isChecked);
			findViewById(R.id.button_to).setEnabled(isChecked);
		});

		findViewById(R.id.button_from).setOnClickListener(v -> showDialog(0));

		findViewById(R.id.button_to).setOnClickListener(v -> showDialog(1));

		identitiesDropDown.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
			@Override
			public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
				if (position == identities.size() - 1) {
					startActivity(new Intent(getApplicationContext(), AddSSHIdentityActivity.class));
				}
			}

			@Override
			public void onNothingSelected(AdapterView<?> parent) {
				// Nothing to do
			}
		});

		if (getIntent().getBooleanExtra("modify", false)) {
			try {
				FilterRule rule = new FilterRule(FilterRule.loadSavedFilterRules(this, false).getJSONArray("rules").getJSONObject(getIntent().getIntExtra("index", 0)));
				tvName.setText(rule.getName());
				tvExec_ssh.setText(rule.getExec_ssh());
				tvFilterPackageName.setText(TextUtils.join("+", rule.getPackageNames()));
				tvMinNotiDelay.setText(Integer.toString((rule.getMinNotiDelay())));
				tvMqttTopic.setText(rule.getMqtt_topic());
				tvMqttPayload.setText(rule.getMqtt_payload());
				if (rule.useDaytime()) {
					daytimeSwitch.setChecked(true);
					from = rule.getFrom();
					to = rule.getTo();
					setDaytimeButtons();
				}
			} catch (IOException | DataFormatException | JSONException e) {
				e.printStackTrace();
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();

		FilterRule rule = null;
		try {
			rule = new FilterRule(FilterRule.loadSavedFilterRules(this, false).getJSONArray("rules").getJSONObject(getIntent().getIntExtra("index", 0)));
		} catch (JSONException | IOException | DataFormatException e) {
			e.printStackTrace();
		}

		identities.clear();
		identities.add("");
		int selection = 0;
		try {
			JSONArray savedIdentities = SSHIdentity.loadSavedIdentities(this, false).getJSONArray("identities");

			for (int i = 0; i < savedIdentities.length(); i++) {
				JSONObject identityObj = savedIdentities.getJSONObject(i);
				SSHIdentity identity = new SSHIdentity(identityObj);
				identities.add(identity.getName());
				if (rule != null && rule.getIdentityID().equals(identity.getId()))
					selection = i + 1;
			}
		} catch (IOException | DataFormatException | JSONException e) {
			e.printStackTrace();
		}
		identities.add(getString(R.string.add_new_identity));
		SpinnerAdapter adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, identities);
		identitiesDropDown.setAdapter(adapter);
		identitiesDropDown.setSelection(selection);
	}

	private void saveFilter() throws JSONException, IOException, DataFormatException {
		tvName.setError(null);

		String name = tvName.getText().toString();
		String packageNameFilter = tvFilterPackageName.getText().toString();
		String exec_ssh = tvExec_ssh.getText().toString();
		String mqtt_payload = tvMqttPayload.getText().toString();
		String mqtt_topic = tvMqttTopic.getText().toString();
		String exec_type = "";
		if(layout.getSelectedTabPosition() == 0)
			exec_type = "mqtt";
		else
			exec_type =  "ssh";

		int minNotiDelay = 0;
		if(!tvMinNotiDelay.getText().toString().isEmpty())
			minNotiDelay = Integer.parseInt(tvMinNotiDelay.getText().toString());

		boolean cancel = false;
		View focusView = null;

		if (TextUtils.isEmpty(name)) {
			tvName.setError(getString(R.string.error_field_required));
			focusView = tvName;
			cancel = true;
		} else if (TextUtils.isEmpty(packageNameFilter)) {
			tvFilterPackageName.setError(getString(R.string.error_field_required));
			focusView = tvFilterPackageName;
			cancel = true;
		} else if (TextUtils.isEmpty(exec_ssh) && exec_type.equals("ssh")) {
			tvExec_ssh.setError(getString(R.string.error_field_required));
			focusView = tvExec_ssh;
			cancel = true;
		} else if (TextUtils.isEmpty(mqtt_payload) && exec_type.equals("mqtt")) {
			tvMqttPayload.setError(getString(R.string.error_field_required));
			focusView = tvMqttPayload;
			cancel = true;
		}else if (TextUtils.isEmpty(mqtt_topic) && exec_type.equals("mqtt")) {
				tvMqttTopic.setError(getString(R.string.error_field_required));
				focusView = tvMqttTopic;
				cancel = true;
		} else {
			for (String packageName : packageNameFilter.split("\\+")) {
				if (!packageName.matches("^[a-z][a-z0-9_]*(\\.[a-z0-9_]+)+[0-9a-z_]?$")) {
					tvFilterPackageName.setError(getString(R.string.invalid_package_name, packageName));
					focusView = tvFilterPackageName;
					cancel = true;
				}
			}
		}

		if (cancel) {
			focusView.requestFocus();
		} else {
			final FilterRule filter = new FilterRule();
			filter.setName(name);
			filter.setPackageNames(packageNameFilter.split("\\+"));
			filter.setMinNotiDelay(minNotiDelay);
			filter.setUseDaytime(daytimeSwitch.isChecked());
			if (daytimeSwitch.isChecked()) {
				filter.setFrom(from);
				filter.setTo(to);
			}
			filter.setIdentityID(SSHIdentity.loadSavedIdentities(this, false).getJSONArray("identities").getJSONObject(identitiesDropDown.getSelectedItemPosition() - 1).getLong("id"));
			filter.setExec_ssh(exec_ssh);
			filter.setMqtt_payload(mqtt_payload);
			filter.setMqtt_topic(mqtt_topic);
			filter.setExec_type(exec_type);
			if (addFilter(filter, false)) {
				new AlertDialog.Builder(this)
						.setTitle(R.string.filter_rule_storage_corrupted_title)
						.setMessage(getString(R.string.filter_rule_storage_corrupted_body))
						.setPositiveButton(R.string.yes, (dialog, which) -> addFilter(filter, true))
						.setNegativeButton(R.string.no, (dialog, which) -> dialog.dismiss())
						.setIcon(android.R.drawable.ic_dialog_alert)
						.show();
			} else {
				finish();
			}
		}
	}

	private void setView(int position){
		switch(position){
			case 0:
				findViewById(R.id.mqtt_container).setVisibility(View.VISIBLE);
				findViewById(R.id.ssh_container).setVisibility(View.GONE);
				break;
			case 1:
				findViewById(R.id.ssh_container).setVisibility(View.VISIBLE);
				findViewById(R.id.mqtt_container).setVisibility(View.GONE);
				break;
		}
	}

	private boolean addFilter(FilterRule filterRule, boolean overwrite) {
		try {
			JSONObject data = FilterRule.loadSavedFilterRules(this, overwrite);
			if (getIntent().getBooleanExtra("modify", false))
				data.getJSONArray("rules").put(getIntent().getIntExtra("index", 0), filterRule.toJSONObject());
			else
				data.accumulate("rules", filterRule.toJSONObject());
			FilterRule.saveRules(this, data);
			return false;
		} catch (Exception e) {
			e.printStackTrace();
			if (overwrite)
				new AlertDialog.Builder(this)
						.setTitle(R.string.error_write_file)
						.setMessage(getString(R.string.error_saving_filter_rule, e.getMessage()))
						.setNeutralButton(getString(R.string.ok), (dialog, which) -> dialog.dismiss())
						.show();
			return true;
		}
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();
		if (id == android.R.id.home) {
			onBackPressed();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected Dialog onCreateDialog(int id) {
		switch (id) {
			case 0:
				return new TimePickerDialog(this, fromTimePickerListener, Integer.parseInt(from.substring(0, 2)), Integer.parseInt(from.substring(3)), true);
			case 1:
				return new TimePickerDialog(this, toTimePickerListener, Integer.parseInt(to.substring(0, 2)), Integer.parseInt(to.substring(3)), true);
			default:
				return null;
		}
	}
}