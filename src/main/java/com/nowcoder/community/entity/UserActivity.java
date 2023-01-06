package com.nowcoder.community.entity;

import java.util.Date;

public class UserActivity {
    private int id;
    private Date from;
    private Date to;
    private String bitmap;
    private Date createTime;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public Date getFrom() {
        return from;
    }

    public void setFrom(Date from) {
        this.from = from;
    }

    public Date getTo() {
        return to;
    }

    public void setTo(Date to) {
        this.to = to;
    }

    public String getBitmap() {
        return bitmap;
    }

    public void setBitmap(String bitmap) {
        this.bitmap = bitmap;
    }

    public Date getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Date createTime) {
        this.createTime = createTime;
    }

    @Override
    public String toString() {
        return "UserActivity{" +
                "id=" + id +
                ", from=" + from +
                ", to=" + to +
                ", bitmap='" + bitmap + '\'' +
                ", createTime=" + createTime +
                '}';
    }
}
