package com.hdu.wsn.uDiskReader.ui.view;

import android.support.v4.provider.DocumentFile;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.hdu.wsn.uDiskReader.R;
import com.hdu.wsn.uDiskReader.usb.file.FileUtil;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by 杨健 on 2017/5/10.
 */

public class DocumentFileAdapter extends RecyclerView.Adapter<DocumentFileAdapter.ViewHolder> implements View.OnLongClickListener, View.OnClickListener {
    // 文件列表
    private List<DocumentFile> data;
    private OnRecyclerViewItemClickListener onRecyclerViewItemClickListener = null;
    // 是否以私密空间item打头
    private boolean secretHeader;
    private int checkBoxVisibility;

    Map<Integer, Boolean> checkMap;

    public DocumentFileAdapter(List<DocumentFile> data) {
        this.data = data;
        checkBoxVisibility = ViewHolder.CHECK_INVISIBILITY;
        checkMap = new HashMap<>();
    }

    public Map<Integer, Boolean> getCheckMap() {
        return checkMap;
    }

    /**
     * 页面刷新
     */
    private void viewRefresh() {
        checkMap = new HashMap<>();
        notifyDataSetChanged();
    }

    /**
     *
     * @param checkBoxVisibility
     */
    public void changeCheckBoxVisibility(int checkBoxVisibility) {
        if (this.checkBoxVisibility == checkBoxVisibility) {
            return;
        }
        checkMap = new HashMap<>();
        this.checkBoxVisibility = checkBoxVisibility;
        viewRefresh();
    }

    /**
     *
     * @param secretHeader
     */
    public void setSecretHeader(boolean secretHeader) {
        this.secretHeader = secretHeader;
        if (secretHeader) {
            data.add(null);
        }
    }

    /**
     *
     * @param position
     */
    public void removeData(int position) {
        data.remove(position);
        viewRefresh();
    }

    /**
     *
     * @param file
     */
    public void addData(DocumentFile file){
        if (secretHeader) {
            data.add(data.size()-1, file);
        } else {
            data.add(file);
        }
        viewRefresh();
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.file_item_layout, parent, false);
        view.setOnClickListener(this);
        view.setOnLongClickListener(this);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {

        int index = position;
        if (!secretHeader) {
            index++;
        }
        if (index > 0) {
            DocumentFile file = data.get(index-1);
            if (file == null) {
                return;
            }
            String name = file.getName();
            String end = name.substring(name.lastIndexOf(".") + 1, name.length()).toLowerCase();
            if (file.isDirectory()) {
                holder.iv_file.setImageResource(R.drawable.folder);
            } else {
                holder.iv_file.setImageResource(FileUtil.getFileImage(end));
            }
            holder.tv_file.setText(name);
            holder.itemView.setTag(position);

            holder.setCheckBoxVisibility(checkBoxVisibility);

            holder.check_file.setChecked(checkMap.get(position)!=null && checkMap.get(position));

            holder.check_file.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    View view = (View) buttonView.getParent();
                    checkMap.put((int)view.getTag(), isChecked);
                    if (onRecyclerViewItemClickListener != null) {
                        onRecyclerViewItemClickListener.onItemCheck(view, (int) view.getTag(), isChecked);
                    }
                }
            });
        } else {
            holder.iv_file.setImageResource(R.drawable.secret_space);
            holder.tv_file.setText("私密空间");
            holder.itemView.setTag(0);
        }
    }

    @Override
    public int getItemCount() {
        return data != null ? data.size() : 0;
    }

    @Override
    public boolean onLongClick(View v) {
        changeCheckBoxVisibility(ViewHolder.CHECK_VISIBILITY);
        if (onRecyclerViewItemClickListener != null) {
            onRecyclerViewItemClickListener.onItemLongClick(v, (int) v.getTag());
        }
        return true;
    }

    @Override
    public void onClick(View v) {
        if (onRecyclerViewItemClickListener != null) {
            if (checkBoxVisibility == ViewHolder.CHECK_INVISIBILITY) {
                onRecyclerViewItemClickListener.onItemClick(v, (int) v.getTag());
            } else {
//                onRecyclerViewItemClickListener.onItemCheck(v, (int) v.getTag(), viewHolder.check());
            }
        }
    }

    /**
     *
     */
    public static class ViewHolder extends RecyclerView.ViewHolder {
        public static final int CHECK_VISIBILITY = View.VISIBLE;
        public static final int CHECK_INVISIBILITY = View.INVISIBLE;

        private ImageView iv_file;
        private TextView tv_file;
        private CheckBox check_file;

        public ViewHolder(View itemView) {
            super(itemView);
            iv_file = (ImageView) itemView.findViewById(R.id.file_icon);
            tv_file = (TextView) itemView.findViewById(R.id.file_name);
            check_file = (CheckBox) itemView.findViewById(R.id.file_check);
        }

        public void setCheckBoxVisibility(int visibility) {
            check_file.setVisibility(visibility);
        }

        public int getCheckBoxVisibility() {
            return check_file.getVisibility();
        }
        
        public boolean check() {
            boolean b = check_file.isChecked();
            b = !b;
            check_file.setChecked(b);
            return b;
        }

    }

    /**
     *
     */
    public interface OnRecyclerViewItemClickListener {
        /**
         *
         * @param view
         * @param position
         */
        void onItemClick(View view, int position);

        /**
         *
         * @param view
         * @param position
         */
        void onItemLongClick(View view, int position);

        /**
         *
         * @param view
         * @param position
         * @param check
         */
        void onItemCheck(View view, int position, boolean check);
    }

    /**
     *
     * @param onRecyclerViewItemClickListener
     */
    public void setOnRecyclerViewItemClickListener(OnRecyclerViewItemClickListener onRecyclerViewItemClickListener) {
        this.onRecyclerViewItemClickListener = onRecyclerViewItemClickListener;
    }

}
