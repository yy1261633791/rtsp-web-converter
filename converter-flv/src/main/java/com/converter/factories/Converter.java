package com.converter.factories;

import javax.servlet.AsyncContext;
import java.io.IOException;

public interface Converter {

    /**
     * 获取该转换的key
     */
    public String getKey();

    /**
     * 获取该转换的url
     *
     * @return
     */
    public String getUrl();

    /**
     * 添加一个流输出
     *
     * @param entity
     */
    public void addOutputStreamEntity(String key, AsyncContext entity) throws IOException;

    /**
     * 退出转换
     */
    public void exit();

    /**
     * 启动
     */
    public void start();

}
