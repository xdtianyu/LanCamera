package org.xdty.lancamera;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.support.annotation.Nullable;
import android.support.design.widget.AppBarLayout;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Display;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(android.R.id.content, new SettingsFragment())
                    .commit();
        }
    }

    public static class SettingsFragment extends PreferenceFragment {

        private SharedPreferences sharedPrefs;
        private Point mPoint;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings);

            sharedPrefs = getPreferenceManager().getSharedPreferences();

            WindowManager mWindowManager = (WindowManager) getActivity().
                    getSystemService(Context.WINDOW_SERVICE);
            Display display = mWindowManager.getDefaultDisplay();
            mPoint = new Point();
            display.getSize(mPoint);

            Preference preference =
                    findPreference(getString(R.string.server_address_key));

            preference.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    showEditDialog(R.string.server_address_key, R.string.server_address,
                            R.string.empty, R.string.server_address_hint);
                    return true;
                }
            });
        }

        private void showEditDialog(int keyId, int title, final int defaultText, int hint) {
            showEditDialog(keyId, title, defaultText, hint, 0, 0);
        }

        private void showEditDialog(int keyId, int title, final int defaultText, int hint,
                final int help, final int helpText) {
            final String key = getString(keyId);
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(title));
            View layout = View.inflate(getActivity(), R.layout.dialog_edit, null);
            builder.setView(layout);

            final EditText editText = (EditText) layout.findViewById(R.id.text);
            editText.setText(sharedPrefs.getString(key, getString(defaultText)));
            editText.setInputType(InputType.TYPE_CLASS_TEXT);
            if (hint > 0) {
                editText.setHint(hint);
            }

            builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    String value = editText.getText().toString();
                    if (value.isEmpty()) {
                        value = getString(defaultText);
                    }
                    findPreference(key).setSummary(value);
                    SharedPreferences.Editor editor = sharedPrefs.edit();
                    editor.putString(key, value);
                    editor.apply();
                }
            });
            builder.setNegativeButton(R.string.cancel, null);

            if (help != 0) {
                builder.setNeutralButton(help, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        showTextDialog(help, helpText);
                    }
                });
            }

            builder.setCancelable(true);
            builder.show();
        }

        @Override
        public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen,
                Preference preference) {
            super.onPreferenceTreeClick(preferenceScreen, preference);
            if (preference instanceof PreferenceScreen) {
                setUpNestedScreen((PreferenceScreen) preference);
            }
            return false;
        }

        public void setUpNestedScreen(PreferenceScreen preferenceScreen) {
            final Dialog dialog = preferenceScreen.getDialog();

            AppBarLayout appBarLayout;

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N
                    || Build.VERSION.RELEASE.equals("7.0") || Build.VERSION.RELEASE.equals("N")) {
                ListView listView = (ListView) dialog.findViewById(android.R.id.list);
                FrameLayout root = (FrameLayout) listView.getParent();

                appBarLayout = (AppBarLayout) LayoutInflater.from(getActivity()).inflate(
                        R.layout.settings_toolbar, root, false);

                int height;
                TypedValue tv = new TypedValue();
                if (getActivity().getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
                    height = TypedValue.complexToDimensionPixelSize(tv.data,
                            getResources().getDisplayMetrics());
                } else {
                    height = appBarLayout.getHeight();
                }
                listView.setPadding(0, height, 0, 0);
                root.addView(appBarLayout, 0);
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                LinearLayout root =
                        (LinearLayout) dialog.findViewById(android.R.id.list).getParent();
                appBarLayout = (AppBarLayout) LayoutInflater.from(getActivity()).inflate(
                        R.layout.settings_toolbar, root, false);
                root.addView(appBarLayout, 0);
            } else {
                ViewGroup root = (ViewGroup) dialog.findViewById(android.R.id.content);
                ListView content = (ListView) root.getChildAt(0);

                root.removeAllViews();

                appBarLayout = (AppBarLayout) LayoutInflater.from(getActivity()).inflate(
                        R.layout.settings_toolbar, root, false);

                int height;
                TypedValue tv = new TypedValue();
                if (getActivity().getTheme().resolveAttribute(R.attr.actionBarSize, tv, true)) {
                    height = TypedValue.complexToDimensionPixelSize(tv.data,
                            getResources().getDisplayMetrics());
                } else {
                    height = appBarLayout.getHeight();
                }

                content.setPadding((int) dpToPx(16), height, (int) dpToPx(16), 0);

                root.addView(content);
                root.addView(appBarLayout);
            }

            Toolbar toolbar = (Toolbar) appBarLayout.findViewById(R.id.toolbar);
            toolbar.setTitle(preferenceScreen.getTitle());
            toolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    dialog.dismiss();
                }
            });
        }

        private void showTextDialog(int title, int text) {
            showTextDialog(title, getString(text));
        }

        private void showTextDialog(int title, String text) {
            AlertDialog.Builder builder =
                    new AlertDialog.Builder(getActivity());
            builder.setTitle(getString(title));
            View layout = View.inflate(getActivity(), R.layout.dialog_text, null);
            builder.setView(layout);

            TextView textView = (TextView) layout.findViewById(R.id.text);
            textView.setText(text);

            builder.setPositiveButton(R.string.ok, null);
            builder.show();
        }

        private float dpToPx(float dp) {
            return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp,
                    getActivity().getResources().getDisplayMetrics());
        }
    }
}
