package com.selfdrivingboat.boatcontroller;


import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Created by WGH on 2017/4/10.
 */

public class MyBluetoothDeviceAdapter extends RecyclerView.Adapter<com.selfdrivingboat.boatcontroller.MyBluetoothDeviceAdapter.ViewHolder> {

    private Context mContext;
    private List<BluetoothDevice> mBluetoothDeviceList;
    private ArrayList<Integer> mDrawableList = new ArrayList<>();
    private OnBluetoothDeviceClickedListener mBluetoothClickListener;
    private int mRandomInt;

    public MyBluetoothDeviceAdapter(List<BluetoothDevice> bluetoothDeviceList, OnBluetoothDeviceClickedListener bluetoothClickListener) {
        mBluetoothDeviceList = bluetoothDeviceList;
        mBluetoothClickListener = bluetoothClickListener;
        initDrawableList();
    }

    private void initDrawableList() {
        if (mDrawableList != null && mDrawableList.size() != 0) {
            mDrawableList.clear();
        }

        mDrawableList.add(R.drawable.bluetoothf);
        Random mRandom = new Random();
        mRandomInt = mRandom.nextInt(mDrawableList.size());
    }

    @Override
    public com.selfdrivingboat.boatcontroller.MyBluetoothDeviceAdapter.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (mContext == null) {
            mContext = parent.getContext();
        }
        View view = LayoutInflater.from(mContext).inflate(R.layout.bluetoothdevice_item, parent, false);
        final ViewHolder holder = new ViewHolder(view);



        holder.cardView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (holder.getAdapterPosition() < 0) {
                    Log.e("MyBluetoothDeviceAd", "holder.getAdapterPosition() : " + holder.getAdapterPosition());
                    return;
                }
                final BluetoothDevice device = mBluetoothDeviceList.get(holder.getAdapterPosition());
                if (device == null) {
                    return;
                }
                //mBluetoothScan.stopScan();

                mBluetoothClickListener.onBluetoothDeviceClicked(device.getName(), device.getAddress());

            }
        });

        holder.cardView.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Log.e("MyBluetoothDeviceAd","LongClick :　" + holder.getAdapterPosition());
                return true;
            }
        });

        return holder;
    }

    @Override
    public void onBindViewHolder(com.selfdrivingboat.boatcontroller.MyBluetoothDeviceAdapter.ViewHolder holder, int position) {
        BluetoothDevice device = mBluetoothDeviceList.get(position);
        if (TextUtils.isEmpty(device.getName())) {
            holder.deviceName.setText("device unknown");
        } else {
            holder.deviceName.setText(device.getName());
        }

        holder.deviceImage.setImageResource(mDrawableList.get(((position + mRandomInt) %
                mDrawableList.size() + mDrawableList.size()) % mDrawableList.size()));

        ViewGroup.LayoutParams layoutParams = holder.deviceImage.getLayoutParams();
        holder.deviceImage.setLayoutParams(layoutParams);
    }

    @Override
    public int getItemCount() {
        return mBluetoothDeviceList.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        CardView cardView;
        ImageView deviceImage;
        TextView deviceName;

        ViewHolder(View view) {
            super(view);
            cardView = (CardView) view;
            deviceImage = (ImageView) view.findViewById(R.id.device_image);
            deviceName = (TextView) view.findViewById(R.id.device_name);
        }
    }
}
