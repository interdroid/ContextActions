package interdroid.swan.contextactions;

import interdroid.swan.ExpressionManager;
import interdroid.swan.swansong.Expression;
import interdroid.swan.swansong.ExpressionFactory;
import interdroid.swan.swansong.ExpressionParseException;
import interdroid.swan.swansong.TriStateExpression;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Toast;

public class ActionsActivity extends PreferenceActivity implements
		OnPreferenceChangeListener {

	private Map<String, Intent> actionIntents = new HashMap<String, Intent>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setResult(RESULT_CANCELED);
		addPreferencesFromResource(R.xml.action_preferences);
		fillActionIntents();
		setup();
	}

	private void fillActionIntents() {
		PackageManager pm = getPackageManager();
		List<ResolveInfo> services = pm.queryIntentServices(new Intent(
				"interdroid.swan.contextactions.ACTION"),
				PackageManager.GET_META_DATA);
		for (ResolveInfo service : services) {
			Intent intent = new Intent();
			intent.setComponent(new ComponentName(
					service.serviceInfo.packageName, service.serviceInfo.name));
			intent.putExtra(ExpressionManager.EXTRA_INTENT_TYPE,
					ExpressionManager.INTENT_TYPE_SERVICE);
			actionIntents.put(
					service.serviceInfo.metaData.getString("description"),
					intent);
		}

	}

	private void fillActionIntentsTest() {
		// music intent
		Intent musicIntent = new Intent(Intent.ACTION_VIEW);
		musicIntent.setDataAndType(
				Uri.parse("file://" + Environment.getExternalStorageDirectory()
						+ "/beep.mp3"), "audio/mp3");
		musicIntent.putExtra(ExpressionManager.EXTRA_INTENT_TYPE,
				ExpressionManager.INTENT_TYPE_ACTIVITY);
		actionIntents.put("play beep", musicIntent);

		// nu.nl intent
		Intent webIntent = new Intent(Intent.ACTION_VIEW);
		webIntent.setData(Uri.parse("http://www.nu.nl/sport"));
		webIntent.putExtra(ExpressionManager.EXTRA_INTENT_TYPE,
				ExpressionManager.INTENT_TYPE_ACTIVITY);
		actionIntents.put("view nu.nl", webIntent);

		// VU office phone number
		Intent callIntent = new Intent(Intent.ACTION_CALL,
				Uri.parse("tel:0205987726"));
		callIntent.putExtra(ExpressionManager.EXTRA_INTENT_TYPE,
				ExpressionManager.INTENT_TYPE_ACTIVITY);
		actionIntents.put("call office", callIntent);

		actionIntents.put("nothing", null);
	}

	private void setup() {
		String[] entries = actionIntents.keySet().toArray(
				new String[actionIntents.size()]);
		findPreference("expression").setTitle(
				getIntent().getStringExtra("name"));
		findPreference("expression").setSummary(
				getIntent().getStringExtra("expression"));
		findPreference("expression").setOnPreferenceClickListener(
				new OnPreferenceClickListener() {

					@Override
					public boolean onPreferenceClick(Preference preference) {
						finish();
						return true;
					}
				});
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		for (int i = 1; i < getPreferenceScreen().getPreferenceCount(); i++) {
			ListPreference preference = (ListPreference) getPreferenceScreen()
					.getPreference(i);
			preference.setEntries(entries);
			preference.setEntryValues(entries);
			preference.setOnPreferenceChangeListener(this);
			preference.setSummary(prefs.getString(preference.getKey(),
					"nothing"));
		}

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.actions, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.action_next:
			// register expression
			try {
				String id = getIntent().getStringExtra("name");
				Expression expression = ExpressionFactory.parse(getIntent()
						.getStringExtra("expression"));
				SharedPreferences prefs = PreferenceManager
						.getDefaultSharedPreferences(this);
				if (prefs.contains(id)) {
					Toast.makeText(this,
							"Replacing expression with id '" + id + "'",
							Toast.LENGTH_LONG).show();
				}
				Set<String> knownExpressions = prefs.getStringSet(
						"expressions", new HashSet<String>());
				knownExpressions.add(id);
				prefs.edit()
						.putStringSet("expressions", knownExpressions)
						.putString(id, getIntent().getStringExtra("expression"))
						.putString(id + ".true",
								prefs.getString("true", "nothing"))
						.putString(id + ".false",
								prefs.getString("false", "nothing"))
						.putString(id + ".undefined",
								prefs.getString("undefined", "nothing"))
						.apply();
				ExpressionManager.registerTriStateExpression(
						ActionsActivity.this, id,
						(TriStateExpression) expression, actionIntents
								.get(prefs.getString("true", "nothing")),
						actionIntents.get(prefs.getString("false", "nothing")),
						actionIntents.get(prefs.getString("undefined",
								"nothing")));
				setResult(RESULT_OK);
				finish();
			} catch (ExpressionParseException e) {
				// should not occur
			}

		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public final boolean onPreferenceChange(final Preference preference,
			final Object newValue) {
		if (preference instanceof ListPreference) {
			for (int i = 0; i < ((ListPreference) preference).getEntryValues().length; i++) {
				if (((ListPreference) preference).getEntryValues()[i]
						.toString().equals(newValue.toString())) {
					preference.setSummary(((ListPreference) preference)
							.getEntries()[i]);
					return true;
				}
			}

		} else {
			preference.setSummary(newValue.toString());
		}
		return true;
	}

}
