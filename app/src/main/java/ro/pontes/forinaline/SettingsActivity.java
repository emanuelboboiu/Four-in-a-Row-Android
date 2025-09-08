package ro.pontes.forinaline;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.View;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

public class SettingsActivity extends Activity {

    private final Context finalContext = this;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        // Set the orientation to be portrait if needed:
        if (MainActivity.isOrientationBlocked && !MainActivity.isTV) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } // end if isOrientationBlocked is true.

        // Check or check the check boxes, depending of current boolean values:

        // For speech in program:
        CheckBox cbtSpeechSetting = findViewById(R.id.cbtSpeechSetting);
        cbtSpeechSetting.setChecked(MainActivity.isSpeech);

        // For sounds in program:
        CheckBox cbtSoundsSetting = findViewById(R.id.cbtSoundsSetting);
        cbtSoundsSetting.setChecked(MainActivity.isSound);

        // For music in program:
        CheckBox cbtMusicSetting = findViewById(R.id.cbtMusicSetting);
        cbtMusicSetting.setChecked(MainActivity.isMusic);

        // For portrait orientation:
        CheckBox cbtPortraitOrientationSetting = findViewById(R.id.cbtPortraitOrientationSetting);
        if (MainActivity.isTV) {
            // For Android TV we need it to be unavailable and unchecked:
            cbtPortraitOrientationSetting.setChecked(false);
            cbtPortraitOrientationSetting.setEnabled(false);
        } else {
            cbtPortraitOrientationSetting
                    .setChecked(MainActivity.isOrientationBlocked);
        }

        // For keeping screen awake:
        CheckBox cbtScreenAwakeSetting = findViewById(R.id.cbtScreenAwakeSetting);
        if (MainActivity.isTV) {
            // For Android TV we need it to be unavailable and unchecked:
            cbtScreenAwakeSetting.setChecked(false);
            cbtScreenAwakeSetting.setEnabled(false);
        } else {
            cbtScreenAwakeSetting.setChecked(MainActivity.isWakeLock);
        }
    } // end onCreate() method.

    // Let's see what happens when a check box is clicked in audio settings:
    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        Settings set = new Settings(this); // to save changes.

        int id = view.getId();

        if (id == R.id.cbtSpeechSetting) {
            MainActivity.isSpeech = checked;
            set.saveBooleanSettings("isSpeech", MainActivity.isSpeech);

        } else if (id == R.id.cbtSoundsSetting) {
            MainActivity.isSound = checked;
            set.saveBooleanSettings("isSound", MainActivity.isSound);

        } else if (id == R.id.cbtMusicSetting) {
            MainActivity.isMusic = checked;
            set.saveBooleanSettings("isMusic", MainActivity.isMusic);

        } else if (id == R.id.cbtPortraitOrientationSetting) {
            MainActivity.isOrientationBlocked = checked;
            set.saveBooleanSettings("isOrientationBlocked", MainActivity.isOrientationBlocked);

        } else if (id == R.id.cbtScreenAwakeSetting) {
            MainActivity.isWakeLock = checked;
            set.saveBooleanSettings("isWakeLock", MainActivity.isWakeLock);
        }
    } // end onClick method.

    // The method to reset to defaults:
    public void resetToDefaults(View view) {
        // Only if the game is not in progress:
        if (!MainActivity.isStarted) {
            // Make an alert with the question:
            // Get the strings to make an alert:
            String tempTitle = getString(R.string.title_default_settings);
            String tempBody = getString(R.string.body_default_settings);
            Context context = new ContextThemeWrapper(this,
                    R.style.MyAlertDialog);
            AlertDialog.Builder alert = new AlertDialog.Builder(context);

            alert.setTitle(tempTitle);
            ScrollView sv = new ScrollView(context);
            LinearLayout ll = new LinearLayout(context);
            ll.setOrientation(LinearLayout.VERTICAL);

            TextView tv = new TextView(context);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainActivity.textSize);
            tv.setFocusable(true);
            tv.setText(tempBody);
            ll.addView(tv);

            // Add now the LinearLayout into ScrollView:
            sv.addView(ll);

            alert.setView(sv);

            alert.setIcon(R.drawable.ic_launcher)
                    .setPositiveButton(R.string.msg_yes,
                            (dialog, whichButton) -> {
                                Settings set = new Settings(finalContext);
                                set.setDefaultSettings();
                                set.chargeSettings();
                                recreateThisActivity();
                            }).setNegativeButton(R.string.msg_no, null).show();
        } // end if the game is not in progress.
        else {
            // Announce that's impossible the reset during a game:
            GUITools.alert(this, getString(R.string.warning),
                    getString(R.string.warning_no_available_if_started),
                    getString(R.string.msg_ok));
        }
    } // end resetToDefaults() method.

    // A method which recreates this activity:
    private void recreateThisActivity() {
        startActivity(getIntent());
        finish();
    } // end recreateThisActivity() method.

    @Override
    public void onBackPressed() {
        this.finish();
        GUITools.goToMainActivity(this);
    } // end onBackPressed()

} // end SettingsActivity class.
