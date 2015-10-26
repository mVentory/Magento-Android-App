/* Copyright (c) 2014 mVentory Ltd. (http://mventory.com)
 * 
* License       http://creativecommons.org/licenses/by-nc-nd/4.0/
* 
* NonCommercial — You may not use the material for commercial purposes. 
* NoDerivatives — If you compile, transform, or build upon the material,
* you may not distribute the modified material. 
* Attribution — You must give appropriate credit, provide a link to the license,
* and indicate if changes were made. You may do so in any reasonable manner, 
* but not in any way that suggests the licensor endorses you or your use. 
*/

package com.mageventory.resprocessor;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;

import android.content.Context;
import android.os.Bundle;

import com.mageventory.MageventoryConstants;
import com.mageventory.client.MagentoClient;
import com.mageventory.job.JobCacheManager;
import com.mageventory.model.OrderList;
import com.mageventory.model.OrderStatus;
import com.mageventory.res.ResourceProcessorManager.IProcessor;
import com.mageventory.settings.SettingsSnapshot;
import com.mageventory.tasks.CreateNewOrderForMultipleProds;

public class OrdersListByStatusProcessor implements IProcessor, MageventoryConstants {

    public static final String SHOPPING_CART_STATUS_CODE = "__shopping_cart_status_code";
    public static final String QUEUED_STATUS_CODE = "__queued";
    public static final String LATEST_STATUS_CODE = "";

    @Override
    public Bundle process(Context context, String[] params, Bundle extras) {

        SettingsSnapshot ss = (SettingsSnapshot) extras.get(EKEY_SETTINGS_SNAPSHOT);

        MagentoClient client;
        try {
            client = new MagentoClient(ss);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e.getMessage());
        }

        final Map<String, Object> orderListMap = client.orderListByStatus(params[0]);

        if (orderListMap != null) {

            String[] statusCodeList = ((Map<String, Object>) orderListMap.get("statuses")).keySet()
                    .toArray(new String[0]);
            String[] statusLabelsList = ((Map<String, Object>) orderListMap.get("statuses"))
                    .values().toArray(new String[0]);

            ArrayList<OrderStatus> orderStatusList = new ArrayList<OrderStatus>();

            orderStatusList.add(new OrderStatus("", "Latest"));
            orderStatusList.add(new OrderStatus(SHOPPING_CART_STATUS_CODE, "Shopping cart"));
            orderStatusList.add(new OrderStatus(QUEUED_STATUS_CODE,
                    CreateNewOrderForMultipleProds.MULTIPLE_PRODUCTS_ORDER_QUEUED_STATE));

            for (int i = 0; i < statusLabelsList.length; i++)
            {
                orderStatusList.add(new OrderStatus(statusCodeList[i], statusLabelsList[i]));
            }

            Collections.sort(orderStatusList, new Comparator<OrderStatus>() {

                @Override
                public int compare(OrderStatus lhs, OrderStatus rhs) {
                    return lhs.mStatusLabel.compareTo(rhs.mStatusLabel);
                }
            });

            OrderList orderList = new OrderList(orderStatusList, orderListMap);

            JobCacheManager.storeOrderList(orderList, params, ss.getUrl());
        } else {
            throw new RuntimeException(client.getLastErrorMessage());
        }

        return null;
    }

}
