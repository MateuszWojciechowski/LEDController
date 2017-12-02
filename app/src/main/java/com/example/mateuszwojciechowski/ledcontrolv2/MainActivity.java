package com.example.mateuszwojciechowski.ledcontrolv2;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.support.annotation.IdRes;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.util.List;
import java.util.UUID;

import static android.R.attr.id;
import static android.icu.lang.UCharacter.GraphemeClusterBreak.L;

public class MainActivity extends AppCompatActivity {

    private Button connectionButton, pulseButton, executeButton;
    private EditText editText;
    private TextView connectionStateText;
    private NumberPicker numberPicker;
    private RadioGroup colorsRadioGroup, chooseDiode;
    private ProgressBar progressBar;
    private BluetoothManager mBluetoothManager;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothGatt mBluetoothGatt;
    private BluetoothDevice mBluetoothDevice;
    private BluetoothGattService mBluetoothGattService;
    private BluetoothGattCharacteristic mBluetoothGattCharacteristic;
    private Context mContext;
    private static final String DEVICE_ADDRESS = "88:4A:EA:8B:8B:CD";
    private static final UUID SERVICE_UUID = UUID.fromString("0000ffe0-0000-1000-8000-00805f9b34fb");
    private static final UUID CHARACTERISTIC_UUID = UUID.fromString("0000ffe1-0000-1000-8000-00805f9b34fb");
    private static final int REQUEST_ENABLE_BT = 1;
    private boolean CONNECTED = false;
    private class ProgressTask extends AsyncTask<Integer, Integer, Boolean> {

        @Override
        protected Boolean doInBackground(Integer... params) {
            int time = params[0];
            int interval = time/500;
            for (int i = 0; i <= interval; i++) {
                SystemClock.sleep(500);
                publishProgress(((i+1) * 500 * 100)/time);
            }
            return true;
        }

        @Override
        protected void onPreExecute() {
            progressBar.setVisibility(ProgressBar.VISIBLE);
        }

