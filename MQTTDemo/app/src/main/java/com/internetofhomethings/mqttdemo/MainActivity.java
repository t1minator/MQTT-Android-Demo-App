package com.internetofhomethings.mqttdemo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.internal.BottomNavigationMenuView;
import android.support.design.widget.BottomNavigationView;
import android.support.v7.app.AppCompatActivity;
import android.text.method.ScrollingMovementMethod;
import android.util.Base64;
import android.util.TypedValue;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ImageView;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import android.support.v7.app.ActionBar;

import org.eclipse.paho.android.service.MqttAndroidClient;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.io.UnsupportedEncodingException;


public class MainActivity extends AppCompatActivity {

    private MqttAndroidClient client;
    private String TAG = "MainActivity";
    private PahoMqttClient pahoMqttClient;
    private String clientid = "";
    private Timer myTimer;
    //Callback when bottom navigation item is selected
    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            TextView tvMessage  = (TextView) findViewById(R.id.subscribedMsg);
            EditText etSubTopic = (EditText) findViewById(R.id.subTopic);
            EditText etPubTopic = (EditText) findViewById(R.id.pubTopic);
            EditText etPubMsg   = (EditText) findViewById(R.id.pubMsg);
            EditText etBroker   = (EditText) findViewById(R.id.urlBroker);
            EditText etUName    = (EditText) findViewById(R.id.clientUn);
            EditText etPWord    = (EditText) findViewById(R.id.clientPw);
            String msg_new="";

            BottomNavigationView bottomNavigationView = (BottomNavigationView) findViewById(R.id.navigation);
            BottomNavigationViewHelper.disableShiftMode(bottomNavigationView);

            switch (item.getItemId()) {
                case R.id.navigation_connect:
                    //--- Set Connection Parameters ---
                    String urlBroker = etBroker.getText().toString().trim();
                    String username  = etUName.getText().toString().trim();
                    String password  = etPWord.getText().toString().trim();

                    Random r = new Random();        //Unique Client ID for connection
                    int i1 = r.nextInt(5000 - 1) + 1;
                    clientid = "mqtt" + i1;

                    if(pahoMqttClient.mqttAndroidClient.isConnected() ) {
                        //Disconnect and Reconnect to  Broker
                        try {
                            //Disconnect from Broker
                            pahoMqttClient.disconnect(client);
                            //Connect to Broker
                            client = pahoMqttClient.getMqttClient(getApplicationContext(), urlBroker, clientid, username, password);
                            //Set Mqtt Message Callback
                            mqttCallback();
                        }
                        catch (MqttException e) {
                        }
                    }
                    else {
                        //Connect to Broker
                        client = pahoMqttClient.getMqttClient(getApplicationContext(), urlBroker, clientid, username, password);
                        //Set Mqtt Message Callback
                        mqttCallback();
                    }
                    return true;
                case R.id.navigation_subscribe:
                    if(!pahoMqttClient.mqttAndroidClient.isConnected() ) {
                        msg_new = "Currently not connected to MQTT broker: Must be connected to subscribe to a topic\r\n";
                        tvMessage.append(msg_new);
                        return true;
                    }
                    String topic = etSubTopic.getText().toString().trim();
                    if (!topic.isEmpty()) {
                        try {
                            pahoMqttClient.subscribe(client, topic, 1);
                            msg_new = "Added subscription topic: " + etSubTopic.getText() + "\r\n";
                            tvMessage.append(msg_new);

                        } catch (MqttException e) {
                            e.printStackTrace();
                        }
                    }
                    return true;
                case R.id.navigation_publish:
                    //Check if connected to broker
                    if(!pahoMqttClient.mqttAndroidClient.isConnected() ) {
                        msg_new = "Currently not connected to MQTT broker: Must be connected to publish message to a topic\r\n";
                        tvMessage.append(msg_new);
                        return true;
                    }
                    //Publish non-blank message
                    String pubtopic = etPubTopic.getText().toString().trim();
                    String msg      = etPubMsg.getText().toString().trim();
                    if (!msg.isEmpty()) {
                        try {
                            pahoMqttClient.publishMessage(client, msg, 1, pubtopic);
                            msg_new = "Message sent to pub topic: " + etPubTopic.getText() + "\r\n";
                            tvMessage.append(msg_new);
                        } catch (MqttException e) {
                            e.printStackTrace();
                        } catch (UnsupportedEncodingException e) {
                            e.printStackTrace();
                        }
                    }
                    return true;
                case R.id.navigation_clear:
                    //Clear message field
                    tvMessage.setText("");
                    return true;
                case R.id.navigation_exit:
                    System.exit(0);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);                                                 // Main Activity layout file

