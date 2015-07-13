package com.example.bleproximitydetector;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothAdapter.LeScanCallback;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.ParcelUuid;
import android.view.View;
import android.widget.Toast;


public class MainActivity extends Activity {

	BluetoothAdapter mBluetoothAdapter = null;
	BluetoothLeScanner mBluetoothLeScanner = null;
	
	private final static int REQUEST_ENABLE_BT = 52231;
	private BluetoothGatt mBluetoothGatt = null;
	
	private boolean mScanning;
    private Handler mHandler = new Handler();
    
    private HashMap<String, BluetoothDevice> mDeviceMap = new HashMap<String, BluetoothDevice>();

    // Stops scanning after 10 seconds.
    private static final long SCAN_PERIOD = 10000;//30000;
	
    @SuppressLint("NewApi") @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "BLE Not Supported", Toast.LENGTH_LONG).show();
            finish();
        }
        
     // Initializes Bluetooth adapter.
        final BluetoothManager bluetoothManager =
                (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = bluetoothManager.getAdapter();
        mBluetoothLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
        
        
     
     
     System.out.println("XXXXXX this device's address is::"+mBluetoothAdapter.getAddress());
     findViewById(R.id.button_scan).setOnClickListener(new View.OnClickListener() {
		
		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			//scanLeDevice(true);
			scanAllDevices(true);
		
			
		}
	});
     
     findViewById(R.id.button_scan_ble).setOnClickListener(new View.OnClickListener() {
 		
 		@Override
 		public void onClick(View v) {
 			// TODO Auto-generated method stub
 			scanLeDevice(true);
 			//scanAllDevices(true);
 		
 			
 		}
 	});
     
  // Ensures Bluetooth is available on the device and it is enabled. If not,
     // displays a dialog requesting user permission to enable Bluetooth.
     if (mBluetoothAdapter == null || !mBluetoothAdapter.isEnabled()) {
         Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
         startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
     }
    }
    
    private void scanAllDevices(boolean state)
    {
    	if(!mBluetoothAdapter.isDiscovering())
    	{
    	mBluetoothAdapter.startDiscovery();
    	// Register the BroadcastReceiver
    	IntentFilter filter = new IntentFilter(BluetoothDevice.ACTION_FOUND);
    	
    	registerReceiver(mReceiver, filter); // Don't forget to unregister during onDestroy
    	}
    }
    
 // Create a BroadcastReceiver for ACTION_FOUND
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            // When discovery finds a device
            if (BluetoothDevice.ACTION_FOUND.equals(action)) {
                // Get the BluetoothDevice object from the Intent
                BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                //String rssi = intent.getParcelableExtra(BluetoothDevice.EXTRA_RSSI);
                short def = 0;
                short rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, def);
                // Add the name and address to an array adapter to show in a ListView
                System.out.println("device name:: "+device.getName() + " device address::" + device.getAddress());
                System.out.println(" device rssi distance::" + (getDistance(rssi, 0)/1000)+" mts.");
                Toast.makeText(MainActivity.this, device.getName()+" is at "+(getDistance(rssi, 0)/1000)+" mts.", 
                		Toast.LENGTH_LONG).show();
            }
        }
    };
    
    
    private void scanLeDevice(final boolean enable) {
        if (enable) {
            // Stops scanning after a pre-defined scan period.
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mScanning = false;
                    System.out.println("XXXXX stopping scan and init connection");
                    mBluetoothLeScanner.stopScan(mScanCallback);
                    initConnection(mDeviceMap);
                    
                }
            }, SCAN_PERIOD);

            mScanning = true;
            ScanSettings settings = new ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .build();
            System.out.println("XXXXXXX bluetooth le scanner is ::"+mBluetoothLeScanner);
            mBluetoothLeScanner.startScan(null, settings, mScanCallback);
        } else {
            mScanning = false;
            mBluetoothLeScanner.stopScan(mScanCallback);
            
        }
        
    }
    
    private ScanCallback mScanCallback = new ScanCallback() {
        @Override
        public void onScanResult(int callbackType, ScanResult result) {
        	super.onScanResult(callbackType, result);
            System.out.println("XXXXXX onscanresult");
            processResult(result);
        }

        @Override
        public void onBatchScanResults(List<ScanResult> results) {
        	super.onBatchScanResults(results);
            System.out.println("XXXXXXonBatchScanResults: "+results.size()+" results");
            for (ScanResult result : results) {
                processResult(result);
            }
        }

        @Override
        public void onScanFailed(int errorCode) {
        	super.onScanFailed(errorCode);
            System.out.println("XXXXXXXXLE Scan Failed: "+errorCode);
        }

        private void processResult(ScanResult result) {
        	System.out.println("XXXXXXX New LE Device Address: " + result.getDevice().getAddress());
        	int txPower = result.getScanRecord().getTxPowerLevel();
        	
        	
        	System.out.println("XXXXXX txpower ::"+txPower+" and Integer.min value::"+Integer.MIN_VALUE);
            System.out.println("XXXXXXX New LE Device Details: " + result.getDevice().getName() + " @rssi:: " + result.getRssi());

            BluetoothDevice device = result.getDevice();
            //if(device.getAddress().equals("72:7B:46:65:82:74"))
            {
            	//System.out.println("XXXXXXXX karbonn found");
            	mDeviceMap.put(device.getAddress(), device);
            }
            
        }
        
        
    };
    
    private void initConnection(HashMap<String, BluetoothDevice> deviceMap)
    {
    	for(Map.Entry<String, BluetoothDevice> dev : deviceMap.entrySet())
    	{
    		mBluetoothGatt = dev.getValue().connectGatt(this, false, mGattCallback);
    	}
    }
    
    double getDistance(int rssi, int txPower) {
        /*
         * RSSI = TxPower - 10 * n * lg(d)
         * n = 2 (in free space)
         * 
         * d = 10 ^ ((TxPower - RSSI) / (10 * n))
         */

        //return Math.pow(10d, ((double) txPower - rssi) / (10 * 2));
    	return Math.pow(10d, ((double) txPower - rssi) / (10 * 2));
    }
    
 // Various callback methods defined by the BLE API.
    private BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status,
                int newState) 
        {
        	final BluetoothGatt bgt = gatt;
            String intentAction;
            
            System.out.println("XXXXXX on connection state change and status is::"+status+" and state is::"+newState);
            if (newState == BluetoothProfile.STATE_CONNECTED) 
            {
            	/*TimerTask task = new TimerTask()
            	   {
            	      @Override
            	      public void run()
            	      {
            	    	 System.out.println("XXXXXXXX reading remote rssi for::"+bgt.getDevice().getAddress());
            	         bgt.readRemoteRssi();
            	      }
            	   };
            	   Timer rssiTimer = new Timer();
            	   rssiTimer.schedule(task, 1000, 10000);*/
            	//bgt.readRemoteRssi();
            	bgt.discoverServices();
            	//bgt.getService(uuid)
            	//bgt.get
            	   
                

            } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                
            }
        }

        @Override
        // New services discovered
        public void onServicesDiscovered(BluetoothGatt gatt, int status) 
        {
            if (status == BluetoothGatt.GATT_SUCCESS) {
               // broadcastUpdate(ACTION_GATT_SERVICES_DISCOVERED);
            	 List<BluetoothGattService> services = gatt.getServices();
                 System.out.println("XXXXXXXonServicesDiscovered::"+services.toString());
                 System.out.println("XXXXXXXNumber of services::"+services.size());
                 //gatt.readCharacteristic(services.get(1).getCharacteristics().get
                   //      (0));
                 //go over all services and all characteristic
                 System.out.println("XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX Getting power service");
                 BluetoothGattService powerServ = gatt.getService(UUID.fromString("00001804-0000-1000-8000-00805f9b34fb"));
                 BluetoothGattCharacteristic powerChar = powerServ.getCharacteristic(UUID.fromString("00002a07-0000-1000-8000-00805f9b34fb"));
                 byte[] val = powerChar.getValue();
                 System.out.println("XXXXXXXXXX val for power char::"+val);
                 /*for(BluetoothGattService service:services)
                 {
                	 System.out.println("XXXXXXXZZZZZZZZZSSSSSSSSS service uuid is::"+service.getUuid());
                	 List<BluetoothGattCharacteristic> charList = service.getCharacteristics();
                	 
                	 for(BluetoothGattCharacteristic charec : charList)
                	 {
                		 //System.out.println("XXXXXXXBBBBBBBBBBSSSSSSSSS characteristic uuid is::"+charec.getUuid());
                	 }
                	 //service.
                 }*/
                 
            } else {
                //Log.w(TAG, "onServicesDiscovered received: " + status);
            }
            
            
            
            gatt.disconnect();
            gatt.close();
        }
        
        

        @Override
        // Result of a characteristic read operation
        public void onCharacteristicRead(BluetoothGatt gatt,
                BluetoothGattCharacteristic characteristic,
                int status) 
        {
            if (status == BluetoothGatt.GATT_SUCCESS) {
                //broadcastUpdate(ACTION_DATA_AVAILABLE, characteristic);
            }
        }
        
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
        	System.out.println("XXXXXXX rssi read status is::"+status);
        	System.out.println("XXXXXXX NAme of dev is::"+gatt.getDevice().getName());
        	System.out.println("XXXXXXX RSSI for::"+gatt.getDevice().getAddress()+" is::"+rssi);
        };
    };


   
}
