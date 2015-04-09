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
 * ��demo����չʾ��ν�϶�λSDKʵ�ֶ�λ����ʹ��MyLocationOverlay���ƶ�λλ�� ͬʱ���������ҽԺ
 * 
 */
public class MainActivity extends Activity {

	// ��λ���
	LocationClient mLocClient;
	public MyLocationListenner myListener = new MyLocationListenner();
	

	MapView mMapView;
	BaiduMap mBaiduMap;
	
	private int load_Index;
	private PoiSearch mPoiSearch;
	
	private double latitude;  //γ��
	private double longitude;  //����

	// UI���
	boolean isFirstLoc = true;// �Ƿ��״ζ�λ
	
	//�������㲥
	//����һ���㲥������
	public class SDKReceiver extends BroadcastReceiver {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR)) {
				new AlertDialog.Builder(MainActivity.this)
				               .setMessage("������󣡣�")
				               .setPositiveButton("OK", null)
				               .show();
			}
			
		}
		
	}
	

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		//ע��㲥�����¼�
		IntentFilter ifilter = new IntentFilter();
		ifilter.addAction(SDKInitializer.SDK_BROADCAST_ACTION_STRING_NETWORK_ERROR);
		SDKReceiver mBroadcastRec = new SDKReceiver(); 
		registerReceiver(mBroadcastRec, ifilter);
		
		mPoiSearch = PoiSearch.newInstance();
		OnGetPoiSearchResultListener poiListener = new OnGetPoiSearchResultListener(){  
		    public void onGetPoiResult(PoiResult result){  
		    //��ȡPOI�������  
		    	if (result == null) {
					Toast.makeText(MainActivity.this, "δ�ҵ����", Toast.LENGTH_LONG)
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
		    //��ȡPlace����ҳ�������  
		    	if (result.error != SearchResult.ERRORNO.NO_ERROR) {
					Toast.makeText(MainActivity.this, "��Ǹ��δ�ҵ����", Toast.LENGTH_SHORT)
							.show();
				} else {
					//�Ի�����ʾҽԺ��Ϣ
					new AlertDialog.Builder(MainActivity.this)
					               .setTitle(result.getName())
					               .setMessage(result.getAddress())
					               .setPositiveButton("OK", null)
					               .show();
					
				}
		    }  
		};
		mPoiSearch.setOnGetPoiSearchResultListener(poiListener);

		

		// ��ͼ��ʼ��
		mMapView = (MapView) findViewById(R.id.bmapView);
		mBaiduMap = mMapView.getMap();
		// ������λͼ��
		mBaiduMap.setMyLocationEnabled(true);
		// ��λ��ʼ��
		mLocClient = new LocationClient(this);
		mLocClient.registerLocationListener(myListener);
		LocationClientOption option = new LocationClientOption();
		option.setOpenGps(true);// ��GPS
		option.setCoorType("bd09ll"); // ������������
		option.setScanSpan(1000);
		mLocClient.setLocOption(option);
		mLocClient.start();
	}
	
	/**
	 * ���õ�ͼ��ʾģʽ
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
	 * �����Ƿ���ʾ��ͨͼ
	 * 
	 * @param view
	 */
	public void setTraffic(View view) {
		mBaiduMap.setTrafficEnabled(((CheckBox) view).isChecked());
	}

	/**
	 * ��λSDK��������
	 */
	
	public class MyLocationListenner implements BDLocationListener {

		private BitmapDescriptor mCurrentMarker;
		private LocationMode mCurrentMode;

		@Override
		public void onReceiveLocation(BDLocation location) {
			// map view ���ٺ��ڴ����½��յ�λ��
			if (location == null || mMapView == null)
				return;
			MyLocationData locData = new MyLocationData.Builder()
					.accuracy(location.getRadius())
					// �˴����ÿ����߻�ȡ���ķ�����Ϣ��˳ʱ��0-360
					.direction(100).latitude(location.getLatitude())
					.longitude(location.getLongitude()).build();
			mBaiduMap.setMyLocationData(locData);
			//�Զ��嶨λͼ��
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
				.keyword("ҽԺ")
				.location(new LatLng(latitude, longitude))
				.pageCapacity(20)
				.pageNum(load_Index)
				.radius(5000));
		
		 //System.out.println("���ȣ�"+latitude);
	     //System.out.println("γ�ȣ�"+longitude);
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
		// �˳�ʱ���ٶ�λ
		mLocClient.stop();
		// �رն�λͼ��
		mBaiduMap.setMyLocationEnabled(false);
		mMapView.onDestroy();
		mMapView = null;
		mPoiSearch.destroy();
		super.onDestroy();
	}

}
