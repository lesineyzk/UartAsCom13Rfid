package com.example.uart;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.TextView;

import java.util.List;

public class ListAdapter extends BaseAdapter {
    private Context context;
    private List<Label> list;


    public ListAdapter(Context mcontext, List<Label> list) {
        context = mcontext;
        this.list = list;

    }
    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int position) {
        return list.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        try {
            final ViewHolder holder;
            if (convertView == null) {
                holder = new ViewHolder();
                convertView = LayoutInflater.from(context).inflate(R.layout.list_item, null);
                holder.textView = (TextView) convertView.findViewById(R.id.textitem);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            if (list != null && !list.isEmpty()) {
                boolean check = list.get(position).getCheck();
                if (check) {
                    holder.textView.setTextColor(holder.textView.getResources().getColor(R.color.red));
                } else {
                    holder.textView.setTextColor(holder.textView.getResources().getColor(R.color.black));
                }
                Log.e("TAG", "getView: " + check );
                holder.textView.setText(list.get(position).getData());
            }
        }catch (Exception e){

        }

        return convertView;
    }

    private class ViewHolder {
       CheckBox checkBox;
        TextView textView ;
    }
}
