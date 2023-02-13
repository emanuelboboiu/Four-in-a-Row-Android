package ro.pontes.fourinaline;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.UiModeManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.text.InputType;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.accessibility.AccessibilityManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Random;

import ro.pontes.forinaline.R;

public class GUITools {

    /*
     * We need a global alertToShow as alert to be able to dismiss it when
     * needed and other things:
     */
    public static AlertDialog alertToShow;

    // A method to show an alert with title and message, just a dismiss button:
    public static void alert(Context parentContext, String title, String message, String buttonName) {
        Context context = new ContextThemeWrapper(parentContext, R.style.MyAlertDialog);
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        // The title:
        alert.setTitle(title);

        // The body creation:
        // Create a LinearLayout with ScrollView with all contents as TextViews:
        ScrollView sv = new ScrollView(context);
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);

        String[] mParagraphs = message.split("\n");

        // A for for each paragraph in the message as TextView:
        for (String mParagraph : mParagraphs) {
            TextView tv = new TextView(context);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainActivity.textSize);
            tv.setFocusable(true);
            tv.setText(mParagraph);
            ll.addView(tv);
        } // end for.

        // Add now the LinearLayout into ScrollView:
        sv.addView(ll);

        alert.setView(sv);

        alert.setPositiveButton(buttonName, (dialog, whichButton) -> {
            // Do nothing yet...
        });
        alert.show();
    } // end alert static method.

    // A static method to get a random number between two integers:
    public static int random(int min, int max) {
        Random rand = new Random();
        return rand.nextInt((max - min) + 1) + min;
    } // end random method.

    public static boolean isAndroidTV(final Context context) {
        UiModeManager uiModeManager = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
        return uiModeManager.getCurrentModeType() == Configuration.UI_MODE_TYPE_TELEVISION;
    } // end isAndroidTV() method.

    // A method for an alert to change nickname:
    public static void changeNickname(final Context parentContext) {
        final Context context = new ContextThemeWrapper(parentContext, R.style.MyAlertDialog);
        AlertDialog.Builder alert = new AlertDialog.Builder(context);

        // The title:
        alert.setTitle(context.getString(R.string.nickname_title));

        // The body:
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);

        // A LayoutParams to add next items into addLLMain:
        LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

        // The text view where we say about nickname:
        TextView tv = new TextView(context);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainActivity.textSize);
        String tempMessage;
        if (MainActivity.nickname == null || MainActivity.nickname.equals("")) {
            // It means no nickname was already set:
            tempMessage = context.getString(R.string.change_nickname_message);
        } else {
            // There's a nickname already set:
            tempMessage = String.format(context.getString(R.string.change_nickname_message2), MainActivity.nickname);
        } // end if there is already a nickname set.
        tv.setText(tempMessage);
        tv.setFocusable(true);
        ll.addView(tv, llParams);

        // Add an EditText view to get user input
        final EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_TEXT_FLAG_CAP_WORDS);
        input.setHint(context.getString(R.string.change_nickname_hint));
        // Add also an action listener:
        input.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                // Next two line are also at the done button pressing:
                String newNickname = input.getText().toString();
                changeNicknameFinishing(context, newNickname);
                alertToShow.dismiss();
            }
            return false;
        });
        // End add action listener for the IME done button of the keyboard..

        ll.addView(input, llParams);

        alert.setView(ll);

        // end if OK was pressed.
        alert.setPositiveButton(context.getString(R.string.msg_ok), (dialog, whichButton) -> {
            // Next two line are also at the done button:
            String newNickname = input.getText().toString();
            changeNicknameFinishing(context, newNickname);
        });

        alert.setNegativeButton(context.getString(R.string.msg_cancel), (dialog, whichButton) -> {
            // Cancelled, do nothing yet.
        });

        alertToShow = alert.create();
        alertToShow.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
        alertToShow.show();
        // end of alert dialog with edit sequence.
    } // end changeNickname() method.

    // A method to check and finish the nickname changing:
    public static void changeNicknameFinishing(final Context context, String newNickname) {
        // Check if the nickname is longer than two character:
        if (newNickname.length() < 3) {
            GUITools.alert(context, context.getString(R.string.error), context.getString(R.string.nickname_too_short), context.getString(R.string.msg_ok));
        } else {
            // A good nickname was written:
            GUITools.alert(context, context.getString(R.string.nickname_title), String.format(context.getString(R.string.nickname_success), newNickname), context.getString(R.string.msg_ok));

            // Change also it in the game:
            MainActivity.nickname = newNickname;
            // Save it into SharedPreferences:
            Settings set = new Settings(context);
            set.saveStringSettings("nickname", MainActivity.nickname);
            MainActivity.aPossession[1] = MainActivity.nickname;
        }
    } // end check and change the nickname method.

    // A method for about dialog for this package:
    @SuppressLint("InflateParams")
    public static void aboutDialog(Context parentContext) {
        final Context context = new ContextThemeWrapper(parentContext, R.style.MyAlertDialog);
        // Inflate the about message contents
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View messageView = inflater.inflate(R.layout.about_dialog, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setIcon(R.drawable.ic_launcher);
        builder.setTitle(R.string.about_title);
        builder.setView(messageView);
        builder.setPositiveButton(context.getString(R.string.msg_close), null);
        builder.create();
        builder.show();
    } // end about dialog.

    // A method to rate this application:
    public static void showRateDialog(final Context parentContext) {
        final Context context = new ContextThemeWrapper(parentContext, R.style.MyAlertDialog);
        AlertDialog.Builder builder = new AlertDialog.Builder(context).setIcon(R.drawable.ic_launcher).setTitle(context.getString(R.string.title_rate_app));

        ScrollView sv = new ScrollView(context);
        LinearLayout ll = new LinearLayout(context);
        ll.setOrientation(LinearLayout.VERTICAL);

        TextView tv = new TextView(context);
        tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainActivity.textSize);
        tv.setFocusable(true);
        tv.setText(context.getString(R.string.body_rate_app));
        ll.addView(tv);

        // Add now the LinearLayout into ScrollView:
        sv.addView(ll);

        builder.setView(sv);

        builder.setPositiveButton(context.getString(R.string.bt_rate), (dialog, which) -> {
            Settings set = new Settings(context);
            set.saveBooleanSettings("wasRated", true);
            String link = "market://details?id=";
            try {
                // play market available
                context.getPackageManager().getPackageInfo("com.android.vending", 0);
                // not available
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
                // Should use browser
                link = "https://play.google.com/store/apps/details?id=";
            }
            // Starts external action
            context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(link + context.getPackageName())));
        }).setNegativeButton(context.getString(R.string.bt_not_now), null);
        builder.show();
    } // end showRateDialog() method.

    // A method which checks if was rated:
    public static void checkIfRated(Context context) {
        Settings set = new Settings(context);
        boolean wasRated = set.getBooleanSettings("wasRated");
        if (!wasRated) {

            if (MainActivity.numberOfLaunches % 6 == 0 && MainActivity.numberOfLaunches > 0) {
                GUITools.showRateDialog(context);
            } // end if was x launches.
        } // end if it was not rated.
    } // end checkIfRated() method.

    // A method to go to main activity:
    public static void goToMainActivity(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        context.startActivity(intent);
    } // end go to main activity.

    // A method which detects if accessibility is enabled:
    public static boolean isAccessibilityEnabled(Context context) {
        AccessibilityManager am = (AccessibilityManager) context.getSystemService(Context.ACCESSIBILITY_SERVICE);
        // boolean isAccessibilityEnabled = am.isEnabled();
        return am.isTouchExplorationEnabled();
    } // end isAccessibilityEnabled() method.

} // end GUITools class.
