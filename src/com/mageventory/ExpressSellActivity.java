package com.mageventory;

import com.mageventory.model.Product;

import android.os.Bundle;
import android.widget.EditText;

public class ExpressSellActivity extends BaseActivity implements MageventoryConstants{

	
	Product P = new Product();
	
	/* (non-Javadoc)
	 * @see android.app.Activity#onCreate(android.os.Bundle)
	 */
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);						
		setContentView(R.layout.express_sell);
		
		// read Description and SKU and Price 
		P.setName(getIntent().getStringExtra(MAGEKEY_PRODUCT_NAME));
		P.setDescription(getIntent().getStringExtra(MAGEKEY_PRODUCT_DESCRIPTION));
		P.setPrice(Double.valueOf((getIntent().getStringExtra(MAGEKEY_PRODUCT_PRICE))));
		P.setQuantity((getIntent().getStringExtra(MAGEKEY_PRODUCT_QUANTITY)));
		P.setSku((getIntent().getStringExtra(MAGEKEY_PRODUCT_SKU)));
		P.setWeight(Double.valueOf((getIntent().getStringExtra(MAGEKEY_PRODUCT_WEIGHT))));
		P.addCategory((getIntent().getStringExtra(MAGEKEY_PRODUCT_CATEGORIES)));
		
		
		((EditText) findViewById(R.id.product_sku_input_express)).setText(P.getSku());
		((EditText) findViewById(R.id.description_input_express)).setText(P.getDescription());
		((EditText) findViewById(R.id.product_price_input_express)).setText(P.getPrice());
		
	}

	
	
	
}
