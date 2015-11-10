package com.burfdevelopment.robotheadandroid;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.StrictMode;
import android.speech.tts.TextToSpeech;

import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import java.io.DataInputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;
import java.util.Locale;

public class MainActivity extends Activity implements  TextToSpeech.OnInitListener{

    private TextToSpeech myTTS;
    private int TT_CHECK = 123;

    static final int PORT = 5678;
    static final int DELAY = 1000;
    ServerSocket cmdSock;
    Socket s;

    int data = 0;
    byte[] messageByte = new byte[1000];

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        goFullScreen();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // check tts
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
        startActivityForResult(checkTTSIntent, TT_CHECK);

    }

    void startListing()
    {
        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        new AsyncTask<Integer, Void, Void>() {
            @Override
            protected Void doInBackground(Integer... params) {
                try {

                    startServer();

                } catch (Exception e) {
                    e.printStackTrace();
                }
                return null;
            }
        }.execute(1);

    }


    //runs without a timer by reposting this handler at the end of the runnable
    Handler timerHandler = new Handler();
    Runnable timerRunnable = new Runnable() {

        @Override
        public void run() {

            Log.i("TAG", "FIRE ");

            try {

                String messageString = "";

                DataInputStream in = new DataInputStream(s.getInputStream());
                int bytesRead = 0;

                bytesRead = in.read(messageByte);

                if (bytesRead != -1)
                {
                    messageString += new String(messageByte, 0, bytesRead);
                    System.out.println("Leng " + bytesRead);

                    System.out.println("MESSAGE: " + messageString);

                    final String finalMessageString = messageString;
                    runOnUiThread(new Runnable() {
                        public void run() {
                            speakWords(finalMessageString);
                        }
                    });
                }
                else
                {
                    System.out.println("No Message");
                    s = cmdSock.accept();
                }

            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();

                System.out.println(e.getLocalizedMessage());

                try {
                    cmdSock = new ServerSocket(PORT);
                    s = cmdSock.accept();
                } catch (IOException e2) {
                    // TODO Auto-generated catch block
                    e2.printStackTrace();
                }

            }

            timerHandler.postDelayed(this, DELAY);
        }
    };

    private void startServer() {

        try {
            cmdSock = new ServerSocket(PORT);
            s = cmdSock.accept();

            timerHandler.postDelayed(timerRunnable, DELAY);

        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }


    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void speakWords(String speech) {
        //implement TTS here
        if (speech != null && speech.length() > 0) {

            HashMap<String, String> myHashAlarm = new HashMap<String, String>();
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_STREAM, String.valueOf(AudioManager.STREAM_ALARM));
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_VOLUME, "1");
            myHashAlarm.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, "SOME MESSAGE");

            AudioManager am = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            int amStreamMusicMaxVol = am.getStreamMaxVolume(am.STREAM_MUSIC);
            am.setStreamVolume(am.STREAM_MUSIC, amStreamMusicMaxVol, 0);

            myTTS.speak(speech, TextToSpeech.QUEUE_FLUSH, myHashAlarm); //QUEUE_ADD

        }
    }

    //act on result of TTS data check
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == TT_CHECK) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                myTTS = new TextToSpeech(this, this);

            } else {
                Intent installTTSIntent = new Intent();
                installTTSIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installTTSIntent);
            }
        }
    }


    @Override
    public void onInit(int status) {
        // TODO Auto-generated method stub
        if (status == TextToSpeech.SUCCESS) {
            myTTS.setLanguage(Locale.UK);
            myTTS.speak("Hello", TextToSpeech.QUEUE_FLUSH, null);
            speakWords("Loading");
            startListing();
        } else if (status == TextToSpeech.ERROR) {
            Toast.makeText(this, "Sorry! Text To Speech failed...", Toast.LENGTH_LONG).show();
        }
    }

    protected void goFullScreen() {
        // go full screen
        // remove title
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        int uiOptions = this.getWindow().getDecorView().getSystemUiVisibility();
        int newUiOptions = uiOptions;

        boolean isImmersiveModeEnabled =
                ((uiOptions | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY) == uiOptions);
        if (isImmersiveModeEnabled) {
            Log.i("TAG", "Turning immersive mode mode off. ");
        } else {
            Log.i("TAG", "Turning immersive mode mode on.");
        }

        // Navigation bar hiding:  Backwards compatible to ICS.
        if (Build.VERSION.SDK_INT >= 14) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        }

        // Status bar hiding: Backwards compatible to Jellybean
        if (Build.VERSION.SDK_INT >= 16) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_FULLSCREEN;
        }

        if (Build.VERSION.SDK_INT >= 18) {
            newUiOptions ^= View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY;
        }

        this.getWindow().getDecorView().setSystemUiVisibility(newUiOptions);
    }
}
