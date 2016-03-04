package org.mems;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import org.mems.BLEService.BLEDeviceContext;
import org.mems.ColorChooser.ImageRGBDelegate;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;

public class LEDSettingsActivity extends Activity implements ImageRGBDelegate, SeekBar.OnSeekBarChangeListener, ColorPickerView.OnColorChangedListener {

    private static final String LOG_TAG = LEDSettingsActivity.class.getSimpleName();

    private BLEController mController = null;
    private String deviceAddress;
    private BLEDeviceContext device;

    private EditText ledNameField;
    private ColorPickerView colorChooser;

//    private SeekBar brightBar;
    private Button setTimeOnBtn;
    private Button setTimeOffBtn;
    private ImageButton switchScheduleOn;
    private ImageButton switchScheduleOff;
    private Button mSaveBtn;

    ScrollView mFirstPage;
    GridView mSecondPage;
    private LinearLayout mThirdPage;
    private Button Button2, Button1, Button3;
    private Button ontimesavebtn;
    private View mTimeScheduleOn_1, mTimeScheduleOn_3;
    private View mTimeScheduleOff_1, mTimeScheduleOff_3;
    private boolean mInitScheduleOn = false;
    private boolean mInitScheduleOff = false;
    private int mInitScheduleOn_hour = 0;
    private int mInitScheduleOn_minute = 0;
    private int mInitScheduleOn_speed = 0;
    private int mInitScheduleOff_hour = 0;
    private int mInitScheduleOff_minute = 0;
    private int mInitScheduleOff_speed = 0;
    private Button goingOnBtn[] = new Button[3];//, goingOnBtn2, goingOnBtn3;
    private Button goingOffBtn[] = new Button[3];//1, goingOffBtn2, goingOffBtn3;

	private int mBrightProgress = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setContentView(R.layout.activity_settings);

        mController = BLEController.getInstance(this);
        Intent intent = getIntent();
        deviceAddress = intent.getExtras().getString("device");

        ledNameField = (EditText) findViewById(R.id.led_name);
        colorChooser = (ColorPickerView) findViewById(R.id.color_chooser);
        colorChooser.setOnColorChangedListenner(this);

//        brightBar = (SeekBar) findViewById(R.id.bright_bar);
//        mBrightProgress = 80;
//        brightBar.setProgress(mBrightProgress);
//        brightBar.setOnSeekBarChangeListener(this);

        switchScheduleOn = (ImageButton) findViewById(R.id.switch_schedule_on);
        switchScheduleOff = (ImageButton) findViewById(R.id.switch_schedule_off);

        //immediately
        goingOnBtn[0] = (Button) findViewById(R.id.goingon_btn1);
        goingOnBtn[1] = (Button) findViewById(R.id.goingon_btn2);
        goingOnBtn[2] = (Button) findViewById(R.id.goingon_btn3);

        goingOffBtn[0] = (Button) findViewById(R.id.goingoff_btn1);
        goingOffBtn[1] = (Button) findViewById(R.id.goingoff_btn2);
        goingOffBtn[2] = (Button) findViewById(R.id.goingoff_btn3);

        setTimeOnBtn = (Button) findViewById(R.id.set_time_on);
        setTimeOffBtn = (Button) findViewById(R.id.set_time_off);
        ontimesavebtn = (Button) findViewById(R.id.ontimesavebtn);

