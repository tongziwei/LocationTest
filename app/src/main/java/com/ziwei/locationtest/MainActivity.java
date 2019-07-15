package com.ziwei.locationtest;

import android.Manifest;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public final int REQUEST_PERMISSION = 1;
    public LocationClient mLocationClient;
    private TextView mTvPosition;
    private List<String> permissionList;
    private MapView mapView;
    private BaiduMap baiduMap;
    private boolean isFirstLocate = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mLocationClient = new LocationClient(getApplicationContext());
        mLocationClient.registerLocationListener(new MyLocationListener());
        SDKInitializer.initialize(getApplicationContext()); //初始化需要在setContentView前调用

        setContentView(R.layout.activity_main);
        mTvPosition = (TextView)findViewById(R.id.tv_position);
        mapView = (MapView)findViewById(R.id.bmapView);
        baiduMap = mapView.getMap(); //BaiduMap 地图总控制器
        baiduMap.setMyLocationEnabled(true);
        permissionList = new ArrayList<>();
        checkPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mapView.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocationClient.stop();
        mapView.onDestroy(); //保证资源得到释放
        baiduMap.setMyLocationEnabled(false);
    }

    private void checkPermission(){
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_FINE_LOCATION)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.ACCESS_FINE_LOCATION);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.READ_PHONE_STATE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.READ_PHONE_STATE);
        }
        if(ContextCompat.checkSelfPermission(MainActivity.this,Manifest.permission.WRITE_EXTERNAL_STORAGE)!=PackageManager.PERMISSION_GRANTED){
            permissionList.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);
        }
        if(!permissionList.isEmpty()){
            String[] permissions = permissionList.toArray(new String[permissionList.size()]);
            ActivityCompat.requestPermissions(MainActivity.this,permissions,REQUEST_PERMISSION);
        }else{
            requestLocation();
        }
    }

    private void requestLocation(){
        initLocation();
       mLocationClient.start();
    }

    private void initLocation(){
        LocationClientOption option = new LocationClientOption();
        option.setScanSpan(5000); //设置更新间隔
       // option.setLocationMode(LocationClientOption.LocationMode.Device_Sensors); //设置定位模式为传感器模式，即只能使用GPS定位
        option.setIsNeedAddress(true);//需要获取地址的详细信息
        mLocationClient.setLocOption(option);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_PERMISSION:
                if(grantResults.length>0){
                    for(int result :grantResults){
                        if(result!= PackageManager.PERMISSION_GRANTED){
                            Toast.makeText(this,"必须同意所有权限才能使用本程序",Toast.LENGTH_SHORT).show();
                            finish();
                            return;
                        }
                    }
                    requestLocation();
                }else{
                    Toast.makeText(this,"发生未知错误",Toast.LENGTH_SHORT).show();
                    finish();
                }
                break;
                default:
        }
    }


    /**
     * 将地图定位到设备所在位置，地图默认显示北京市
     * @param location
     */
    private void navigateTo(BDLocation location){
        if(isFirstLocate){           //防止程序多次调用animateMapStatus
            LatLng ll =  new LatLng(location.getLatitude(),location.getLongitude());   //LatLng 存放经纬度
            MapStatusUpdate update = MapStatusUpdateFactory.newLatLng(ll);
            baiduMap.animateMapStatus(update);
            update = MapStatusUpdateFactory.zoomTo(16f);  //缩放级别，取值范围3-19，可以是小数，值越大，显示越精细
            baiduMap.animateMapStatus(update);
            isFirstLocate = false;
        }
        MyLocationData.Builder locationBuilder = new MyLocationData.Builder();
        locationBuilder.latitude(location.getLatitude());
        locationBuilder.longitude(location.getLongitude());
        MyLocationData locationData = locationBuilder.build();
        baiduMap.setMyLocationData(locationData);
    }

    public class MyLocationListener implements BDLocationListener{
        @Override
        public void onReceiveLocation(BDLocation bdLocation) {
            if(bdLocation.getLocType() == BDLocation.TypeGpsLocation || bdLocation.getLocType()== BDLocation.TypeNetWorkLocation){
                navigateTo(bdLocation);  //地图移动到设备所在位置
            }

            StringBuilder currentPosition = new StringBuilder();
            currentPosition.append("纬度：").append(bdLocation.getLatitude()).append("\n");
            currentPosition.append("经度：").append(bdLocation.getLongitude()).append("\n");
            currentPosition.append("国家：").append(bdLocation.getCountry()).append("\n");
            currentPosition.append("省：").append(bdLocation.getProvince()).append("\n");
            currentPosition.append("市：").append(bdLocation.getCity()).append("\n");
            currentPosition.append("区：").append(bdLocation.getDistrict()).append("\n");
            currentPosition.append("街道：").append(bdLocation.getStreet()).append("\n");
            currentPosition.append("定位方式：");
            if(bdLocation.getLocType() == BDLocation.TypeGpsLocation){
                currentPosition.append("GPS");
            }else if(bdLocation.getLocType() ==BDLocation.TypeNetWorkLocation){
                currentPosition.append("网络");
            }
            mTvPosition.setText(currentPosition);
        }
    }
}
