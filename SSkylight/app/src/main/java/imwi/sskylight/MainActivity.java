package imwi.sskylight;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;

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

    MqttAndroidClient client;

    static String URI = "tcp://m12.cloudmqtt.com:17387";
    static String USER = "wind2115";
    static String PASSWORD = "841566";

    Button btRoof;
    TextView tvMode;
    TextView tvState;
    Switch swMode;
    TextView tvConnection;
    Button btRefresh;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        InitWidget();
        InitMQTTConnection();
    }

    public void InitWidget()
    {
        btRoof = (Button) findViewById(R.id.btRoof);
        tvMode = (TextView) findViewById(R.id.tvMode);
        tvState = (TextView) findViewById(R.id.tvState);
        swMode = (Switch) findViewById(R.id.swMode);
        btRefresh = (Button) findViewById(R.id.btRefresh);
        tvConnection = (TextView) findViewById(R.id.tvConnection);
    }

    public void InitMQTTConnection()
    {
        String clientId = MqttClient.generateClientId();
        client = new MqttAndroidClient(this.getApplicationContext(), URI, clientId);

        MqttConnectOptions options = new MqttConnectOptions();
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1);
        options.setUserName(USER);
        options.setPassword(PASSWORD.toCharArray());


        try {
            IMqttToken token = client.connect(options);

            tvConnection.setText("Connecting ...");

            token.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    tvConnection.setText("Connection : Successful");
                    Subscribe("SensorValue");
                    Subscribe("SmartPhone");
                    Publish("State", "ControlValue");
                    Publish("Mode", "ControlValue");
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    tvConnection.setText("Connection : Failed");

                }
            });
        } catch (MqttException e) {

            tvConnection.setText("Error");
            e.printStackTrace();
        }

        client.setCallback(new MqttCallback() {
            @Override
            public void connectionLost(Throwable cause) {

            }

            @Override
            public void messageArrived(String topic, MqttMessage message) throws Exception {
                ProcessMQTTData(topic, message);
            }

            @Override
            public void deliveryComplete(IMqttDeliveryToken token) {

            }
        });
    }

    public void ProcessMQTTData(String topic, MqttMessage message)
    {
        String msg = message.toString();
        //tvConnection.setText(msg);

        if (msg.charAt(0) == 'S')
        {
            if (msg.charAt(2) == 'C')
            {
                tvState.setText("Giếng trời đang đóng");
                btRoof.setText("MỞ");
            }
            else
            {
                tvState.setText("Giếng trời đang mở");
                btRoof.setText("ĐÓNG");
            }

            return;
        }

        if (msg.charAt(0) == 'M')
        {
            if (msg.charAt(2) == 'M')
            {
                tvMode.setText("Chế độ thông minh đang đóng");
                swMode.setChecked(false);
            }
            else
            {
                tvMode.setText("Chế độ thông minh đang mở");
                swMode.setChecked(true);
            }

            return;
        }
    }

    public void TurnMode(View v)
    {
        if (swMode.isChecked() == true)
        {
            Publish("Smart", "ControlValue");
            tvMode.setText("Chế độ thông minh đang mở");
        }
        else
        {
            Publish("Manual", "ControlValue");
            tvMode.setText("Chế độ thông minh đang đóng");
        }
    }

    public void Refresh(View v)
    {
        Publish("State", "ControlValue");
        Publish("Mode", "ControlValue");
    }

    public void TurnRoof(View v)
    {
        if (swMode.isChecked() == true)
            return;

        if (btRoof.getText() == "MỞ")
        {
            Publish("OpenRoof", "ControlValue");
            btRoof.setText("ĐÓNG");
            tvState.setText("Giếng trời đang mở");
        }
        else
        {
            Publish("CloseRoof", "ControlValue");
            btRoof.setText("MỞ");
            tvState.setText("Giếng trời đang đóng");
        }
    }

    public void Subscribe(String topic)
    {
        int qos = 1;
        try {
            IMqttToken subToken = client.subscribe(topic, qos);
            subToken.setActionCallback(new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards

                }
            });
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void Publish(String msg, String topic)
    {
        byte[] encodedPayload = new byte[0];
        try {
            encodedPayload = msg.getBytes("UTF-8");
            MqttMessage message = new MqttMessage(encodedPayload);
            //message.setRetained(true);
            client.publish(topic, message);
        } catch (UnsupportedEncodingException | MqttException e) {
            e.printStackTrace();
        }
    }
}
