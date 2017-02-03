package student.dpl.drogobych.lviv.led;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.AlertDialog;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.math.BigInteger;
import java.util.UUID;


public class ledControl extends ActionBarActivity {
    static double PriceElectricity=-1;
    static double PaymentSum=-1;
    static double PowerHeating=-1;
    String stringMenu="Enable Saving Mode";
    int temperature = -1;
    static boolean stan=false;
    byte[] first = new byte[2];
    int min,sec;
    ImageView iv,st;
    Button btnOn, btnOff, btnDis,start,stop;
    SeekBar brightness,timer;
    TextView lumn,tv5,tc;
    String address = null;
    LinearLayout lay,lay2;
    private ProgressDialog progress;
    BluetoothAdapter myBluetooth = null;
    BluetoothSocket btSocket = null;
    private boolean isBtConnected = false;
    static CheckBox cb,cb1;
    TextView ttt;
    int a,p=0;
    int pos;
    //SPP UUID. Look for it
    static final UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
    Context context=this;
    //static Thread thread;
    Button set;
    CounterClass counterClass=null;
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        Intent newint = getIntent();
        address = newint.getStringExtra(Device_List.EXTRA_ADDRESS); //receive the address of the bluetooth device

        //view of the ledControl
        setContentView(R.layout.activity_led_control);
        //getSupportActionBar().hide();
        //call the widgtes
        ttt = (TextView) findViewById(R.id.textView7);
        tc=(TextView)findViewById(R.id.textView5);
        iv=(ImageView)findViewById(R.id.imageView1);
        st=(ImageView)findViewById(R.id.stan);
        timer = (SeekBar) findViewById(R.id.seekBar2);
        btnOn = (Button)findViewById(R.id.button2);
        start = (Button) findViewById(R.id.button6);
        stop = (Button) findViewById(R.id.button7);
        stop.setEnabled(false);
        btnOff = (Button)findViewById(R.id.button3);
        btnDis = (Button)findViewById(R.id.button4);
        brightness = (SeekBar)findViewById(R.id.seekBar);
        lumn = (TextView)findViewById(R.id.lumn);
        tv5 = (TextView)findViewById(R.id.textView5);
        lumn.setText("Temperature - 40");
        lay=(LinearLayout)findViewById(R.id.lay);
        brightness.setProgress(40);
        lay2=(LinearLayout)findViewById(R.id.lin2);
        cb = (CheckBox)findViewById(R.id.checkBox1);
        cb1 = (CheckBox)findViewById(R.id.checkBox2);
        set = (Button) findViewById(R.id.button5);
        iv.setImageResource(R.mipmap.timeroff);
        new ConnectBT().execute();
        start.setEnabled(false);
        final TextView priceElStr = (TextView)findViewById(R.id.FirstText);
        priceElStr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(context);

                alert.setTitle("Enter price");
                alert.setMessage("Enter the price of electricity:");

