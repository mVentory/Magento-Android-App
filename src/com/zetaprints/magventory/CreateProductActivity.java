package com.zetaprints.magventory;


import java.util.HashMap;

import com.zetaprints.magventory.client.MagentoClient;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

public class CreateProductActivity extends Activity {

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.new_product);
		Button create=(Button) findViewById(R.id.createbutton);
		create.setOnClickListener(buttonlistener);
	}
	
	private OnClickListener buttonlistener = new OnClickListener() {
        public void onClick(View v) {
        if(v.getId()==R.id.createbutton){

        String name= ((TextView)findViewById(R.id.product_name_input)).getText().toString();
        String price= ((TextView)findViewById(R.id.product_price_input)).getText().toString();
        String sku= ((TextView)findViewById(R.id.product_sku_input)).getText().toString();
        MagentoClient magentoClient= new MagentoClient("http://magento.chilerocks.org/index.php/api/xmlrpc/","api-user", "123123");
        Object[] map=(Object[])magentoClient.execute("product_attribute_set.list");
        Log.d("APP","que es "+map[0].getClass().getName());
        Log.d("APP","size "+map.length);
        Log.d("APP","size "+map[0].toString());
        String set_id=(String)((HashMap)map[0]).get("set_id");
        HashMap<String, Object> product_data= new HashMap<String, Object>();
        product_data.put("name", name);
        product_data.put("price", price);
        product_data.put("website","1");
        product_data.put("description","1");
        product_data.put("short_description","1");
        product_data.put("description","1");
        product_data.put("status",1);
        product_data.put("weight",1);
        product_data.put("categories",0);
       
        magentoClient.execute("catalog_product.create",new Object[]{"simple",4,sku,product_data});
        }
  }       	
};

}

/*
$newProductData = array(
	    'name'              => 'name of product',
	     // websites - Array of website ids to which you want to assign a new product
	    'websites'          => array(1), // array(1,2,3,...)
	    'short_description' => 'short description',
	    'description'       => 'description',
	    'status'            => 1,
	    'weight'            => 0,
	    'tax_class_id'      => 1,
	    'categories'    => array(3),    //3 is the category id   
	    'price'             => 12.05
	);
*/