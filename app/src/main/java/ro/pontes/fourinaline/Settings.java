package ro.pontes.fourinaline;

import android.content.Context;
import android.content.SharedPreferences;

public class Settings {

    // The file name for save and load preferences:
    private final static String PREFS_NAME = "connectFourSettings";

    private final Context context;

    public Settings(Context context) {
        this.context = context;
    }

    // A method to detect if a preference exist or not:
    public boolean isPreference(String key) {
        boolean value;
        // Restore preferences
        SharedPreferences settings = context
                .getSharedPreferences(PREFS_NAME, 0);
        value = settings.contains(key);
        return value;
    } // end detect if a preference exists or not.

    // Methods for save and read preferences with SharedPreferences:

    // Save a boolean value:
    public void saveBooleanSettings(String key, boolean value) {
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = context
                .getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean(key, value);
        // Commit the edits!
        editor.commit();
    } // end save boolean.

    // Read boolean preference:
    public boolean getBooleanSettings(String key) {
        boolean value;
        // Restore preferences
        SharedPreferences settings = context
                .getSharedPreferences(PREFS_NAME, 0);
        value = settings.getBoolean(key, false);

        return value;
    } // end get boolean preference from SharedPreference.

    // Save a integer value:
    public void saveIntSettings(String key, int value) {
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = context
                .getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(key, value);
        // Commit the edits!
        editor.commit();
    } // end save integer.

    // Read integer preference:
    public int getIntSettings(String key) {
        int value;
        // Restore preferences
        SharedPreferences settings = context
                .getSharedPreferences(PREFS_NAME, 0);
        value = settings.getInt(key, 0);

        return value;
    } // end get integer preference from SharedPreference.

    // For float values in shared preferences:
    public void saveFloatSettings(String key, float value) {
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = context
                .getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat(key, value);
        // Commit the edits!
        editor.commit();
    } // end save integer.

    // Read integer preference:
    public float getFloatSettings(String key) {
        float value;
        // Restore preferences
        SharedPreferences settings = context
                .getSharedPreferences(PREFS_NAME, 0);
        value = settings.getFloat(key, 2.2F); // a default value like the value
        // for moderate magnitude.

        return value;
    } // end get float preference from SharedPreference.

    // Save a String value:
    public void saveStringSettings(String key, String value) {
        // We need an Editor object to make preference changes.
        // All objects are from android.context.Context
        SharedPreferences settings = context
                .getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putString(key, value);
        // Commit the edits!
        editor.commit();
    } // end save String.

    // Read String preference:
    public String getStringSettings(String key) {
        String value;
        // Restore preferences
        SharedPreferences settings = context
                .getSharedPreferences(PREFS_NAME, 0);
        value = settings.getString(key, null);

        return value;
    } // end get String preference from SharedPreference.
    // End read and write settings in SharedPreferences.

    // A method to detect if a preference exist or not:
    public boolean preferenceExists(String key) {
        // Restore preferences
        SharedPreferences settings = context
                .getSharedPreferences(PREFS_NAME, 0);
        return settings.contains(key);
    } // end detect if a preference exists or not.

    // Charge Settings function:
    public void chargeSettings() {

        // Determine if is first launch of the program:
        boolean isNotFirstRunning = getBooleanSettings("isFirstRunning");

        if (!isNotFirstRunning) {
            saveBooleanSettings("isFirstRunning", true);
            // Make default values in SharedPrefferences:
            setDefaultSettings();
        }

        // Now charge settings:

        // The nickname:
        MainActivity.nickname = getStringSettings("nickname");

        // For level in ConnectFour game:
        MainActivity.cfLevel = getIntSettings("cfLevel");
        if (MainActivity.cfLevel < 2) {
            MainActivity.cfLevel = MainActivity.CF_DEFAULT_LEVEL;
        }
        ConnectFourAI.MAX_DEPTH = MainActivity.cfLevel;

        // Play or not the sounds and speech:
        MainActivity.isSpeech = getBooleanSettings("isSpeech");
        MainActivity.isSound = getBooleanSettings("isSound");
        MainActivity.isMusic = getBooleanSettings("isMusic");

        // For lastTurnAtStart:
        if (preferenceExists("lastTurnAtStart")) {
            MainActivity.lastTurnAtStart = getIntSettings("lastTurnAtStart");
        } // end if lastTurnAtStart preference exists.

        // For colours, yellow or red for user:
        MainActivity.isReversedColors = getBooleanSettings("isReversedColors");

        // For blocking the device in portrait orientation:
        if (preferenceExists("isOrientationBlocked")) {
            MainActivity.isOrientationBlocked = getBooleanSettings("isOrientationBlocked");
        } // end if isOrientationBlocked preference exists.

        // Wake lock, keep screen awake:
        MainActivity.isWakeLock = getBooleanSettings("isWakeLock");

        /* About number of launches, useful for information, rate and others: */
        // Get current number of launches:
        MainActivity.numberOfLaunches = getIntSettings("numberOfLaunches");
        // Increase it by one:
        MainActivity.numberOfLaunches++;
        // Save the new number of launches:
        saveIntSettings("numberOfLaunches", MainActivity.numberOfLaunches);
    } // end charge settings.

    public void setDefaultSettings() {
        // The nickname:
        MainActivity.nickname = null;
        saveStringSettings("nickname", null);

        // The level for connect four game:
        saveIntSettings("cfLevel", MainActivity.CF_DEFAULT_LEVEL);
        MainActivity.cfLevel = MainActivity.CF_DEFAULT_LEVEL;
        ConnectFourAI.MAX_DEPTH = MainActivity.cfLevel;

        // Save the lastTurnAtStart to 2, user to become first:
        MainActivity.lastTurnAtStart = 2;
        saveIntSettings("lastTurnAtStart", MainActivity.lastTurnAtStart);

        // // Activate speech if accessibility, explore by touch is enabled:
        MainActivity.isSpeech = GUITools.isAccessibilityEnabled(context);
        saveBooleanSettings("isSpeech", MainActivity.isSpeech);

        MainActivity.isSound = true;
        saveBooleanSettings("isSound", MainActivity.isSound);
        MainActivity.isMusic = true;
        saveBooleanSettings("isMusic", MainActivity.isMusic);

        // Make yellow user's colour:
        MainActivity.isReversedColors = false;
        saveBooleanSettings("isReversedColors", MainActivity.isReversedColors);

        // Activate orientation not to be portrait if is not TV:
        MainActivity.isOrientationBlocked = false;
        saveBooleanSettings("isOrientationBlocked",
                MainActivity.isOrientationBlocked);

        // For keeping screen awake:
        MainActivity.isWakeLock = false;
        saveBooleanSettings("isWakeLock", MainActivity.isWakeLock);

        // Reset also some static variable:
        MainActivity.isStarted = false;
        MainActivity.myScore = 0;
        saveIntSettings("myScore", MainActivity.myScore);
        MainActivity.partnersScore = 0;
        saveIntSettings("partnersScore", MainActivity.partnersScore);

        MainActivity.gameType = 0;

    } // end setDefaultSettings function.

} // end Settings Class.