        @Override
        protected void onPostExecute(Boolean bool) {
            if (bool) {
                progressBar.setVisibility(ProgressBar.INVISIBLE);
                progressBar.setProgress(0);
            }
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            progressBar.setProgress(values[0]);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = this.getApplicationContext();

        mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        mBluetoothDevice = mBluetoothAdapter.getRemoteDevice(DEVICE_ADDRESS);


        //Inicjalizowanie elementów widoku
        connectionStateText = (TextView) findViewById(R.id.connectionState);

        connectionButton = (Button) findViewById(R.id.connectionButton);
        connectionButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (CONNECTED == false) {
                    if (mBluetoothGatt == null) {
                        mBluetoothGatt = mBluetoothDevice.connectGatt(mContext, false, gattCallback);
                    }
                } else {
                    if (mBluetoothGatt != null) {
                        mBluetoothGatt.disconnect();
                    }
                }
            }
        });

        chooseDiode = (RadioGroup) findViewById(R.id.chooseDiodeRadios);

        editText = (EditText) findViewById(R.id.editText);

        pulseButton = (Button) findViewById(R.id.pulseButton);
        pulseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                pulseEffect();
            }
        });

        colorsRadioGroup = (RadioGroup) findViewById(R.id.colorsGroup);

        numberPicker = (NumberPicker) findViewById(R.id.numberPicker);
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(30);
        numberPicker.setWrapSelectorWheel(false);

        progressBar = (ProgressBar) findViewById(R.id.progressBar);

        executeButton = (Button) findViewById(R.id.executeButton);
        executeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int time = fadeEffect();
                new ProgressTask().execute(time);
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //wymuszenie włączenia Bluetooth
        if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
            Intent enableBT = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBT, REQUEST_ENABLE_BT);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //jeśli użytkownik odmówi włączenia BT to aplikacja wyłącza się
        if (requestCode == REQUEST_ENABLE_BT) {
            if (resultCode == Activity.RESULT_CANCELED) {
                finish();
                return;
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    //BT callback
    private BluetoothGattCallback gattCallback = new BluetoothGattCallback() {
        //funkcja wywołuje się jak urządzenie się połączy/rozłączy
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
            switch (newState) {
                case BluetoothProfile.STATE_CONNECTED:
                    CONNECTED = true;
                    mBluetoothGatt.discoverServices();
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connectionButton.setText(getString(R.string.disconnect));
                            connectionStateText.setText(getResources().getString(R.string.connected));
                            connectionStateText.setTextColor(getResources().getColor(R.color.connected));
                            executeButton.setEnabled(true);
                        }
                    });
                    break;
                case BluetoothProfile.STATE_DISCONNECTED:
                    CONNECTED = false;
                    mBluetoothGatt.close();
                    mBluetoothGatt = null;
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            connectionButton.setText(getString(R.string.connect));
                            connectionStateText.setText(getResources().getString(R.string.disconnected));
                            connectionStateText.setTextColor(getResources().getColor(R.color.disconnected));
                            executeButton.setEnabled(false);
                        }
                    });
                    break;
                default:

            }
        }

        //wywołuje się gdy smartfon odkrywa usługi BT Gatt
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            mBluetoothGattService = gatt.getService(SERVICE_UUID);
            mBluetoothGattCharacteristic = mBluetoothGattService.getCharacteristic(CHARACTERISTIC_UUID);
        }

        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

        }

        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {

        }

        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {

        }
    };

    //wysyła komendę zmiany koloru na podstawie aktualnych ustawień
    private int fadeEffect() {
        String command = "";
        command += getDiodeFromRadio();
        command += getColorFromRadio(colorsRadioGroup);
        command += ":" + (numberPicker.getValue() * 1000) + ";";

        mBluetoothGattCharacteristic.setValue(command);
        mBluetoothGatt.writeCharacteristic(mBluetoothGattCharacteristic);

        return (numberPicker.getValue() * 1000);
    }

    //wysyła komendę pulsowania aktualnie wybranej diody
    private void pulseEffect() {
        String command = "";
        command += getDiodeFromRadio();
        command += ":pulse:";
        command += editText.getText() + ";";

        mBluetoothGattCharacteristic.setValue(command);
        mBluetoothGatt.writeCharacteristic(mBluetoothGattCharacteristic);
    }

    //zwraca kolor diody na podstawie wybranego przycisku
    private String getColorFromRadio(RadioGroup group) {
        RadioButton radio = (RadioButton) findViewById(group.getCheckedRadioButtonId());
        String color = radio.getText().toString();
        if (color == getResources().getString(R.string.red)) {
            return ":R";
        } else if (color == getResources().getString(R.string.green)) {
            return ":G";
        } else if (color == getResources().getString(R.string.blue)) {
            return ":B";
        } else if (color == getResources().getString(R.string.yellow)) {
            return ":Y";
        } else if (color == getResources().getString(R.string.orange)) {
            return ":O";
        } else if (color == getResources().getString(R.string.purple)) {
            return ":P";
        } else if (color == getResources().getString(R.string.ltblue)) {
            return ":L";
        } else if (color == getResources().getString(R.string.white)) {
            return ":W";
        } else if (color == getResources().getString(R.string.off)) {
            return ":OFF";
        } else {
            return ":ERROR";
        }
    }

    //zwraca numer diody na podstawie wybranego przycisku
    private int getDiodeFromRadio() {
        RadioButton radio = (RadioButton) findViewById(chooseDiode.getCheckedRadioButtonId());
        if (chooseDiode.getCheckedRadioButtonId() == -1)
            return -1;
        String id = radio.getText().toString();
        if (id.equals("1")){
            return 0;
        } else if (id.equals("2")) {
            return 1;
        } else if (id.equals("3")) {
            return 2;
        } else { return -1; }
    }
}