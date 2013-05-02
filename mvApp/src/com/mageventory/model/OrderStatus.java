package com.mageventory.model;

import java.io.Serializable;

public class OrderStatus implements Serializable {
	public String mStatusCode;
	public String mStatusLabel;
	
	public OrderStatus(String statusCode, String statusLabel)
	{
		mStatusCode = statusCode;
		mStatusLabel = statusLabel;
	}
}
