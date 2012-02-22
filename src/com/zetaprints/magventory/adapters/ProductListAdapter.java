package com.zetaprints.magventory.adapters;


import java.util.ArrayList;

import com.zetaprints.magventory.R;
import com.zetaprints.magventory.model.Product;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class ProductListAdapter extends ArrayAdapter<Product> {

        private ArrayList<Product> items;

        public ProductListAdapter(Context context, int textViewResourceId, ArrayList<Product> items) {
                super(context, textViewResourceId, items);
                this.items = items;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
                View v = convertView;
                if (v == null) {
                    LayoutInflater vi = (LayoutInflater)getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    v = vi.inflate(R.layout.item, null);
                }
                Product o = items.get(position);
                if (o != null) {
                        TextView tt = (TextView) v.findViewById(R.id.nombre);
                        TextView tt2 = (TextView) v.findViewById(R.id.description);
                        //TextView bt = (TextView) v.findViewById(R.id.bottomtext);
                        if (tt != null) {
                              tt.setText(o.getName());    
                              tt2.setText(o.getDescription());
                              }
                        //if(bt != null){
                        //      bt.setText("Status: "+ o.getOrderStatus());
                        //}
                }
                return v;
        }
}