        device = mController.getDevice(deviceAddress);
        final TextView lightname = (TextView)findViewById(R.id.light_name);
        lightname.setText(device.name);
        ledNameField.setText(device.name);

//        set_time_off.r
        mSaveBtn = (Button)findViewById(R.id.btn_save_name);
        mSaveBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mSaveBtn.getText().equals(getString(R.string.btn_change_name))) {
                    lightname.setVisibility(View.GONE);
                    ledNameField.setVisibility(View.VISIBLE);

                    mSaveBtn.setText(R.string.btn_save_name);
                }
                else {
                    onChangeName(view);
                    ledNameField.setVisibility(View.GONE);
                    lightname.setText(ledNameField.getText());
                    lightname.setVisibility(View.VISIBLE);
                    mSaveBtn.setText(R.string.btn_change_name);
                }
            }
        });

        mFirstPage = ((ScrollView)findViewById(R.id.first_page));
        mSecondPage = (GridView) findViewById(R.id.second_page);
        mThirdPage = (LinearLayout) findViewById(R.id.third_page);
        mFirstPage.scrollTo(0, 0);

        Button1 = (Button)findViewById(R.id.Button1);
        Button1.setSelected(true);
        Button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFirstPage.setVisibility(View.VISIBLE);
                mSecondPage.setVisibility(View.GONE);
                mThirdPage.setVisibility(View.GONE);
                Button3.setSelected(false);
                Button2.setSelected(false);
                Button1.setSelected(true);
            }
        });

        Button2 = (Button)findViewById(R.id.Button2);
        Button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFirstPage.setVisibility(View.GONE);
                mSecondPage.setVisibility(View.VISIBLE);
                mThirdPage.setVisibility(View.GONE);
                Button3.setSelected(false);
                Button2.setSelected(true);
                Button1.setSelected(false);
            }
        });

        Button3 = (Button)findViewById(R.id.Button3);
        Button3.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mFirstPage.setVisibility(View.GONE);
                mSecondPage.setVisibility(View.GONE);
                mThirdPage.setVisibility(View.VISIBLE);
                Button3.setSelected(true);
                Button2.setSelected(false);
                Button1.setSelected(false);
            }
        });

        mSecondPage.setAdapter(new GridAdapter(this, initLightSet()));
        mSecondPage.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long l) {
                device.changeScene(position);
            }
        });
        Log.d(LOG_TAG, "Settings::onCreate");

        mTimeScheduleOn_1 = findViewById(R.id.timeschedule_on_1);
        mTimeScheduleOn_3 = findViewById(R.id.timeschedule_on_3);
        mTimeScheduleOff_1 = findViewById(R.id.timeschedule_off_1);
        mTimeScheduleOff_3 = findViewById(R.id.timeschedule_off_3);
    }

    @Override
    public void imageColor(int color) {
        if (device != null) {
            device.changeColor(color);
        }
    }

    @Override
    public void onColorChanged(int color, int originalColor, float saturation) {
        if (device != null) {
            device.changeColor(originalColor);
        }
    }

    @Override
    public void onSingleColorChanged(int originalColor) {
        if (device != null) {
            device.changeColor(originalColor);
        }
    }

    @Override
    public void onBrightnessChanged(int color, float saturation) {
        if (device != null) {
            device.changeBright(color, (int) saturation);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences p = getSharedPreferences("timer", Context.MODE_PRIVATE);
        mInitScheduleOn_speed = p.getInt(SharedPrefManager.PRE_KEY_timeOn_SPEED + deviceAddress, 0);
        mInitScheduleOff_speed = p.getInt(SharedPrefManager.PRE_KEY_timeOff_SPEED + deviceAddress, 0);
        mInitScheduleOn = p.getBoolean(SharedPrefManager.PRE_KEY_timeOnStatus + deviceAddress, false);
        mInitScheduleOff = p.getBoolean(SharedPrefManager.PRE_KEY_timeOffStatus + deviceAddress, false);

        switchScheduleOn.setActivated(mInitScheduleOn);
        switchScheduleOff.setActivated(mInitScheduleOff);

        updateGoingBtn(true, mInitScheduleOn_speed);
        updateGoingBtn(false, mInitScheduleOff_speed);

        if (switchScheduleOn.isActivated()) {
            mTimeScheduleOn_1.setVisibility(View.VISIBLE);
            mTimeScheduleOn_3.setVisibility(View.VISIBLE);
        }
        else {
            mTimeScheduleOn_1.setVisibility(View.GONE);
            mTimeScheduleOn_3.setVisibility(View.GONE);
        }

        mInitScheduleOn_hour = p.getInt(SharedPrefManager.PRE_KEY_timeOnHour + deviceAddress, 0);
        mInitScheduleOn_minute = p.getInt(SharedPrefManager.PRE_KEY_timeOnMinute + deviceAddress, 0);
        setTimeOnBtn.setText(mInitScheduleOn_hour + ":" + mInitScheduleOn_minute);

        if (switchScheduleOff.isActivated()) {
            mTimeScheduleOff_1.setVisibility(View.VISIBLE);
            mTimeScheduleOff_3.setVisibility(View.VISIBLE);
        }
        else {
            mTimeScheduleOff_1.setVisibility(View.GONE);
            mTimeScheduleOff_3.setVisibility(View.GONE);
        }

        mInitScheduleOff_hour = p.getInt(SharedPrefManager.PRE_KEY_timeOffHour + deviceAddress, 0);
        mInitScheduleOff_minute = p.getInt(SharedPrefManager.PRE_KEY_timeOffMinute + deviceAddress, 0);
        setTimeOffBtn.setText(mInitScheduleOff_hour + ":" + mInitScheduleOff_minute);

        Log.d(LOG_TAG, "Settings::onStart");
    }

    private void updateGoingBtn(boolean isOn, int speed) {
        Button goingBtn[] = isOn ? goingOnBtn : goingOffBtn;

        for (int i = 0; i < goingBtn.length; i++) {
            if (i == speed) {
                goingBtn[i].setActivated(true);
            }
            else {
                goingBtn[i].setActivated(false);
            }
        }//
    }

    public void onChangeName(View view) {
        String name = ledNameField.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "名字不能为空", Toast.LENGTH_LONG).show();
        } else {
            mController.changeDeviceName(device, name);
            Toast.makeText(this, "保存成功", Toast.LENGTH_LONG).show();
        }
    }

    
    /**
     * event for xml.
     */
    public void onBackClick(View view) {
    	finish();
    }
    public void onRemoveLED(View view) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(LEDSettingsActivity.this)
        		.setTitle("提醒")
                .setMessage("是否移除设备")
                .setNegativeButton("取消", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        //
                    }
                })
                .setPositiveButton("移除", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialoginterface, int which) {
                        mController.removeDevice(deviceAddress);
                        finish();
                    }
                });
        dialogBuilder.show();
    }

    public void switchScheduleOnClick(View view) {
        ImageButton btn = (ImageButton) view;
        if (!btn.isActivated()) {
//            setTimeOnBtn.setVisibility(View.VISIBLE);
            mTimeScheduleOn_1.setVisibility(View.VISIBLE);
//            mTimeScheduleOn_2.setVisibility(View.VISIBLE);
            mTimeScheduleOn_3.setVisibility(View.VISIBLE);

            //at the same time auto close off time.
            mTimeScheduleOff_1.setVisibility(View.GONE);
//            mTimeScheduleOff_2.setVisibility(View.GONE);
            mTimeScheduleOff_3.setVisibility(View.GONE);

            //auto close off part.
            switchScheduleOff.setActivated(false);

            mInitScheduleOff = false;
            mInitScheduleOn = true;
            //auto let user select time.
            setTimeOnBtn.performClick();
        } else {
            mTimeScheduleOn_1.setVisibility(View.GONE);
            mTimeScheduleOn_3.setVisibility(View.GONE);
            mInitScheduleOn = false;
        }

        btn.setActivated(mInitScheduleOn);

        showSaveAndEnable();
    }

    private void showSaveAndEnable() {
        ontimesavebtn.setVisibility(View.VISIBLE);
        ontimesavebtn.setEnabled(true);
    }

    public void switchScheduleOffClick(View view) {
        ImageButton btn = (ImageButton) view;
        if (!btn.isActivated()) {
            mTimeScheduleOff_1.setVisibility(View.VISIBLE);
            mTimeScheduleOff_3.setVisibility(View.VISIBLE);

            //at the same time auto close ON time.
            mTimeScheduleOn_1.setVisibility(View.GONE);
            mTimeScheduleOn_3.setVisibility(View.GONE);

            switchScheduleOn.setActivated(false);
            mInitScheduleOn = false;

            mInitScheduleOff = true;
            setTimeOffBtn.performClick();
        } else {
            mTimeScheduleOff_1.setVisibility(View.GONE);
            mTimeScheduleOff_3.setVisibility(View.GONE);
//            device.stopScheduleOff();
            mInitScheduleOff = false;
        }

        btn.setActivated(mInitScheduleOff);

        showSaveAndEnable();
    }

    public void onSetTimeForTurnOn(View view) {
        TimePickerDialog timeOnPicker = new TimePickerDialog(this, timeOnListener, mInitScheduleOn_hour, mInitScheduleOn_minute, true);
        timeOnPicker.show();
    }

    public void onSetTimeForTurnOff(View view) {
//        SharedPreferences p = getSharedPreferences("timer", Context.MODE_PRIVATE);
//        int h = p.getInt("timeOffHour" + deviceAddress, 0);
//        int m = p.getInt("timeOffMinute" + deviceAddress, 0);
        TimePickerDialog timeOffPicker = new TimePickerDialog(this, timeOffListener, mInitScheduleOff_hour, mInitScheduleOff_minute, true);
        timeOffPicker.show();
    }

    @Override
    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
        mBrightProgress  = progress;
    }

    @Override
    public void onStartTrackingTouch(SeekBar seekBar) {
    	//
    }

    @Override
    public void onStopTrackingTouch(SeekBar seekBar) {
    	device.changeBright(1, mBrightProgress);
    }

    private TimePickerDialog.OnTimeSetListener timeOnListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            mInitScheduleOn_hour = hourOfDay;
            mInitScheduleOn_minute = minute;

            setTimeOnBtn.setText(hourOfDay + ":" + minute);
            showSaveAndEnable();
        }
    };

    private TimePickerDialog.OnTimeSetListener timeOffListener = new TimePickerDialog.OnTimeSetListener() {
        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
        	

            Calendar calendar = new GregorianCalendar();
            int second = calendar.get(Calendar.SECOND);
            if (second > 29) {
                minute++;
                if (minute == 60) {
                    minute = 0;
                    hourOfDay++;
                }
            }

        	
            mInitScheduleOff_hour = hourOfDay;
            mInitScheduleOff_minute = minute;

            setTimeOffBtn.setText(hourOfDay + ":" + minute);
            showSaveAndEnable();
        }
    };

    public void onTimeSaveClick(View view) {
        Log.d("schedule", "onTimeSaveClick-------------->in");
        ontimesavebtn.setEnabled(false);
       int result = -1;

        SharedPreferences sp = getSharedPreferences(SharedPrefManager.PRE_FILE_TIME, Context.MODE_PRIVATE);
        boolean old_timeOffStatus = sp.getBoolean(SharedPrefManager.PRE_KEY_timeOffStatus + deviceAddress, false);
        boolean old_timeOnStatus = sp.getBoolean(SharedPrefManager.PRE_KEY_timeOnStatus + deviceAddress, false);
        Log.d("schedule", "onTimeSaveClick:on:old="+old_timeOnStatus+".new="+mInitScheduleOn);
        Log.d("schedule", "onTimeSaveClick:off::old="+old_timeOffStatus+".new="+mInitScheduleOff);

        if (old_timeOffStatus != mInitScheduleOff) {
            sp.edit().putBoolean(SharedPrefManager.PRE_KEY_timeOffStatus + deviceAddress, mInitScheduleOff).apply();
        }

        if (old_timeOnStatus != mInitScheduleOn) {
            sp.edit().putBoolean(SharedPrefManager.PRE_KEY_timeOnStatus + deviceAddress, mInitScheduleOn).apply();
        }
        
        //
        if (mInitScheduleOn) {
            int old_timeOnHour = sp.getInt(SharedPrefManager.PRE_KEY_timeOnHour + deviceAddress, 0);
            int old_timeOnMinute = sp.getInt(SharedPrefManager.PRE_KEY_timeOnMinute + deviceAddress, 0);
            int old_timeOnSpeed = sp.getInt(SharedPrefManager.PRE_KEY_timeOn_SPEED + deviceAddress, 0);
            if (old_timeOnHour != mInitScheduleOn_hour || old_timeOnMinute != mInitScheduleOn_minute
                    || old_timeOnSpeed != mInitScheduleOn_speed) {
                sp.edit().putInt(SharedPrefManager.PRE_KEY_timeOnHour + deviceAddress, mInitScheduleOn_hour)
                        .putInt(SharedPrefManager.PRE_KEY_timeOnMinute + deviceAddress, mInitScheduleOn_minute)
                        .putInt(SharedPrefManager.PRE_KEY_timeOn_SPEED + deviceAddress, mInitScheduleOn_speed).apply();
                Log.d("schedule", "onTimeSaveClick: do startScheduleOn");
                result = device.startScheduleOn(mInitScheduleOn_hour, mInitScheduleOn_minute, mInitScheduleOn_speed);
            }
            Toast.makeText(this, "设置成功", Toast.LENGTH_SHORT).show();
            Log.d("schedule", "onTimeSaveClick1-------------->out");
            return ;
        }
        
        if (mInitScheduleOff) {
            int old_timeOffHour = sp.getInt(SharedPrefManager.PRE_KEY_timeOffHour + deviceAddress, 0);
            int old_timeOffMinute = sp.getInt(SharedPrefManager.PRE_KEY_timeOffMinute + deviceAddress, 0);
            int old_timeOffSpeed = sp.getInt(SharedPrefManager.PRE_KEY_timeOffMinute + deviceAddress, 0);
            if (old_timeOffHour != mInitScheduleOff_hour || old_timeOffMinute != mInitScheduleOff_minute
                    || old_timeOffSpeed != mInitScheduleOff_speed) {
                sp.edit().putInt(SharedPrefManager.PRE_KEY_timeOffHour + deviceAddress, mInitScheduleOff_hour)
                        .putInt(SharedPrefManager.PRE_KEY_timeOffMinute + deviceAddress, mInitScheduleOff_minute)
                        .putInt(SharedPrefManager.PRE_KEY_timeOff_SPEED + deviceAddress, mInitScheduleOff_speed).apply();
            }

            Log.d("schedule", "onTimeSaveClick: do startScheduleOff");
            result = device.startScheduleOff(mInitScheduleOff_hour, mInitScheduleOff_minute, mInitScheduleOff_speed);
            Toast.makeText(this, "设置成功", Toast.LENGTH_SHORT).show();
            Log.d("schedule", "onTimeSaveClick2-------------->out");
            return;
        }
        
        if (old_timeOffStatus != mInitScheduleOff) {
            if (!mInitScheduleOff) {
                result = device.stopScheduleOff();
            }
        }
        else 
        if (old_timeOnStatus != mInitScheduleOn) {
            if (!mInitScheduleOn) {
                result = device.stopScheduleOn();
            }
        }

        if (result >= 0) {
            Toast.makeText(this, "设置成功", Toast.LENGTH_SHORT).show();
        }

        Log.d("schedule", "onTimeSaveClick-------------->out");
    }

    public void onGoingOffBtn3Click(View view) {
        mInitScheduleOff_speed = 2;
        updateGoingBtn(false, mInitScheduleOff_speed);
        showSaveAndEnable();
    }
    public void onGoingOffBtn2Click(View view) {
        mInitScheduleOff_speed = 1;
        updateGoingBtn(false, mInitScheduleOff_speed);
        showSaveAndEnable();
    }
    public void onGoingOffBtn1Click(View view) {
        mInitScheduleOff_speed = 0;
        updateGoingBtn(false, mInitScheduleOff_speed);
        showSaveAndEnable();
    }
    public void onGoingOnBtn1Click(View view) {
        mInitScheduleOn_speed = 0;
        updateGoingBtn(true, mInitScheduleOn_speed);
        showSaveAndEnable();
    }
    public void onGoingOnBtn2Click(View view) {
        mInitScheduleOn_speed = 1;
        updateGoingBtn(true, mInitScheduleOn_speed);
        showSaveAndEnable();
    }
    public void onGoingOnBtn3Click(View view) {
        mInitScheduleOn_speed = 2;
        updateGoingBtn(true, mInitScheduleOn_speed);
        showSaveAndEnable();
    }


    class LightSet {
        String name;
        int icon;
    }

    private int[] lightIcon = {
    	R.drawable.ic_mingliang,R.drawable.ic_mingliang,R.drawable.ic_mingliang,R.drawable.ic_mingliang,R.drawable.ic_mingliang,
        R.drawable.ic_mingliang,R.drawable.ic_myueguang,R.drawable.ic_myuedu,
        R.drawable.ic_lenglie, R.drawable.ic_mwennuan,R.drawable.ic_meihuo,
        R.drawable.ic_jianbian,R.drawable.ic_jianbian,R.drawable.ic_jianbian,R.drawable.ic_jianbian,R.drawable.ic_jianbian,
        
//        R.drawable.ic_edit
    };
    private String[] lightName = {
        "照明","淡雅","暖光","晨曦","暮光","明亮", "月光", "阅读", "冷冽","温暖","魅惑","变色1","变色2","变色3","变色4","变色5"
    };
    private ArrayList<LightSet> initLightSet() {
        ArrayList<LightSet> lightSet = new ArrayList<LightSet>();
        for (int i = 0; i < lightIcon.length; i++) {
            LightSet set = new LightSet();
            set.icon = lightIcon[i];
            set.name = lightName[i];
            lightSet.add(set);
        }
        return lightSet;
    }
    class GridAdapter extends BaseAdapter {

        private final Context mContext;
        private ArrayList<LightSet> mLightSet;

        GridAdapter(Context context, ArrayList<LightSet> lightSet) {
            super();
            mContext = context;
            mInflater = (LayoutInflater) mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mLightSet = lightSet;
        }

        private LayoutInflater mInflater;

        @Override
        public int getCount() {
            return mLightSet !=null ?mLightSet.size():0;
        }

        @Override
        public Object getItem(int i) {
            return null;
        }

        @Override
        public long getItemId(int i) {
            return 0;
        }

        @Override
        public View getView(int i, View convertView, ViewGroup viewGroup) {
            View v = convertView;
            final ViewHolder viewHolder;
            if (v == null) {
                v = mInflater.inflate(R.layout.gridview_item, null);
                viewHolder = new ViewHolder(v);
                if (v != null) {
                    v.setTag(viewHolder);
                }
            } else {
                viewHolder = (ViewHolder) v.getTag();
            }

            final LightSet light = mLightSet.get(i);
            viewHolder.icon.setImageResource(light.icon);
            viewHolder.name.setText(light.name);
            return v;
        }

        class ViewHolder {
            public TextView name;
            public ImageView icon;

            public ViewHolder(View view) {
                name = (TextView) view.findViewById(R.id.lightName);
                icon = (ImageView) view.findViewById(R.id.lightTou);
            }
        }
    }
}
