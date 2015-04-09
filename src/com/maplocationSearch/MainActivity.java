package com.maplocationSearch;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.view.View;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.Toast;

import com.baidu.location.BDLocation;
import com.baidu.location.BDLocationListener;
import com.baidu.location.LocationClient;
import com.baidu.location.LocationClientOption;
import com.baidu.mapapi.SDKInitializer;
import com.baidu.mapapi.map.BaiduMap;
import com.baidu.mapapi.map.BitmapDescriptor;
import com.baidu.mapapi.map.BitmapDescriptorFactory;
import com.baidu.mapapi.map.MapStatusUpdate;
import com.baidu.mapapi.map.MapStatusUpdateFactory;
import com.baidu.mapapi.map.MapView;
import com.baidu.mapapi.map.MyLocationConfiguration;
import com.baidu.mapapi.map.MyLocationConfiguration.LocationMode;
import com.baidu.mapapi.map.MyLocationData;
import com.baidu.mapapi.model.LatLng;
import com.baidu.mapapi.overlayutil.PoiOverlay;
import com.baidu.mapapi.search.core.PoiInfo;
import com.baidu.mapapi.search.core.SearchResult;
import com.baidu.mapapi.search.poi.OnGetPoiSearchResultListener;
import com.baidu.mapapi.search.poi.PoiDetailResult;
import com.baidu.mapapi.search.poi.PoiDetailSearchOption;
import com.baidu.mapapi.search.poi.PoiNearbySearchOption;
import com.baidu.mapapi.search.poi.PoiResult;
import com.baidu.mapapi.search.poi.PoiSearch;
import com.maplocationSearch.R;

/**
 * 此demo用来展示如何结合定位SDK实现定位，并使用MyLocationOverlay绘制定位位置 同时标出附近的医院
 * 
 */
public class MainActivity extends Activity {

	// 定位相关
	LocationClient mLocClient;
	public MyLocationListenner myListener = new MyLocationListenner();
	

	MapView mMapView;
	BaiduMap mBaiduMap;
	
	private int load_Index;
	private PoiSearch mPoiSearch;
	
	private double latitude;  //纬度
	private double longitude;  //经度

	// UI相关
	boolean isFirstLoc = true;// 是否首次定位
	
