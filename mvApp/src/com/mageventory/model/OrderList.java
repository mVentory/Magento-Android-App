package com.mageventory.model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class OrderList implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<OrderStatus> orderStatusList = new ArrayList<OrderStatus>();
    private Map<String, Object> data;

    public OrderList(List<OrderStatus> orderStatusList, Map<String, Object> data) {
        super();
        this.orderStatusList = orderStatusList;
        this.data = data;
    }

    public List<OrderStatus> getOrderStatusList() {
        return orderStatusList;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
