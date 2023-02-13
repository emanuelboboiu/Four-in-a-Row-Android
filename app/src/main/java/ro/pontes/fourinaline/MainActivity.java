package ro.pontes.fourinaline;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Point;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.TypedValue;
import android.view.ContextThemeWrapper;
import android.view.Display;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Chronometer;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.initialization.InitializationStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;

import java.lang.ref.WeakReference;
import java.util.Timer;
import java.util.TimerTask;

import ro.pontes.forinaline.R;


public class MainActivity extends Activity {

    // For music background:
    private SoundPlayer sndMusic;

    // Global fields:
    public static String nickname = null;
    public static int textSize = 20;
    public static int cfLevel = 5;
    public final static int CF_DEFAULT_LEVEL = 5;
    public static boolean isStarted = false;
    public static boolean isTV = false;
    public static boolean isSpeech = false;
    public static boolean isAccessibility = false;
    public static boolean isSound = true;
    public static boolean isMusic = true;
    public static int soundMusicPercentage = 75;
    public static boolean isWakeLock = false;
    public static boolean isOrientationBlocked = false;
    public static boolean isReversedColors = false;
    public static boolean isReversedColorsTemporary = false;
    public static int numberOfLaunches = 0;
    public static int lastTurnAtStart = 2; // user becomes first in first game.
    public static int myScore = 0;
    public static int partnersScore = 0;
    public static int SCORE_LIMIT = 6;
    public static boolean isEntireGameFinished = false;
    public static int gameType = 0; // 0 is against dealer, 1 is partner.
    private int pd = 0;
    private static byte isTurn = 1;
    private static boolean isAIMoving = false;
    private static int aiGlobalColumn = 0;
    private boolean isAbandon = false;
    private static int numberOfMoves = 1;
    private final Context finalContext = this;
    private static long elapsedTime = 0;

    // Creating object of AdView for AdMob:
    private AdView bannerAdView;

    // The TableLayout for grid, found in XML layout file:
    private TableLayout tlGrid;
    private final int rows = 6;
    private final int cols = 7;

    // Some global text views:
    private TextView tvCfStatus;

    // A chronometer:
    Chronometer chron = null;

    // Some global buttons:
    // Let's declare variables for all the buttons, we will use them to enable
    // or disable depending of the game status:
    private ImageButton btCfChangeGameType;
    private ImageButton btCfNew;
    private ImageButton btCfAbandon;

    // An array for images:
    private Bitmap[] resizedImages;

    // An array for Connect four Grid.
    private static byte[][] grid;
    // A static instance of Board class:
    private static Board board;
    // A static instance of ConnectFourAI class:
    private static ConnectFourAI ai;

    // An array for possession:
    public static String[] aPossession = new String[3];

    // Some global objects:
    private SpeakText speak;

    // For a timer:
    private Timer t;

    // Messages for handler to manage the interface:
    private final int MAKE_AI_MOVE_VIA_HANDLER = 0;
    private final int MAKE_CHANGE_CELL_VIA_HANDLER = 1;

    // A static inner class for handler:
    static class MyHandler extends Handler {
        WeakReference<MainActivity> cfActivity;

        MyHandler(MainActivity aCfActivity) {
            cfActivity = new WeakReference<>(aCfActivity);
        }
    } // end static class for handler.

    // This handler will receive a delayed message
    private final MyHandler mHandler = new MyHandler(this) {
        @Override
        public void handleMessage(Message msg) {
            // Do task here
            // MainActivity theActivity = cfActivity.get();

            // Do nothing yet.
            if (msg.what == MAKE_AI_MOVE_VIA_HANDLER) {
                makeAIMove();
            }
            if (msg.what == MAKE_CHANGE_CELL_VIA_HANDLER) {
                makeAIMove2(aiGlobalColumn);
            }

        }
    };

    // End handler stuff.

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Settings set = new Settings(this);
        set.chargeSettings();

        // Detect if is accessibility to set clickable all the squares:
        MainActivity.isAccessibility = GUITools.isAccessibilityEnabled(this);

        // Determine if it's a TV or not:
        if (GUITools.isAndroidTV(this)) {
            isTV = true;
        } // end determine if it is TV.

