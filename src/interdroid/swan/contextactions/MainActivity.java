package interdroid.swan.contextactions;

import interdroid.swan.ExpressionManager;
import interdroid.swan.SensorInfo;
import interdroid.swan.SwanException;
import interdroid.swan.swansong.BinaryLogicOperator;
import interdroid.swan.swansong.Comparator;
import interdroid.swan.swansong.ComparisonExpression;
import interdroid.swan.swansong.ConstantValueExpression;
import interdroid.swan.swansong.Expression;
import interdroid.swan.swansong.ExpressionFactory;
import interdroid.swan.swansong.ExpressionParseException;
import interdroid.swan.swansong.HistoryReductionMode;
import interdroid.swan.swansong.LogicExpression;
import interdroid.swan.swansong.MathOperator;
import interdroid.swan.swansong.MathValueExpression;
import interdroid.swan.swansong.SensorValueExpression;
import interdroid.swan.swansong.TriStateExpression;
import interdroid.swan.swansong.UnaryLogicOperator;
import interdroid.swan.swansong.ValueExpression;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.ArrayAdapter;
import android.widget.CheckedTextView;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends ListActivity {

	private static final int REQUEST_ADD_SENSOR = 1;
	private static final int REQUEST_EDIT_SENSOR = 2;
	private static final int REQUEST_TRIGGER = 3;

	private List<NamedExpression> expressions = new ArrayList<NamedExpression>();
	private NamedExpression editedExpression;
	private NamedExpression triggeredExpression;
	private ExpressionAdapter adapter;

	@Override
	protected void onPause() {
		super.onPause();
		saveExpressions();

	}

	private void saveExpressions() {
		System.out.println("saving expressions...");
		SharedPreferences.Editor editor = PreferenceManager
				.getDefaultSharedPreferences(this).edit();
		editor.putInt("#expressions", expressions.size());
		for (int i = 0; i < expressions.size(); i++) {
			System.out.println(i + "S-> "
					+ expressions.get(i).expression.toParseString());
			editor.putString(i + ".expression",
					expressions.get(i).expression.toParseString());
			editor.putString(i + ".name", expressions.get(i).name);
			editor.putBoolean(i + ".triggered", expressions.get(i).triggered);
			editor.putBoolean(i + ".autodelete", expressions.get(i).autoDelete);
		}
		editor.apply();
		System.out.println("saving expressions... done");
	}

	@Override
	protected void onResume() {

		expressions.clear();
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		System.out.println("resuming... (" + prefs.getInt("#expressions", 0)
				+ ")");
		for (int i = 0; i < prefs.getInt("#expressions", 0); i++) {
			NamedExpression expression = new NamedExpression();
			System.out.println(i + " -> "
					+ prefs.getString(i + ".expression", null));
			try {
				expression.expression = ExpressionFactory.parse(prefs
						.getString(i + ".expression", null));
			} catch (ExpressionParseException e) {
				e.printStackTrace();
			}
			expression.name = prefs.getString(i + ".name", null);
			expression.triggered = prefs.getBoolean(i + ".triggered", false);
			expression.autoDelete = prefs.getBoolean(i + ".autodelete", false);
			adapter.add(expression);
		}
		System.out.println("resuming done...");
		super.onResume();
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final ListView listView = getListView();
		listView.setChoiceMode(ListView.CHOICE_MODE_MULTIPLE_MODAL);
		listView.setMultiChoiceModeListener(new MultiChoiceModeListener() {

			private List<NamedExpression> selected = new ArrayList<NamedExpression>();

			@Override
			public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
				// update selected
				for (int i = 0; i < expressions.size(); i++) {
					if (expressions.get(i).selected) {
						if (!selected.contains(expressions.get(i))) {
							selected.add(expressions.get(i));
						}
					} else {
						selected.remove(expressions.get(i));
					}
				}
				menu.clear();
				if (selected.size() == 1) {
					if (selected.get(0).expression instanceof TriStateExpression) {
						mode.getMenuInflater().inflate(
								R.menu.single_tristate_cab, menu);
					}
					mode.getMenuInflater().inflate(R.menu.single_default_cab,
							menu);

				} else {
					boolean allTriState = true;
					boolean allValue = true;
					for (NamedExpression expression : selected) {
						if (!(expression.expression instanceof TriStateExpression)) {
							allTriState = false;
						}
						if (!(expression.expression instanceof ValueExpression)) {
							allValue = false;
						}
					}
					if (allTriState) {
						mode.getMenuInflater().inflate(
								R.menu.multi_tristate_cab, menu);
					}
					if (allValue) {
						mode.getMenuInflater().inflate(R.menu.multi_value_cab,
								menu);
					}

				}
				mode.getMenuInflater().inflate(R.menu.default_cab, menu);
				return true;
			}

			@Override
			public void onDestroyActionMode(ActionMode mode) {
				for (NamedExpression expression : expressions) {
					expression.selected = false;
				}
				selected.clear();
				adapter.notifyDataSetChanged();
			}

			@Override
			public boolean onCreateActionMode(ActionMode mode, Menu menu) {
				return true;
			}

			private void handleMathAction(MathOperator operator) {

				while (selected.size() >= 2) {
					int minIndex = expressions.size();
					NamedExpression result = new NamedExpression();
					result.name = selected.get(0).name + " "
							+ operator.toString() + " " + selected.get(1).name;
					result.expression = new MathValueExpression(
							Expression.LOCATION_INFER,
							(ValueExpression) selected.get(0).expression,
							operator,
							(ValueExpression) selected.get(1).expression,
							HistoryReductionMode.ANY);
					for (int i = 0; i < 2; i++) {
						NamedExpression removed = selected.remove(0);
						if (removed.autoDelete && !removed.triggered) {
							minIndex = Math.min(minIndex,
									expressions.indexOf(removed));
							expressions.remove(removed);
						}
					}
					selected.add(0, result);
					expressions.add(minIndex, result);
				}

			}

			private void handleComparisonAction(Comparator comparator) {

				while (selected.size() >= 2) {
					int minIndex = expressions.size();
					NamedExpression result = new NamedExpression();
					result.name = selected.get(0).name + " "
							+ comparator.toString() + " "
							+ selected.get(1).name;
					result.expression = new ComparisonExpression(
							Expression.LOCATION_INFER,
							(ValueExpression) selected.get(0).expression,
							comparator,
							(ValueExpression) selected.get(1).expression);
					for (int i = 0; i < 2; i++) {
						NamedExpression removed = selected.remove(0);
						if (removed.autoDelete && !removed.triggered) {
							minIndex = Math.min(minIndex,
									expressions.indexOf(removed));
							expressions.remove(removed);
						}
					}
					selected.add(0, result);
					expressions.add(minIndex, result);
				}
			}

			private void handleLogicAction(BinaryLogicOperator operator) {
				while (selected.size() >= 2) {
					int minIndex = expressions.size();
					NamedExpression result = new NamedExpression();
					result.name = selected.get(0).name
							+ " "
							+ (operator == BinaryLogicOperator.AND ? "AND"
									: "OR") + " " + selected.get(1).name;
					result.expression = new LogicExpression(
							Expression.LOCATION_INFER,
							(TriStateExpression) selected.get(0).expression,
							operator,
							(TriStateExpression) selected.get(1).expression);
					for (int i = 0; i < 2; i++) {
						NamedExpression removed = selected.remove(0);
						if (removed.autoDelete && !removed.triggered) {
							minIndex = Math.min(minIndex,
									expressions.indexOf(removed));
							expressions.remove(removed);
						}
					}
					selected.add(0, result);
					expressions.add(minIndex, result);
				}
			}

			@Override
			public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
				final NamedExpression expression = selected.get(0);
				AlertDialog.Builder builder = new AlertDialog.Builder(
						MainActivity.this);

				switch (item.getItemId()) {
				case R.id.action_autodelete:
					for (int i = expressions.size() - 1; i >= 0; i--) {
						if (expressions.get(i).selected) {
							expressions.get(i).autoDelete = !expressions.get(i).autoDelete
									|| expressions.get(i).triggered;
						}
					}
					adapter.notifyDataSetChanged();
					break;
				case R.id.action_trigger:
					Intent actionIntent = new Intent(MainActivity.this,
							ActionsActivity.class);
					actionIntent.putExtra("expression",
							expression.expression.toParseString());
					actionIntent.putExtra("name", expression.name);
					triggeredExpression = expression;
					startActivityForResult(actionIntent, REQUEST_TRIGGER);
					break;
				case R.id.action_delete:
					for (int i = expressions.size() - 1; i >= 0; i--) {
						if (expressions.get(i).selected) {
							if (expressions.get(i).triggered) {
								// unregister, remove actions from shared prefs
								ExpressionManager.unregisterExpression(
										MainActivity.this,
										expressions.get(i).name);
								SharedPreferences prefs = PreferenceManager
										.getDefaultSharedPreferences(MainActivity.this);
								SharedPreferences.Editor editor = prefs.edit();
								Set<String> known = prefs.getStringSet(
										"expressions", new HashSet<String>());
								known.remove(expressions.get(i).name);
								editor.putStringSet("expressions", known);
								editor.remove(expressions.get(i).name);
								editor.remove(expressions.get(i).name + ".true");
								editor.remove(expressions.get(i).name
										+ ".false");
								editor.remove(expressions.get(i).name
										+ ".undefined");
								editor.apply();
							}
							expressions.remove(i);
						}
					}
					break;
				case R.id.action_negate:
					selected.get(0).name = "not " + selected.get(0).name;
					selected.get(0).expression = new LogicExpression(
							Expression.LOCATION_INFER, UnaryLogicOperator.NOT,
							(TriStateExpression) selected.get(0).expression);
					break;
				case R.id.action_and:
					handleLogicAction(BinaryLogicOperator.AND);
					break;
				case R.id.action_or:
					handleLogicAction(BinaryLogicOperator.OR);
					break;
				case R.id.action_rename:
					final View renameView = LayoutInflater.from(
							MainActivity.this).inflate(R.layout.dialog_name,
							null);
					if (selected.get(0).triggered) {
						Toast.makeText(
								MainActivity.this,
								"cannot rename, expression is already triggered",
								Toast.LENGTH_LONG).show();
						break;
					}
					((EditText) renameView.findViewById(R.id.name))
							.setText(selected.get(0).name);
					builder.setTitle(R.string.dialog_edit_name_title);
					builder.setView(renameView);
					builder.setPositiveButton(android.R.string.ok,
							new OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									expression.name = ((EditText) renameView
											.findViewById(R.id.name)).getText()
											.toString();
									adapter.notifyDataSetChanged();
								}
							});
					builder.create().show();

					break;
				case R.id.action_edit:
					if (expression.expression instanceof SensorValueExpression) {
						// start edit intent
						try {
							Intent intent = ExpressionManager
									.getSensor(
											MainActivity.this,
											((SensorValueExpression) expression.expression)
													.getEntity())
									.getConfigurationIntent();
							intent.putExtra("expression",
									expression.expression.toParseString());
							editedExpression = expression;
							startActivityForResult(intent, REQUEST_EDIT_SENSOR);
						} catch (SwanException e) {
							e.printStackTrace();
						}

					} else if (expression.expression instanceof ConstantValueExpression) {
						// edit constant
						builder.setTitle(R.string.dialog_add_constant_title);
						final View constantView = LayoutInflater.from(
								MainActivity.this).inflate(
								R.layout.dialog_constant, null);
						((EditText) constantView.findViewById(R.id.value))
								.setText(""
										+ ((ConstantValueExpression) expression.expression)
												.getResult().getValues()[0]
												.getValue());
						builder.setView(constantView);
						builder.setPositiveButton(android.R.string.ok,
								new OnClickListener() {

									@Override
									public void onClick(DialogInterface dialog,
											int which) {
										String value = ((EditText) constantView
												.findViewById(R.id.value))
												.getText().toString();
										try {
											Long valueLong = Long
													.parseLong(value);
											expression.expression = new ConstantValueExpression(
													valueLong);
										} catch (NumberFormatException e) {
											try {
												Double valueDouble = Double
														.parseDouble(value);
												expression.expression = new ConstantValueExpression(
														valueDouble);
											} catch (NumberFormatException e2) {
												// it was not a double, it must
												// be a string
												expression.expression = new ConstantValueExpression(
														value);
											}
										}
										adapter.notifyDataSetChanged();
									}
								});
						builder.create().show();
					} else {
						Toast.makeText(MainActivity.this,
								R.string.toast_cannot_edit, Toast.LENGTH_SHORT)
								.show();
					}
					break;
				case R.id.action_plus:
					handleMathAction(MathOperator.PLUS);
					break;
				case R.id.action_minus:
					handleMathAction(MathOperator.MINUS);
					break;
				case R.id.action_multiply:
					handleMathAction(MathOperator.TIMES);
					break;
				case R.id.action_divide:
					handleMathAction(MathOperator.DIVIDE);
					break;
				case R.id.action_modulo:
					handleMathAction(MathOperator.MOD);
					break;
				case R.id.action_greater_than:
					handleComparisonAction(Comparator.GREATER_THAN);
					break;
				case R.id.action_greater_than_equals:
					handleComparisonAction(Comparator.GREATER_THAN_OR_EQUALS);
					break;
				case R.id.action_equals:
					handleComparisonAction(Comparator.EQUALS);
					break;
				case R.id.action_not_equals:
					handleComparisonAction(Comparator.NOT_EQUALS);
					break;
				case R.id.action_less_than_equals:
					handleComparisonAction(Comparator.LESS_THAN_OR_EQUALS);
					break;
				case R.id.action_less_than:
					handleComparisonAction(Comparator.LESS_THAN);
					break;
				case R.id.action_contains:
					handleComparisonAction(Comparator.STRING_CONTAINS);
					break;
				case R.id.action_regex:
					handleComparisonAction(Comparator.REGEX_MATCH);
					break;
				}
				if (item.getItemId() != R.id.menu_comparison
						&& item.getItemId() != R.id.menu_math
						&& item.getItemId() != R.id.menu_edit) {
					mode.finish();
				}
				return true;
			}

			@Override
			public void onItemCheckedStateChanged(ActionMode mode,
					int position, long id, boolean checked) {
				mode.setTitle(getListView().getCheckedItemCount() + " selected");
				expressions.get(position).selected = checked;
				adapter.notifyDataSetChanged();
				mode.invalidate();
			}
		});

		adapter = new ExpressionAdapter(expressions);
		adapter.setNotifyOnChange(true);

		// SharedPreferences prefs = PreferenceManager
		// .getDefaultSharedPreferences(this);
		// Set<String> knownExpressions = prefs.getStringSet("expressions",
		// new HashSet<String>());
		//
		// for (String id : knownExpressions) {
		// NamedExpression known = new NamedExpression();
		// known.name = id;
		// try {
		// known.expression = ExpressionFactory.parse(prefs.getString(id,
		// null));
		// } catch (ExpressionParseException e) {
		// // ignore
		// }
		// known.triggered = true;
		// adapter.add(known);
		// }

		setListAdapter(adapter);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		switch (item.getItemId()) {
		case R.id.action_add_sensor:
			builder.setSingleChoiceItems(
					new SensorAdapter(ExpressionManager.getSensors(this)), -1,
					new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							Intent intent = ((SensorInfo) ((AlertDialog) dialog)
									.getListView().getAdapter().getItem(which))
									.getConfigurationIntent();
							dialog.dismiss();
							startActivityForResult(intent, REQUEST_ADD_SENSOR);
						}
					});
			builder.setTitle(R.string.dialog_add_sensor_title);
			builder.create().show();
			break;
		case R.id.action_add_constant:
			final View constantView = LayoutInflater.from(this).inflate(
					R.layout.dialog_constant, null);
			builder.setView(constantView);
			builder.setTitle(R.string.dialog_add_constant_title);
			builder.setPositiveButton(android.R.string.ok,
					new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							NamedExpression named = new NamedExpression();
							String value = ((EditText) constantView
									.findViewById(R.id.value)).getText()
									.toString();
							named.name = value;
							try {
								Long valueLong = Long.parseLong(value);
								named.expression = new ConstantValueExpression(
										valueLong);
							} catch (NumberFormatException e) {
								try {
									Double valueDouble = Double
											.parseDouble(value);
									named.expression = new ConstantValueExpression(
											valueDouble);
								} catch (NumberFormatException e2) {
									// it was not a double, it must be a string
									named.expression = new ConstantValueExpression(
											value);
								}
							}
							expressions.add(named);

						}
					});
			builder.create().show();
			break;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == RESULT_OK) {
			switch (requestCode) {
			case REQUEST_TRIGGER:
				triggeredExpression.triggered = true;
				adapter.notifyDataSetChanged();
				break;
			case REQUEST_EDIT_SENSOR:
				try {
					Expression expression = ExpressionFactory.parse(data
							.getStringExtra("Expression"));
					editedExpression.expression = expression;
					adapter.notifyDataSetChanged();
				} catch (ExpressionParseException e) {
					e.printStackTrace();
				}
				break;
			case REQUEST_ADD_SENSOR:
				try {
					Expression expression = ExpressionFactory.parse(data
							.getStringExtra("Expression"));

					NamedExpression named = new NamedExpression();
					named.expression = expression;
					named.name = ((SensorValueExpression) expression)
							.getEntity();
					adapter.add(named);
					saveExpressions();
				} catch (ExpressionParseException e) {
					e.printStackTrace();
				}
				break;
			default:
				break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	class NamedExpression {

		public NamedExpression() {
			autoDelete = true;
			triggered = false;
			selected = false;
		}

		public Expression expression;
		public String name;
		public boolean selected;
		public boolean triggered;
		public boolean autoDelete;

	}

	class ExpressionAdapter extends ArrayAdapter<NamedExpression> {

		ExpressionAdapter(List<NamedExpression> objects) {
			super(MainActivity.this, R.layout.item_expression, objects);
		}

		@Override
		public View getView(final int position, View convertView,
				ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(
						R.layout.item_expression, null);
			}

			TypedArray ta = MainActivity.this
					.obtainStyledAttributes(new int[] { android.R.attr.activatedBackgroundIndicator });
			convertView.setBackgroundDrawable(ta.getDrawable(0));
			ta.recycle();

			convertView.findViewById(R.id.triggered)
					.setVisibility(
							getItem(position).triggered ? View.VISIBLE
									: View.INVISIBLE);
			convertView.findViewById(R.id.autodelete).setVisibility(
					getItem(position).autoDelete
							&& !getItem(position).triggered ? View.INVISIBLE
							: View.VISIBLE);

			((TextView) convertView.findViewById(R.id.name))
					.setText(getItem(position).name);
			((TextView) convertView.findViewById(R.id.expression))
					.setText(getItem(position).expression.toParseString());
			((CheckedTextView) convertView.findViewById(R.id.name))
					.setChecked(getItem(position).selected);
			((CheckedTextView) convertView.findViewById(R.id.name))
					.setOnClickListener(new View.OnClickListener() {

						@Override
						public void onClick(View v) {
							getListView().setItemChecked(position,
									!getItem(position).selected);
						}
					});
			return convertView;
		}
	}

	class SensorAdapter extends ArrayAdapter<SensorInfo> {

		SensorAdapter(List<SensorInfo> objects) {
			super(MainActivity.this, R.layout.item_sensor, objects);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			if (convertView == null) {
				convertView = LayoutInflater.from(getContext()).inflate(
						R.layout.item_sensor, null);
			}
			((TextView) convertView.findViewById(R.id.name)).setText(getItem(
					position).getEntity());
			((ImageView) convertView.findViewById(R.id.icon))
					.setImageDrawable(getItem(position).getIcon());
			return convertView;
		}
	}

}
