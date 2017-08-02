package com.hdu.wsn.uDiskReader.db.bean;

import com.hdu.wsn.uDiskReader.db.BaseRecord;
import com.orm.dsl.Column;
import com.orm.dsl.Table;

/**
 * Created by ASUS on 2017/7/27 0027.
 */
@Table(name = "app_info")
public class AppInfo extends BaseRecord{
    @Column(name = "wrongPass")
    private Integer wrongPass;

    @Column(name="rootUriPath")
    private String rootUriPath;

    public Integer getWrongPass() {
        return wrongPass;
    }

    public void setWrongPass(Integer wrongPass) {
        this.wrongPass = wrongPass;
    }

    public String getRootUriPath() {
        return rootUriPath;
    }

    public void setRootUriPath(String rootUriPath) {
        this.rootUriPath = rootUriPath;
    }
}