        ActionBar actionBar = getSupportActionBar();                                            // Add Icon to title bar
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.mipmap.ic_app_launcher);

        TextView tvMessage = (TextView) findViewById(R.id.subscribedMsg);
        tvMessage.setMovementMethod(new ScrollingMovementMethod());                             // Scroller for feedback TextView object

        //Generate unique client id for MQTT broker connection
        Random r = new Random();
        int i1 = r.nextInt(5000 - 1) + 1;
        clientid = "mqtt" + i1;

        //Get Edit field values from layout GUI
        EditText etBroker   = (EditText) findViewById(R.id.urlBroker);
        EditText etUName    = (EditText) findViewById(R.id.clientUn);
        EditText etPWord    = (EditText) findViewById(R.id.clientPw);
        String urlBroker    = etBroker.getText().toString().trim();
        String username     = etUName.getText().toString().trim();
        String password     = etPWord.getText().toString().trim();

        pahoMqttClient = new PahoMqttClient();
        client = pahoMqttClient.getMqttClient(  getApplicationContext(),                        // Connect to MQTT Broker
                                                urlBroker,
                                                clientid,
                                                username,
                                                password
                                             );
        //Register Bottom Navigation Callback
        BottomNavigationView navigation = (BottomNavigationView) findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);      // Set nav menu "Select" callback
        BottomNavigationViewHelper.disableShiftMode(navigation);                                // Make all Text Visible

        //Create listener for MQTT messages.
        mqttCallback();

        //Create Timer to report MQTT connection status every 1 second
        myTimer = new Timer();
        myTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                ScheduleTasks();
            }

        }, 0, 1000);
    }
    private void ScheduleTasks()
    {
        //This method is called directly by the timer
        //and runs in the same thread as the timer.

        //We call the method that will work with the UI
        //through the runOnUiThread method.
        this.runOnUiThread(RunScheduledTasks);
    }


    private Runnable RunScheduledTasks = new Runnable() {
        public void run() {
            //This method runs in the same thread as the UI.

            //Check MQTT Connection Status
            TextView tvMessage  = (TextView) findViewById(R.id.cnxStatus);
            String msg_new="";

            if(pahoMqttClient.mqttAndroidClient.isConnected() ) {
                msg_new = "Connected\r\n";
                tvMessage.setTextColor(0xFF00FF00); //Green if connected
                tvMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            }
            else {
                msg_new = "Disconnected\r\n";
                tvMessage.setTextColor(0xFFFF0000); //Red if not connected
                tvMessage.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14);
            }
            tvMessage.setText(msg_new);
        }
    };


    // Called when a subscribed message is received
    protected void mqttCallback() {
        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {
                //msg("Connection lost...");
            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                TextView tvMessage = (TextView) findViewById(R.id.subscribedMsg);
                if(topic.equals("mycustomtopic1")) {
                    //Add custom message handling here (if topic = "mycustomtopic1")
                }
                else if(topic.equals("mycustomtopic2")) {
                    //Add custom message handling here (if topic = "mycustomtopic2")
                }
                else {
                    GsonBuilder builder = new GsonBuilder();
                    builder.setPrettyPrinting();
                    Gson gson = builder.create();
                    tvMessage.append( "received message\r\n");
                    ImageView iv = (ImageView)findViewById(R.id.imageView);
                    tvMessage.append( "attempt conversion \r\n");
                    CaptureMessage cm = new CaptureMessage();
                    try {
                        cm = gson.fromJson(message.toString(), CaptureMessage.class);
                    } catch (Error e){
                        tvMessage.append( e.getMessage() + " fail \r\n");
                    }

                    String ps2 = cm.getFields().getFrom().getEncode();
                    //String ps2 = "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";
                    //tvMessage.append( ps2 + "  \r\n");
                    byte[] data = Base64.decode(ps2, Base64.DEFAULT);
                    Bitmap bitmap = BitmapFactory.decodeByteArray(data, 0, data.length);
                    iv.setImageBitmap(bitmap);

                    String msg = "topic: " + topic + "\r\nMessage: " + "" + "\r\n" + cm.getVersion() + " \r\n " ;
                    tvMessage.append( msg);

                }
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }
}
