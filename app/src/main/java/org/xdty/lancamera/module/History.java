package org.xdty.lancamera.module;

import com.google.gson.annotations.SerializedName;

public class History {

    private String name;
    private Type type;
    private String mtime;
    private long size;
    private String path;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public String getTime() {
        return mtime;
    }

    public void setTime(String mtime) {
        this.mtime = mtime;
    }

    public long getSize() {
        return size;
    }

    public void setSize(long size) {
        this.size = size;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public enum Type {
        @SerializedName("directory")
        DIRECTORY,
        @SerializedName("file")
        FILE
    }
}