                // Set an EditText view to get user input
                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                alert.setView(input);
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        PriceElectricity=Double.parseDouble(value);
                        priceElStr.setBackgroundColor(Color.GREEN);
                        priceElStr.setText("Price electricity - "+String.valueOf(PriceElectricity));
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });
                alert.show();
            }
        });
        final TextView paymentStr = (TextView)findViewById(R.id.SecondText);
        paymentStr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(context);

                alert.setTitle("Enter sum");
                alert.setMessage("Enter the sum of payment:");

                // Set an EditText view to get user input
                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                alert.setView(input);
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        PaymentSum=Double.parseDouble(value);
                        paymentStr.setBackgroundColor(Color.GREEN);
                        paymentStr.setText("Payment sum - "+String.valueOf(PaymentSum));
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });
                alert.show();
            }
        });
        final Button btnGetRes = (Button)findViewById(R.id.getRes);
        btnGetRes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(PriceElectricity!=-1 && PaymentSum!=-1 && PowerHeating!=-1){
                    if(PowerHeating/60<PaymentSum/PriceElectricity){
                        double time=PaymentSum/PriceElectricity*60/PowerHeating;
                        timer.setProgress((int)(time-1));
                        start.setEnabled(true);
                        ttt.setText(String.valueOf(timer.getProgress()+1).concat(":00"));
                        min=timer.getProgress()+1;
                        sec=00;
                        try {
                            btSocket.getOutputStream().write(toBytes(1024+timer.getProgress()+1));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        msg("Incorect date.");
                    }
                } else {
                    msg("Enter all date.");
                }
            }
        });
        final TextView powerStr = (TextView)findViewById(R.id.ThirdText);
        powerStr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                AlertDialog.Builder alert = new AlertDialog.Builder(context);

                alert.setTitle("Enter power");
                alert.setMessage("Enter the power of heating:");

                // Set an EditText view to get user input
                final EditText input = new EditText(context);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                alert.setView(input);
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        PowerHeating=Double.parseDouble(value);
                        powerStr.setBackgroundColor(Color.GREEN);
                        powerStr.setText("Power heating - "+String.valueOf(PowerHeating));
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });
                alert.show();
            }
        });
         //Call the class to connect
        //commands to be sent to bluetooth
        btnOn.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                turnOnLed();      //method to turn on
            }
        });

        btnOff.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v)
            {
                turnOffLed();   //method to turn off
            }
        });

        btnDis.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Disconnect(); //close connection
            }
        });
        start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try
                {
                    counterClass=null;
                    counterClass = new CounterClass((timer.getProgress()+ 1) * 60000, 1000);
                    counterClass.start();
                    stop.setEnabled(true);
                    start.setEnabled(false);
                    set.setEnabled(false);
                    btSocket.getOutputStream().write(toBytes(1281));

                }
                catch (IOException e)
                {
                    msg("Error");
                }


            }
        });
        stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try
                {
                    ttt.setText("");
                    btSocket.getOutputStream().write(toBytes(1280));
                }
                catch (IOException e)
                {
                    msg("Error");
                }
                counterClass.cancel();
                counterClass=null;
                iv.setImageResource(R.mipmap.timeroff);
                p=0;
                stop.setEnabled(false);
                start.setEnabled(true);
                set.setEnabled(true);
                ttt.setText(String.valueOf(timer.getProgress() + 1).concat(":00"));
                min=timer.getProgress()+1;
                sec=00;
            }
        });
        set.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                start.setEnabled(true);
                ttt.setText(String.valueOf(timer.getProgress()+1).concat(":00"));
                min=timer.getProgress()+1;
                sec=00;
                try {
                    btSocket.getOutputStream().write(toBytes(1024+timer.getProgress()+1));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
        timer.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tv5.setText("Timer - " + String.valueOf(progress + 1)+" m.");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
        brightness.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progres, boolean fromUser) {
                double p=progres*70/80;
                int progress=(int)p;
                if (fromUser==true)
                {
                    lumn.setText("Temperature - "+String.valueOf(progres));
                    try
                    {
                        btSocket.getOutputStream().write(toBytes(768+80-progress));
                    }
                    catch (IOException e)
                    {
                        msg("Error");
                    }
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
    }

    private void Disconnect()
    {
        if (btSocket!=null) //If the btSocket is busy
        {
            try
            {
                btSocket.close(); //close connection
            }
            catch (IOException e)
            { msg("Error");}
        }
        finish(); //return to the first layout

    }

    private void turnOffLed()
    {
        if (btSocket!=null)
        {
            try
            {
                byte[] a= toBytes(256);
                btSocket.getOutputStream().flush();
                btSocket.getOutputStream().write(a);
            }
            catch (IOException e)
            {
                msg("Error");
            }
            st.setImageResource(R.mipmap.off);
        }
    }
    private void turnOnLed()
    {
        int k;
        st.setImageResource(R.mipmap.on);
        if (cb.isChecked()){
            k=768+80-brightness.getProgress();
        } else {
            k=768+80-brightness.getProgress();
        }

        byte[] a;

        if (btSocket!=null)
        {
            try
            {
                a=toBytes(k);
                btSocket.getOutputStream().write(toBytes(257));
                btSocket.getOutputStream().flush();
                btSocket.getOutputStream().write(a);
            }
            catch (IOException e)
            {
                msg("Error");
            }
        }
    }

    // fast way to call Toast
    private void msg(String s)
    {
        Toast.makeText(getApplicationContext(),s,Toast.LENGTH_LONG).show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(1,1,1,"Low");
        menu.add(2,2,2,"Normal");
        menu.add(3,3,3,"High");
        menu.add(4,4,4,stringMenu);
        // Inflate the menu; this adds items to the action bar if it is present.
        //getMenuInflater().inflate(R.menu.menu_led_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        //Toast.makeText(getApplicationContext(),String.valueOf(id),Toast.LENGTH_SHORT).show();
        boolean a = cb.isChecked();
        if(id==4){

            if(stringMenu.equals("Enable Saving Mode")) {
                //
                AlertDialog.Builder alert = new AlertDialog.Builder(this);

                alert.setTitle("Enter Temperature");
                alert.setMessage("Enter the highest temperature:");

                // Set an EditText view to get user input
                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_NUMBER);
                alert.setView(input);
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String value = input.getText().toString();
                        stringMenu = "Disable Saving Mode";
                        temperature=Integer.parseInt(value);
                        stan=true;
                        item.setTitle("Disable Saving Mode");
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });
                alert.show();
                //
                //

            } else {
                stringMenu = "Enable Saving Mode";
                item.setTitle("Enable Saving Mode");
                stan=false;
                temperature=-1;
                //

            }
            return super.onOptionsItemSelected(item);
        }
        if (!a) {
            cb.setChecked(true);

        }
        cb1.setChecked(false);
        ch();
            if (id==1){
                brightness.setProgress(20);
                lumn.setText("Temperature - " + String.valueOf(20));
                try
                {
                    btSocket.getOutputStream().write(toBytes(768 + 80 - 17));
                }
                catch (IOException e)
                {
                    msg("Error");
                }
            } else
            if(id==2){
                brightness.setProgress(40);
                lumn.setText("Temperature - " + String.valueOf(40));
                try
                {
                    btSocket.getOutputStream().write(toBytes(768 + 80 - 35));
                }
                catch (IOException e)
                {
                    msg("Error");
                }
            } else
            if(id==3){
                brightness.setProgress(80);
                lumn.setText("Temperature - " + String.valueOf(80));
                try
                {
                    btSocket.getOutputStream().write(toBytes(768 + 80 - 80));
                }
                catch (IOException e) {
                    msg("Error");
                }
            }
        //noinspection SimplifiableIfStatement
        /*if (id == R.id.action_settings) {
            return true;
        }*/

        return super.onOptionsItemSelected(item);
    }
    public void ch(){
        if (cb.isChecked()){
            lay.setVisibility(View.VISIBLE);
            //brightness.setProgress(pos);
        } else {
            lay.setVisibility(View.GONE);
            pos=brightness.getProgress();
            //brightness.setProgress(40);
        }
        if (cb1.isChecked()){
            lay2.setVisibility(View.VISIBLE);
            //brightness.setProgress(pos);
        } else {
            lay2.setVisibility(View.GONE);
            pos=brightness.getProgress();
            //brightness.setProgress(40);
        }
    }
    public void clicking(View view) {
        if (cb1.isChecked()){
            cb1.setChecked(false);
        }
        ch();
        lumn.setText(String.valueOf("Temperature - "+brightness.getProgress()));
        try
        {
            btSocket.getOutputStream().write(toBytes(768+80-brightness.getProgress()));
        }
        catch (IOException e)
        {

        }
    }

    public void clicking1(View view) {
        if (cb.isChecked()){
            cb.setChecked(false);
        }
        ch();
    }

    private class ConnectBT extends AsyncTask<Void, Void, Void>  // UI thread
    {
        private boolean ConnectSuccess = true; //if it's here, it's almost connected

        @Override
        protected void onPreExecute()
        {
            progress = ProgressDialog.show(ledControl.this, "Connecting...", "Please wait!!!");  //show a progress dialog
        }

        @Override
        protected Void doInBackground(Void... devices) //while the progress dialog is shown, the connection is done in background
        {
            try
            {
                if (btSocket == null || !isBtConnected)
                {
                    myBluetooth = BluetoothAdapter.getDefaultAdapter();//get the mobile bluetooth device
                    BluetoothDevice dispositivo = myBluetooth.getRemoteDevice(address);//connects to the device's address and checks if it's available
                    btSocket = dispositivo.createInsecureRfcommSocketToServiceRecord(myUUID);//create a RFCOMM (SPP) connection
                    BluetoothAdapter.getDefaultAdapter().cancelDiscovery();
                    btSocket.connect();//start connection
                }
            }
            catch (IOException e)
            {
                ConnectSuccess = false;//if the try failed, you can check the exception here
            }
            return null;
        }
        @Override
        protected void onPostExecute(Void result) //after the doInBackground, it checks if everything went fine
        {
            super.onPostExecute(result);

            if (!ConnectSuccess)
            {
                msg("Connection Failed. Is it a SPP Bluetooth? Try again.");
                finish();
            }
            else
            {

                //Toast.makeText(getApplicationContext(), Arrays.toString(first),Toast.LENGTH_SHORT).show();
                try {
                    btSocket.getInputStream().read(first);
                    //Toast.makeText(getApplicationContext(), Arrays.toString(first),Toast.LENGTH_SHORT).show();
                    if (first[0]==1 && first[1]==1){
                        st.setImageResource(R.mipmap.on);
                    } else {
                        st.setImageResource(R.mipmap.off);
                    }
                } catch (IOException e) {

                }

                msg("Connected.");
                isBtConnected = true;
            }
            progress.dismiss();
        }
    }
    static byte[] toBytes(int i)
    {
        byte[] result = new byte[2];

        result[0] = (byte) (i >> 8);
        result[1] = (byte) (i);
        byte k=result[0];
        result[0]=result[1];
        result[1]=k;


        return result;
    }
    static int toInt(byte[]result){

        byte s=result[0];
        result[0]=result[1];
        result[1]=s;
        return new BigInteger(result).intValue();
    }
    public class CounterClass extends CountDownTimer {

        public CounterClass(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @SuppressLint("NewApi")
        @TargetApi(Build.VERSION_CODES.GINGERBREAD)
        @Override
        public void onTick(long millisUntilFinished) {
            p++;
            sec--;
            if(sec<0){
                min--;
                sec=59;
            }
            if (sec<10) {
                ttt.setText(String.valueOf(min).concat(":0".concat(String.valueOf(sec))));
            } else {
                ttt.setText(String.valueOf(min).concat(":".concat(String.valueOf(sec))));
            }
            switch (p%6){
                case 1:{
                    iv.setImageResource(R.mipmap.timer);
                    break;
                }
                case 2:{
                    iv.setImageResource(R.mipmap.timer1);
                    break;
                }
                case 3:{
                    iv.setImageResource(R.mipmap.timer2);
                    break;
                }
                case 4:{
                    iv.setImageResource(R.mipmap.timer3);
                    break;
                }
                case 5:{
                    iv.setImageResource(R.mipmap.timer4);
                    break;
                }
                case 0:{
                    iv.setImageResource(R.mipmap.timer5);
                    break;
                }
            }
        }

        @Override
        public void onFinish() {
            iv.setImageResource(R.mipmap.timeroff);
            st.setImageResource(R.mipmap.off);
            ttt.setText("Time over");
            p=0;
            stop.setEnabled(false);
            set.setEnabled(true);
            start.setEnabled(false);
            /*AlertDialog.Builder ad=new AlertDialog.Builder(ledControl.this);
            ad.setTitle("Time over.");
            ad.setMessage("Lump is off.");
            ad.setIcon(R.mipmap.ic_launcher);
            ad.setNegativeButton("Ok", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            ad.show();*/
        }
    }
}