	//网络错误广播
	//定义一个广播监听器
	public class SDKReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR)) {
				new AlertDialog.Builder(MainActivity.this)
				               .setMessage("网络错误！！")
				               .setPositiveButton("OK", null)
				               .show();
			}
			
		}
		
	}
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//注册广播监听事件
		IntentFilter ifilter = new IntentFilter();
		ifilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
		SDKReceiver mBroadcastRec = new SDKReceiver(); 
		registerReceiver(mBroadcastRec, ifilter);
		
		mPoiSearch = PoiSearch.newInstance();
		OnGetPoiSearchResultListener poiListener = new OnGetPoiSearchResultListener(){  
		    public void onGetPoiResult(PoiResult result){  
		    //获取POI检索结果  
		    	if (result == null) {
					Toast.makeText(MainActivity.this, "未找到结果", Toast.LENGTH_LONG)
					.show();
					return;
				}
				if (result.error == SearchResult.ERRORNO.NO_ERROR) {
					mBaiduMap.clear();
					PoiOverlay overlay = new MyPoiOverlay(mBaiduMap);
					mBaiduMap.setOnMarkerClickListener(overlay);
					overlay.setData(result);
					overlay.addToMap();
					overlay.zoomToSpan();
					return;
				}
		    }  
		    public void onGetPoiDetailResult(PoiDetailResult result){  
		    //获取Place详情页检索结果  
		    	if (result.error != SearchResult.ERRORNO.NO_ERROR) {
					Toast.makeText(MainActivity.this, "抱歉，未找到结果", Toast.LENGTH_SHORT)
							.show();
				} else {
					//对话框显示医院信息
					new AlertDialog.Builder(MainActivity.this)
					               .setTitle(result.getName())
					               .setMessage(result.getAddress())
					               .setPositiveButton("OK", null)
					               .show();
					
				}
		    }  
		};
		mPoiSearch.setOnGetPoiSearchResultListener(poiListener);

		

		// 地图初始化
		mMapView = (MapView) findViewById(R.id.bmapView);
		mBaiduMap = mMapView.getMap();
		// 开启定位图层
		mBaiduMap.setMyLocationEnabled(true);
		// 定位初始化
		mLocClient = new LocationClient(this);
		mLocClient.registerLocationListener(myListener);
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true);// 打开GPS
		option.setCoorType("bd09ll"); // 设置坐标类型
		option.setScanSpan(1000);
		mLocClient.setLocOption(option);
		mLocClient.start();
	}
	
	/**
	 * 设置底图显示模式
	 * 
	 * @param view
	 */
	public void setMapMode(View view) {
		boolean checked = ((RadioButton) view).isChecked();
		switch (view.getId()) {
		case R.id.normal:
			if (checked)
				mBaiduMap.setMapType(BaiduMap.MAP_TYPE_NORMAL);
			break;
		case R.id.statellite:
			if (checked)
				mBaiduMap.setMapType(BaiduMap.MAP_TYPE_SATELLITE);
			break;
		}
	}

	/**
	 * 设置是否显示交通图
	 * 
	 * @param view
	 */
	public void setTraffic(View view) {
		mBaiduMap.setTrafficEnabled(((CheckBox) view).isChecked());
	}

	/**
	 * 定位SDK监听函数
	 */
	
	public class MyLocationListenner implements BDLocationListener {

		private BitmapDescriptor mCurrentMarker;
		private LocationMode mCurrentMode;

		@Override
		public void onReceiveLocation(BDLocation location) {
			// map view 销毁后不在处理新接收的位置
			if (location == null || mMapView == null)
				return;
			MyLocationData locData = new MyLocationData.Builder()
					.accuracy(location.getRadius())
					// 此处设置开发者获取到的方向信息，顺时针0-360
					.direction(100).latitude(location.getLatitude())
					.longitude(location.getLongitude()).build();
			mBaiduMap.setMyLocationData(locData);
			//自定义定位图标
			mCurrentMarker = BitmapDescriptorFactory
					.fromResource(R.drawable.loc1_m);
			mCurrentMode = LocationMode.FOLLOWING;
			mBaiduMap
					.setMyLocationConfigeration(new MyLocationConfiguration(
							mCurrentMode, true, mCurrentMarker));
			
			
			if (isFirstLoc) {
				isFirstLoc = false;
				LatLng ll = new LatLng(location.getLatitude(),
						location.getLongitude());
				MapStatusUpdate u = MapStatusUpdateFactory.newLatLng(ll);
				mBaiduMap.animateMapStatus(u);
			}
		 latitude=location.getLatitude();
		 longitude=location.getLongitude();
		}

		public void onReceivePoi(BDLocation poiLocation) {
		}
	}
	
	public void searchButtonProcess(View v) {
		
		mPoiSearch.searchNearby((new PoiNearbySearchOption())
				.keyword("医院")
				.location(new LatLng(latitude, longitude))
				.pageCapacity(20)
				.pageNum(load_Index)
				.radius(5000));
		
		 //System.out.println("经度："+latitude);
	     //System.out.println("纬度："+longitude);
	}

	public void goToNextPage(View v) {
		load_Index++;
		searchButtonProcess(null);
	}
	
	private class MyPoiOverlay extends PoiOverlay {

		public MyPoiOverlay(BaiduMap baiduMap) {
			super(baiduMap);
		}

		@Override
		public boolean onPoiClick(int index) {
			super.onPoiClick(index);
			PoiInfo poi = getPoiResult().getAllPoi().get(index);
			// if (poi.hasCaterDetails) {
				mPoiSearch.searchPoiDetail((new PoiDetailSearchOption())
						.poiUid(poi.uid));
			// }
			return true;
		}
	}

	@Override
	protected void onPause() {
		mMapView.onPause();
		super.onPause();
	}

	@Override
	protected void onResume() {
		mMapView.onResume();
		super.onResume();
	}

	@Override
	protected void onDestroy() {
		// 退出时销毁定位
		mLocClient.stop();
		// 关闭定位图层
		mBaiduMap.setMyLocationEnabled(false);
		mMapView.onDestroy();
		mMapView = null;
		mPoiSearch.destroy();
		super.onDestroy();
	}

}