        if (isTV) {
            setContentView(R.layout.activity_main_tv);
        } else {
            // Set the orientation to be portrait if is blocked in settings:
            if (isOrientationBlocked) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } // end if isOrientationBlocked is true.
            setContentView(R.layout.activity_main);
        }

        // Get some global text views:
        tvCfStatus = findViewById(R.id.tvCfStatus);

        // Charge the chronometer if it isn't already charged:
        if (chron == null) {
            chron = findViewById(R.id.tvGameTimer);
        } // end if chronometer is null.

        /*
         * Get the ImageButtons into their global variables, we will use them to
         * enable or disable periodically:
         */
        btCfNew = findViewById(R.id.btCfNew);
        btCfAbandon = findViewById(R.id.cfAbandon);
        btCfChangeGameType = findViewById(R.id.cfChangeGameType);

        // Add listener for long click on start button:
        btCfNew.setOnLongClickListener(view -> {
            setGameType();
            return true;
        });
        // End add listener for long click on start button.

        // Fill the array for bitmaps:
        createResizedImages();

        // Fill the aPossession array:
        aPossession[0] = "";
        /*
         * Make the nickname for user if nickname exists, at index 0 in
         * possession array:
         */
        if (nickname != null && nickname.length() >= 3) {
            aPossession[1] = nickname;
        } else {
            // If no nickname was set, the nickname will be "you":
            aPossession[1] = getString(R.string.you);
            // Ask now for nickname, it would be a mandatory thing:
            GUITools.changeNickname(this);
        } // end if no nickname was set.
        /*
         * For the other player the value is set in enableOrDisableButtons(),
         * from dealer into partner and vice versa.
         */
        aPossession[2] = getString(R.string.partner);

        // Initialise the speak object:
        speak = new SpeakText(this);

        enableOrDisableButtons();
        updateTextViewsForTotals();

        // Get the TableLayout tlGrid:
        tlGrid = findViewById(R.id.tlGrid);

        // Make things if it's an old game:
        if (isStarted) {
            updateNumberOfMovesStatus();
        } // end if is not started yet.
        else {
            // Change the tvStatus to show welcome name:
            // only if nickname exists:
            if (nickname != null) {
                String welcomeMessage = String.format(getString(R.string.welcome_message), nickname);
                updateStatus(welcomeMessage);
            } // end if nickname exists.
        } // end if it is not started.

        // To keep screen awake:
        if (isWakeLock) {
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } // end wake lock.

        if (!isTV) {
            GUITools.checkIfRated(this);
            // Initializing the AdView object
            bannerAdView = findViewById(R.id.bannerAdView);
            adMobSequence();
        } // end if is not Android TV.

        // Calculate the padding in PD:
        int paddingPixel = 2;
        float density = getResources().getDisplayMetrics().density;
        pd = (int) (paddingPixel * density);
        // end calculate mPaddingDP
    } // end onCreate method.

    // A method to set the timer, in onResume() method:
    public void setTheTimer() {
        // Set the timer to send messages to the mHandler:
        t = new Timer();
        t.scheduleAtFixedRate(new TimerTask() {

            @Override
            public void run() {
                runOnUiThread(() -> {
                    // Send a message to the handler:
                    /*
                     * If turn is 2 in a game against dealer and is not AI
                     * moving right now:
                     */
                    if (gameType == 0 && isTurn == 2 && !isAIMoving) {
                        isAIMoving = true;
                        mHandler.sendEmptyMessageDelayed(MAKE_AI_MOVE_VIA_HANDLER, 500);
                    } // end event for dealer turn to make AI move.
                });
            }
        }, 500, 200); // 500 means start, and the 200 is do the
        // loop interval.
        // end set the timer.
    } // end setTheTimer method.

    @Override
    public void onResume() {
        super.onResume();

        setTheTimer();

        // Generate a new track:
        sndMusic = new SoundPlayer();
        sndMusic.playMusic(this);

        // Resume the AdView:
        // end if is not Android TV.

        // Start the chronometer if the game is started:
        if (isStarted) {
            chronStart();
            drawGrid();
        }

    } // end onResume method.

    @Override
    public void onPause() {
        // Add here what you want to happens on pause:

        // Stop the chronometer if the game is started:
        if (isStarted) {
            chronStop();
        }
        t.cancel();
        t = null;

        sndMusic.stopLooped();

        // Pause the AdView.
        // end if is not Android TV.

        super.onPause();
    } // end onPause method.

    @Override
    public void onDestroy() {
        // Destroy the AdView:
        // end if is not Android TV.

        super.onDestroy();
    } // end onDestroy() method.

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.mnuCfLevel) {
            chooseDifficultyLevel();
        } // end difficulty level in menu.
        else if (id == R.id.mnuNickname) {
            GUITools.changeNickname(this);
        } // end if change nickname was chosen.
        else if (id == R.id.mnuSettings) {
            goToSettingsActivity();
        } // end if settings was chosen in main menu.
        else if (id == R.id.mnuRate) {
            GUITools.showRateDialog(this);
        } // end if rate option was chosen in menu.
        else if (id == R.id.mnuAbout) {
            GUITools.aboutDialog(this);
        } // end if About was chosen in main menu.
        else if (id == R.id.mnuGameType) {
            setGameType();
        } // end if Game settings was chosen in main menu.

        return super.onOptionsItemSelected(item);
    }

    private void goToSettingsActivity() {
        // Called when the user clicks the settings option in menu:
        this.finish();
        Intent intent = new Intent(this, SettingsActivity.class);
        startActivity(intent);
    } // end goToSettingsActivity() method.

    private void chooseDifficultyLevel() {
        AlertDialog levelDialog;
        // Creating and Building the Dialog:
        final Context context = new ContextThemeWrapper(this, R.style.MyAlertDialog);
        AlertDialog.Builder builder = new AlertDialog.Builder(context);

        // Strings to Show In Dialog with Radio Buttons
        final CharSequence[] items = {getString(R.string.cf_easy), getString(R.string.cf_medium), getString(R.string.cf_hard), getString(R.string.cf_impossible)};
        final int[] cfLevels = {3, 5, 7, 8};

        builder.setTitle(getString(R.string.cf_choose_level));
        // Determine current item selected:
        int cfLevelPosition = -1;
        for (int i = 0; i < cfLevels.length; i++) {
            if (cfLevels[i] == cfLevel) {
                cfLevelPosition = i;
                break;
            }
        } // end for search current position of the current level chosen before.
        builder.setSingleChoiceItems(items, cfLevelPosition, (dialog, item) -> {

            switch (item) {
                case 0:
                case 1:
                case 2:
                case 3:
                    MainActivity.cfLevel = cfLevels[item];
                    break;
            } // end switch.
        });

        builder.setPositiveButton(getString(R.string.msg_ok), (dialog, whichButton) -> {
            Settings set = new Settings(context);
            set.saveIntSettings("cfLevel", cfLevel);
            ConnectFourAI.MAX_DEPTH = MainActivity.cfLevel;
            //
        });
        levelDialog = builder.create();
        levelDialog.show();
    } // end chooseDifficultyLevel() method.

    // The method to start a new game:
    public void newGridGame(View view) {
        newGridGameActions();
    }

    // A method for abandon or stop the game:
    public void abandonGame(View view) {
        // Only if it's the user's turn, isTurn==1:
        if (isTurn == 1) {
            // Make an alert with the question:
            // Get the strings to make an alert:
            String tempTitle = getString(R.string.title_abandon_game);
            String tempBody = getString(R.string.body_abandon_game);

            Context context = new ContextThemeWrapper(this, R.style.MyAlertDialog);

            AlertDialog.Builder alert = new AlertDialog.Builder(context);
            alert.setTitle(tempTitle);

            ScrollView sv = new ScrollView(context);
            LinearLayout ll = new LinearLayout(context);
            ll.setOrientation(LinearLayout.VERTICAL);

            TextView tv = new TextView(context);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_SP, MainActivity.textSize);
            tv.setText(tempBody);
            ll.addView(tv);

            // Add now the LinearLayout into ScrollView:
            sv.addView(ll);

            alert.setView(sv);

            alert.setPositiveButton(R.string.msg_yes, (dialog, whichButton) -> {
                // Things when yes was pressed:
                isStarted = false;
                isAbandon = true;
                checkForFinish();
            }).setNegativeButton(R.string.msg_no, null).show();
        } // end if it's user's turn:
        else {
            // If it's the partner's turn, abandon it's not possible:
            GUITools.alert(this, getString(R.string.warning), getString(R.string.abandon_at_user_turn_message), getString(R.string.msg_ok));
        } // end if it's not the user's turn.
    } // end abandonGame() method.

    // A method to change the game type friend or AI:
    public void changeGameType(View view) {
        // If gameType is 0 make it 1 and vice versa:
        if (gameType == 1) {
            gameType = 0;
            // Change the second value of the string aPossession into dealer:
            aPossession[2] = getString(R.string.dealer);
            btCfChangeGameType.setContentDescription(getString(R.string.cf_game_type1));
            btCfChangeGameType.setImageResource(R.drawable.button_partner);
            // Announce this change:
            updateStatus(getString(R.string.play_against_computer));
        } else {
            gameType = 1;
            aPossession[2] = getString(R.string.partner);
            btCfChangeGameType.setContentDescription(getString(R.string.cf_game_type0));
            btCfChangeGameType.setImageResource(R.drawable.button_computer);
            updateStatus(getString(R.string.play_against_partner));
        }
        SoundPlayer.playSimple(this, "reverse");
        updateTextViewsForTotals();
    } // end change game type method.

    private void createResizedImages() {
        // Get the size of the screen:
        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        int screenWidth = size.x;
        int screenHeight = size.y;
        /*
         * For portrait orientation the height of the grid will be 40% of
         * screenHeight, for landscape it will be 70% of the screen width. The
         * height of a TextView, a cell of the table will be 40% divided by
         * number of rows.
         */

        /*
         * If height is greater than width, it means it is portrait, otherwise
         * it is landscape:
         */
        boolean isPortrait = screenWidth <= screenHeight;

        // Determine the size of a cell in table depending of the orientation:
        // The dimensions for shapes:
        int ivHeight;
        if (isPortrait) {
            ivHeight = (screenHeight * 40 / 100) / rows;
        } else {
            // It means it is landscape:
            ivHeight = (screenHeight * 70 / 100) / rows;
        } // end if it is landscape.

        int ivWidth = ivHeight;

        // Instantiate the Bitmap array:
        resizedImages = new Bitmap[3];

        // Fill the bitmap array with resized images:
        for (int i = 0; i < 3; i++) {
            String shapeName = "cell" + i;
            int resId = getResources().getIdentifier(shapeName, "drawable", getPackageName());

            // Get the resized image:
            Bitmap bmp = BitmapFactory.decodeResource(getResources(), resId);
            Bitmap resizedbitmap = Bitmap.createScaledBitmap(bmp, ivWidth, ivHeight, true);
            resizedImages[i] = resizedbitmap;
        } // end for.

        /*
         * Reverse colour 1 with colour 2 if isReversedColors is true. It means
         * user plays with red:
         */
        if (isReversedColors) {
            Bitmap tempBitmap = resizedImages[1];
            resizedImages[1] = resizedImages[2];
            resizedImages[2] = tempBitmap;
        } // end if isReversedColors is true.
    } // end createResizedImages() method.

    public void newGridGameActions() {
        if (!isStarted) {
            isStarted = true;

            // Check if it was an entire game finish:
            if (isEntireGameFinished) {
                isEntireGameFinished = false;
                myScore = 0;
                partnersScore = 0;
                saveScore();
                updateTextViewsForTotals();
            } // end if it was a entire game finished before.

            // Stop previously speaking:
            speak.stop();
            SoundPlayer.playSimple(this, "new_event");
            enableOrDisableButtons();

            isAIMoving = false;
            // Change lastTurnAtStart:
            lastTurnAtStart = 3 - lastTurnAtStart;
            // Save it in SharedPreferences:
            Settings set = new Settings(this);
            set.saveIntSettings("lastTurnAtStart", lastTurnAtStart);
            isTurn = (byte) lastTurnAtStart;
            numberOfMoves = 1;
            isAbandon = false;

            // Show for fist time whose move is:
            updateNumberOfMovesStatus();

            // Start also the chronometer:
            chronReset();
            chronStart();

            // Initialise the grid array:
            grid = new byte[rows][cols];
            newGrid(); // a method to make 0 in each cell of the array.
            drawGrid(); // a method to draw the grid based on the grid array.

            /* Initialise the ConnectFourAI object created globally as static: */
            // Only if it's a game against dealer:
            if (gameType == 0) {
                board = new Board(rows, cols, 4);
                ai = new ConnectFourAI(board);
            }
        } // end if is not started.
    } // end new grid game actions.

    // A method to initialise the array for grid when a new game is started:
    private void newGrid() {
        for (int i = 0; i < rows; i++) {
            for (int j = 0; j < cols; j++) {
                grid[i][j] = 0;
            } // end for vertical movement in the grid array.
        } // end for horizontal movement in the grid array.
    } // end new grid method.

    // A method to draw the grid depending of the grid array:
    private void drawGrid() {
        // A StringBuilder for letters:
        StringBuilder sb = new StringBuilder("ABCDEFGHIJ");
        tlGrid.removeAllViews();

        for (int i = rows - 1; i >= 0; i--) {
            TableRow tr = new TableRow(this);
            for (int j = 0; j < cols; j++) {
                ImageView iv = new ImageView(this);
                iv.setImageBitmap(resizedImages[grid[i][j]]);

                final String cellName = "" + sb.charAt(j) + "" + (i + 1);
                // Determine if there is something there for contentDescription
                // from the grid and aPossession array:
                String tempMessage = cellName + " " + aPossession[grid[i][j]];
                iv.setContentDescription(tempMessage);
                // Set an ID for the text view:
                final int ivId = 1000 + ((10 * i) + j);
                iv.setId(ivId);

                // Make it focusable only if it is the upper row or accessibility:
                if (i == rows - 1 || isAccessibility) {
                    iv.setFocusable(true);
                    iv.setBackgroundColor(R.drawable.selector_background_selected);
                    // Something about focus for Android TV:
                    iv.setOnFocusChangeListener((view, hasFocus) -> {
                        // Find the current element by ID:
                        ImageView tempIv = findViewById(ivId);
                        if (hasFocus) {
                            tempIv.setAlpha(0.7F);
                        } else {
                            tempIv.setAlpha(1F);
                        }
                    });
                    // End focus changed.

                } else { // not first row and not accessibility:
                    iv.setFocusable(false);
                }
                iv.setClickable(true);
                // Make final x and y variables for coordinate as parameters for
                // changeCell() method:
                // final int x = i;
                final int y = j;
                // end onClick.
                iv.setOnClickListener(v -> {
                    if (isTurn == 1 || gameType == 1) {
                        // Determine the row, knowing the column Y:
                        int mRow = -1; // this will be column full.
                        for (int i1 = 0; i1 < rows; i1++) {
                            if (grid[i1][y] == 0) {
                                mRow = i1;
                                break;
                            }
                        } // end for to determine the aiRow.
                        /* If it is still -1, it means no column available: */
                        if (mRow == -1) {
                            SoundPlayer.playSimple(finalContext, "forbidden");
                            String columns = "ABCDEFGHIJ";
                            speak.say(String.format(getString(R.string.cf_column_is_full), columns.charAt(y)), true);
                        } // end if column is full.
                        else {
                            /*
                             * We calculate now the ID of the cell, because
                             * it is not necessarily that's clicked:
                             */
                            int cellId = 1000 + ((10 * mRow) + y);
                            changeCell(cellId, mRow, y);
                        } // end if column is not full.
                    } // end if gameType is 1 and isTurn is 1 too.
                });

                tr.addView(iv);
            } // end for horizontal movement in grid, from 1 to 7.
            tlGrid.addView(tr);
        } // end for vertical movement in grid, from 1 to 6.
    } // end draw grid.

    // A method to change a cell in grid depending of the game status:
    private void changeCell(int cellId, int x, int y) {
        if (isStarted) {
            ImageView iv = findViewById(cellId);
            String cellName = iv.getContentDescription().toString();

            // Check if there is still a 0 value, nothing yet:
            if (grid[x][y] == 0) {

                // Check if there is something below or x row is 0:
                if (x == 0 || grid[x - 1][y] > 0) {
                    SoundPlayer.playSimple(this, "cf_turn");

                    grid[x][y] = isTurn;
                    // Make string for content description and speak action,
                    // name put on current cell:
                    String tempMessage = cellName + " " + aPossession[isTurn];

                    iv.setContentDescription(tempMessage);
                    speak.say(tempMessage, true);

                    /*
                     * Fill the cell with corresponding shape - 1 or 2,
                     * depending of the who is turn now.
                     */

                    iv.setImageBitmap(resizedImages[isTurn]);

                    /*
                     * Show in another method with new thread the last movement,
                     * blinks:
                     */
                    showLastMovement(cellId, isTurn);

                    /*
                     * Give to AI the column accessed by player if it's his turn
                     * and it's a game against computer:
                     */
                    if (gameType == 0 && isTurn == 1) {
                        // Randomize a little the level:
                        int min = MainActivity.cfLevel - 1;
                        int max = (MainActivity.cfLevel < 8) ? (MainActivity.cfLevel + 1) : MainActivity.cfLevel;
                        ConnectFourAI.MAX_DEPTH = GUITools.random(min, max);
                        board.makeMovePlayer(y);
                    }
                    checkForFinish();
                    // Now, if is still started:
                    // Last things happens only if is not a finish:
                    if (isStarted) {
                        /*
                         * increase the number of moves to show them in status
                         * text view.
                         */
                        numberOfMoves = numberOfMoves + 1;
                        changeTurn();
                    }
                } else {
                    SoundPlayer.playSimple(this, "forbidden");
                    updateStatus(String.format(getString(R.string.cf_cell_forbidden), cellName));
                } // end if there is not something below or y == 0.
            } else {
                SoundPlayer.playSimple(this, "forbidden");
                speak.say(String.format(getString(R.string.cf_cell_already_used), cellName), true);
            } // end if there is already something there, the value in grid is
            // greater than 0, in fact 1 or 2.
        } else {
            // It is not started:
            SoundPlayer.playSimple(this, "forbidden");
        }
    } // end change cell method.

    private void showLastMovement(final int cellId, final int who) {
        int numberOfTimesToBlink = 10;
        long blinkInterval = 150;
        long blinkDuration = (numberOfTimesToBlink + 1) * blinkInterval;

        final ImageView blinkingImageView = findViewById(cellId);

        blinkingImageView.setImageBitmap(resizedImages[who]);
        blinkingImageView.setTag("yourFirstImage");

        final CountDownTimer blinkTimer = new CountDownTimer(blinkDuration, blinkInterval) {
            @Override
            public void onTick(long millisUntilFinished) {
                if (blinkingImageView.getTag() == "yourFirstImage") {
                    blinkingImageView.setImageBitmap(resizedImages[0]);
                    blinkingImageView.setTag("yourSecondImage");
                } else if (blinkingImageView.getTag() == "yourSecondImage") {
                    blinkingImageView.setImageBitmap(resizedImages[who]);
                    blinkingImageView.setTag("yourFirstImage");
                }
            }

            @Override
            public void onFinish() {
                /* Check if happened a wrong thing, an empty cell to be shown: */
                if (blinkingImageView.getTag() == "yourSecondImage") {
                    blinkingImageView.setImageBitmap(resizedImages[who]);
                    blinkingImageView.setTag("yourFirstImage");
                } // end if.
                // GUITools.beep();
            }
        };

        // Now start the think, the blinking:
        blinkTimer.start();
    } // end showLastMovement() method.

    // A method to make an AI move:
    private void makeAIMove() {
        if (isStarted && isTurn == 2) {
            new Thread(() -> {
                // Do task here in a new thread:
                // A global static int for aiColumn:
                aiGlobalColumn = ai.makeTurn();

                // Make now the move via handler:
                mHandler.sendEmptyMessageDelayed(MAKE_CHANGE_CELL_VIA_HANDLER, 2000);
            }).start();
            // end thread to do AI move.
        } // end if is started.
    } // end make AI move.

    /*
     * The method to make the AI turn via handler. This will happen after the
     * aiGlobalColumn is calculated by AI. The previous method calculates this
     * column into a new thread after it sends a message via handler for
     * computer to change also the cell. This method makes the rest of the task
     * after aiGlobalColumn calculation.
     */
    private void makeAIMove2(int aiColumn) {
        // Determine also the valid row:
        int aiRow = 0;
        for (int i = 0; i < rows; i++) {
            if (grid[i][aiColumn] == 0) {
                aiRow = i;
                break;
            }
        } // end for to determine the aiRow.

        /*
         * Determine also the cellID depending of the x and y coordinates:
         */
        int tvId = 1000 + ((10 * aiRow) + aiColumn);
        changeCell(tvId, aiRow, aiColumn);
    } // end makeAIMove2() method.

    // A method to change turn:
    private void changeTurn() {
        if (isStarted) {
            if (isTurn == 1) {
                isTurn = 2;
            } else {
                isTurn = 1;
                isAIMoving = false;
            }
            // Now update the number of moves status:
            updateNumberOfMovesStatus();
        } // end if is started.
    } // end change turn method.

    // A method to detect if is a win or a draw:
    private void checkForFinish() {
        // Determine if there is or not four in a row or a draw:
        int who = isFourInARow();
        // Check if an abandon:
        if (isAbandon) {
            who = 2;
        }
        /*
         * If who is greater than 0, because 0 means nothing, the game
         * continues:
         */
        if (who > 0) {
            String tempMessage;
            if (who == 2) {
                // Computer or partner is the final winner:
                if (gameType == 0) {
                    // Do nothing, no money to win.
                } else {
                    SoundPlayer.playSimple(this, "lose_game");
                } // to have the win game sound if it's 0 as bet.
                partnersScore = partnersScore + 1;
                tempMessage = getString(R.string.cf_lose);
                SoundPlayer.playSimple(this, "lose_game");
            } else if (who == 1) {
                // Player is the winner:
                if (gameType == 0) {
                    // Do nothing, no money to win
                } else {
                    SoundPlayer.playSimple(this, "win_game");
                } // end else if to have the win game sound if it's 0 as bet.
                myScore = myScore + 1;
                tempMessage = getString(R.string.cf_win);
                SoundPlayer.playSimple(this, "win_game");
            } else {
                // It's a draw:
                // Do nothing yet.
                tempMessage = getString(R.string.cf_draw);
            } // end the 3 possibilities for winner.
            saveScore();
            updateTextViewsForTotals();
            updateStatus(tempMessage);
            enableOrDisableButtons();
            chronStop();

            checkForEntireGameFinish();
        } // end is it was a finish, abandon, four in a row or a draw.
    } // end check for finish.

    // A method which checks if is entire game finished:
    private void checkForEntireGameFinish() {
        String tempMessage = "";
        if (myScore >= SCORE_LIMIT) {
            isEntireGameFinished = true;
            tempMessage = String.format(getString(R.string.user_won_entire_game), aPossession[1]);
            SoundPlayer.playSimpleDelayed(this, 1500, "win_entire_game");
        } // end if user won the entire game.
        else if (partnersScore >= SCORE_LIMIT) {
            isEntireGameFinished = true;
            tempMessage = String.format(getString(R.string.partner_won_entire_game), aPossession[2]);
            SoundPlayer.playSimpleDelayed(this, 1500, "lose_entire_game");
        } // end if partner won the entire game.

        // Now, if the match was finished:
        if (isEntireGameFinished) {
            updateStatus(tempMessage);
            myScore = 0;
            partnersScore = 0;
            saveScore();
        } // end if the entire match was finished.
    } // end checkForEntireGameFinish() method.

    // A method to check for four in a row:
    private int isFourInARow() {
        int who = 0;

        if (isStarted) {
            // First check for four in a row horizontally:
            outerloop:
            for (int i = 0; i < rows; i++) {
                int nr = 1; // this must become 4 if there are four in a row.
                for (int j = 1; j < cols; j++) {
                    if (grid[i][j] > 0 && grid[i][j] == grid[i][j - 1]) {
                        nr = nr + 1;
                    } else {
                        nr = 1; // reset the nrVariable back to 1 for other
                        // tries.
                    }
                    // Check if there are 4 in a row:
                    if (nr >= 4) {
                        who = grid[i][j]; // we detect who was with this
                        // connection done.
                        isStarted = false;
                        break outerloop;
                    } // end check if there are four, the nrVariable is 4.
                } // end going through columns, from left to right in a row.
            } // end for going through rows.
        } // end if isStarted.

        // If it is still started, check if there are four in a row vertically:
        if (isStarted) {

            outerloop:
            for (int j = 0; j < cols; j++) {
                int nr = 1;
                for (int i = 1; i < rows; i++) {
                    if (grid[i][j] > 0 && grid[i][j] == grid[i - 1][j]) {
                        nr = nr + 1;
                    } else {
                        nr = 1; // reset the nrVariable back to 1 for other
                        // tries.
                    }
                    // Check if there are 4 in a row:
                    if (nr >= 4) {
                        who = grid[i][j]; // we detect who was with this
                        // connection done.
                        isStarted = false;
                        break outerloop;
                    } // end check if there are four, the nrVariable is 4.
                } // end for rows, inner loop.
            } // end for columns, outer loop.
        } // end if it is still started to check for vertical rows.

        // If is still started for diagonal from left to right:
        if (isStarted) {
            outerloop:
            for (int i = 0; i < rows - 3; i++) {
                for (int j = 0; j < cols - 3; j++) {
                    if (grid[i][j] > 0 && grid[i][j] == grid[i + 1][j + 1] && grid[i][j] == grid[i + 2][j + 2] && grid[i][j] == grid[i + 3][j + 3]) {
                        who = grid[i][j]; // we detect who was with this
                        // connection done.
                        isStarted = false;
                        break outerloop;
                    }

                } // end going through columns, from left to right in a row.
            } // end for rows.
        } // end if game is started for checking the left to right diagonal.

        // If is still started for diagonal from right to left:
        if (isStarted) {
            outerloop:
            for (int i = 0; i < rows - 3; i++) {
                for (int j = 3; j < cols; j++) {
                    if (grid[i][j] > 0 && grid[i][j] == grid[i + 1][j - 1] && grid[i][j] == grid[i + 2][j - 2] && grid[i][j] == grid[i + 3][j - 3]) {
                        who = grid[i][j]; // we detect who was with four this
                        // connection done.
                        isStarted = false;
                        break outerloop;
                    }
                } // end going through columns, from left to right in a row.
            } // end for rows.
        } // end if game is still started for checking the right to left
        // diagonal.

        // If is still started but no win yet, after all four checks:
        // There where maximum number of moves and nobody won:
        if (isStarted) {
            if (numberOfMoves >= rows * cols) {
                who = 3; // a draw.
                isStarted = false;
            }
        } // end if it is still started to check for a draw.

        return who;
    } // end check for four in a row.

    // A method to update TextViews for score:
    public void updateTextViewsForTotals() {
        // Charge first the score:
        chargeScore();

        // Change the general score TextView:
        // Get the TextView:
        TextView tvGeneralScore = findViewById(R.id.tvGeneralScore);
        tvGeneralScore.setText(String.format(getString(R.string.general_score), aPossession[1], "" + myScore, aPossession[2], "" + partnersScore));
    } // end updateTextViews with totals in hands method.

    // A method to update the Status TextView:
    public void updateStatus(String text) {
        tvCfStatus.setText(text);
        speak.say(text, false);
    } // end updateBjStatus method.

    // A method to update the status with number of moves message:
    private void updateNumberOfMovesStatus() {
        String text = String.format(getString(R.string.cf_current_move), "" + numberOfMoves, aPossession[isTurn]);
        tvCfStatus.setText(text);
    } // end update status with number of moves message.

    // A method to make ImageButtons state active or inactive, depending of the
    // status of the game:
    public void enableOrDisableButtons() {
        // If is started or not:
        if (isStarted) {
            btCfNew.setEnabled(false);
            btCfAbandon.setEnabled(true);
            btCfChangeGameType.setEnabled(false);
        } else {
            btCfNew.setEnabled(true);
            btCfAbandon.setEnabled(false);
            btCfChangeGameType.setEnabled(true);
        } // end if is started or not.

        // Depending of the game type:
        if (gameType == 0) {
            // Change the second value of the string aPossession into dealer:
            aPossession[2] = getString(R.string.dealer);
            btCfChangeGameType.setContentDescription(getString(R.string.cf_game_type1));
            btCfChangeGameType.setImageResource(R.drawable.button_partner);
        } else {
            // Change the second value of the string aPossession into partner:
            aPossession[2] = getString(R.string.partner);
            btCfChangeGameType.setContentDescription(getString(R.string.cf_game_type0));
            btCfChangeGameType.setImageResource(R.drawable.button_computer);
        }
    } // end enableOrDisableButtons.

    // A method to set the game type, choose colour etc.::
    private void setGameType() {
        // This is only if the game is not started:
        final Context context = new ContextThemeWrapper(this, R.style.MyAlertDialog);

        if (!isStarted) {
            // We save current colours:
            isReversedColorsTemporary = isReversedColors;

            // A LayoutParams to add items as match_parent for width:
            LinearLayout.LayoutParams llParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            // We create first a layout for this action:
            LinearLayout addLLMain = new LinearLayout(context);
            addLLMain.setOrientation(LinearLayout.VERTICAL);
            addLLMain.setGravity(Gravity.CENTER_HORIZONTAL);
            addLLMain.setLayoutParams(llParams);

            TextView tvAddThis = new TextView(context);
            tvAddThis.setTextSize(TypedValue.COMPLEX_UNIT_SP, textSize);
            // Make the string for this text view:
            String strAddThis = getString(R.string.choose_color_in_game_type_alert);
            tvAddThis.setText(strAddThis);
            addLLMain.addView(tvAddThis, llParams);

            // Make two pieces to be chosen:
            // A horizontal layout for both of them:
            LinearLayout llImageViews = new LinearLayout(context);
            llImageViews.setOrientation(LinearLayout.HORIZONTAL);
            llImageViews.setLayoutParams(llParams);

            LinearLayout llLeft = new LinearLayout(context);
            llLeft.setOrientation(LinearLayout.VERTICAL);
            llLeft.setGravity(Gravity.CENTER_HORIZONTAL);

            // A LayoutParams to add items as wrap_content:
            LinearLayout.LayoutParams llParamsWrap = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);

            // First ImageView as Button:
            final ImageView iv = new ImageView(this);
            final ImageView iv2 = new ImageView(this);
            if (isReversedColors) {
                iv.setImageBitmap(resizedImages[2]);
            } else {
                iv.setImageBitmap(resizedImages[1]);
                iv.setEnabled(false);
            }
            iv.setContentDescription(getString(R.string.play_with_yellow));
            iv.setFocusable(true);
            iv.setClickable(true);
            iv.setOnClickListener(v -> {
                isReversedColorsTemporary = false;
                // Disable this image and activate the other one:
                // Make a red border around it:
                iv.setPadding(pd, pd, pd, pd);
                iv.setBackgroundColor(Color.parseColor("#aa0000"));
                iv.setEnabled(false);
                iv2.setEnabled(true);
                iv2.setBackgroundColor(Color.TRANSPARENT);
                iv2.setPadding(0, 0, 0, 0);
            });
            // Add now the button in :
            llLeft.addView(iv, llParamsWrap);

            LinearLayout llRight = new LinearLayout(context);
            llRight.setOrientation(LinearLayout.VERTICAL);
            llRight.setGravity(Gravity.CENTER_HORIZONTAL);

            /*
             * Second ImageView as Button. It was created above, when first
             * image was instantiated:
             */
            if (isReversedColors) {
                iv2.setImageBitmap(resizedImages[1]);
                iv2.setEnabled(false);
            } else {
                iv2.setImageBitmap(resizedImages[2]);
            }
            iv2.setContentDescription(getString(R.string.play_with_red));
            iv2.setFocusable(true);
            iv2.setClickable(true);
            iv2.setOnClickListener(v -> {
                isReversedColorsTemporary = true;
                // Disable this image and activate the other one:
                iv.setEnabled(true);
                iv.setBackgroundColor(Color.TRANSPARENT);
                iv.setPadding(0, 0, 0, 0);
                iv2.setEnabled(false);
                iv2.setPadding(pd, pd, pd, pd);
                iv2.setBackgroundColor(Color.parseColor("#aa0000"));
            });
            // Add now the button in llRight:
            llRight.addView(iv2, llParamsWrap);

            /*
             * Add the two left and right layouts into llImageViews. Both
             * LinearLayouts, left and right will have weight 1. For weight to
             * be 1, we have llParams2:
             */

            LinearLayout.LayoutParams llParams2 = new LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
            llParams2.weight = 1.0f;

            llImageViews.addView(llLeft, llParams2);
            llImageViews.addView(llRight, llParams2);

            // Add the horizontal LinearLayout into the main one:
            addLLMain.addView(llImageViews, llParams);

            // Create now the alert:
            AlertDialog.Builder alertDialog = new AlertDialog.Builder(context);
            alertDialog.setTitle(R.string.title_game_type_alert);
            alertDialog.setView(addLLMain);

            // The buttons can be add now and cancel!:
            // end if OK was pressed.
            alertDialog.setPositiveButton(R.string.msg_ok, (dialog, whichButton) -> {
                // Here code for OK:
                isReversedColors = isReversedColorsTemporary;
                createResizedImages();
                Settings set = new Settings(finalContext);
                set.saveBooleanSettings("isReversedColors", isReversedColors);
            });

            alertDialog.setNegativeButton(R.string.msg_cancel, (dialog, whichButton) -> {
                // Cancelled:
                // Do nothing.
            });

            alertDialog.create();
            alertDialog.show();
        } // end if the game isn't started.
        else {
            // The game is started, show a warning alert:
            GUITools.alert(this, getString(R.string.warning), getString(R.string.warning_no_available_if_started), getString(R.string.msg_ok));
        } // end if the game is already started.
    } // end setGameType() method.

    // Methods to be called from XML:
    public void chooseLevelFromXML(View view) {
        chooseDifficultyLevel();
    } // end chooseLevelFromXML() method.

    public void setGameTypeFromXML(View view) {
        setGameType();
    } // end setGameTypeFromXML() method.

    public void changeNicknameFromXML(View view) {
        GUITools.changeNickname(this);
    } // end changeNicknameFromXML() method.

    public void goToSettingsFromXML(View view) {
        goToSettingsActivity();
    } // end goToSettingsFromXML() method.

    public void aboutDialogFromXML(View view) {
        GUITools.aboutDialog(this);
    } // end aboutDialogFromXML) method.

    // Methods to start, reset and stop the Chronometer:
    private void chronStart() {
        chron.setBase(SystemClock.elapsedRealtime() - elapsedTime);
        chron.start();
    } // end chronStart() method.

    private void chronStop() {
        elapsedTime = SystemClock.elapsedRealtime() - chron.getBase();
        chron.stop();
    } // end chronStop() method.

    private void chronReset() {
        elapsedTime = 0;
    }

    // end chronometer methods.

    // A method to save current scores in shared settings:
    private void saveScore() {
        Settings set = new Settings(this);
        set.saveIntSettings("myScore", myScore);
        set.saveIntSettings("partnersScore", partnersScore);
    } // end saveScore() method.

    // A method to charge score from shared settings:
    private void chargeScore() {
        Settings set = new Settings(this);
        myScore = set.getIntSettings("myScore");
        partnersScore = set.getIntSettings("partnersScore");
    } // end chargeScore() method.

    // The method to generate the AdMob sequence:
    private void adMobSequence() {
        //initializing the Google Admob SDK
        MobileAds.initialize(this, new OnInitializationCompleteListener() {
            @Override
            public void onInitializationComplete(InitializationStatus initializationStatus) {
                // Now, because it is initialized, we load the ad:
                loadBannerAd();
            }
        });
    } // end adMobSequence().

    // Now we will create a simple method to load the Banner Ad in the correct place:
    private void loadBannerAd() {
        // Creating  a Ad Request
        AdRequest adRequest = new AdRequest.Builder().build();
        // load Ad with the Request
        bannerAdView.loadAd(adRequest);
    } // end loadBannerAd() method.

} // end MainActivity class.
