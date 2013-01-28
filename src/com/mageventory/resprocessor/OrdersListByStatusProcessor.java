package com.mageventory.resprocessor;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.MyApplication;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.OrderStatus;
import com.mageventory.model.Product;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.xmlrpc.XMLRPCException;
import com.mageventory.xmlrpc.XMLRPCFault;

public class OrdersListByStatusProcessor implements IProcessor, MageventoryConstants {
	
	public static final String SHIPPING_CART_STATUS_CODE = "__shipping_cart_status_code";
	
	@Override
	public Bundle process(Context context, String[] params, Bundle extras) {
		
		SettingsSnapshot ss = (SettingsSnapshot)extras.get(EKEY_SETTINGS_SNAPSHOT);
		
		MagentoClient client;
		try {
			client = new MagentoClient(ss);
		} catch (MalformedURLException e) {
			throw new RuntimeException(e.getMessage());
		}

		final Map<String, Object> orderList = client.orderListByStatus(params[0]);

		if (orderList != null) {
			
			String [] statusCodeList = ((Map<String, Object>)orderList.get("statuses")).keySet().toArray(new String [0]);
			String [] statusLabelsList = ((Map<String, Object>)orderList.get("statuses")).values().toArray(new String [0]);
			
			ArrayList<OrderStatus> orderStatusList = new ArrayList<OrderStatus>();
			
			orderStatusList.add(new OrderStatus("", "Latest"));
			orderStatusList.add(new OrderStatus(SHIPPING_CART_STATUS_CODE, "Shipping cart"));
			
			for (int i=0; i<statusLabelsList.length; i++)
			{
				orderStatusList.add(new OrderStatus(statusCodeList[i], statusLabelsList[i]));
			}
			
			Collections.sort(orderStatusList, new Comparator<OrderStatus>() {
				
				@Override
				public int compare(OrderStatus lhs, OrderStatus rhs) {
					return lhs.mStatusLabel.compareTo(rhs.mStatusLabel);
				}
			});
			
			orderList.put("statuses", orderStatusList);
			
			JobCacheManager.storeOrderList(orderList, params, ss.getUrl());
		} else {
			throw new RuntimeException(client.getLastErrorMessage());
		}
		
		return null;
	}

}
