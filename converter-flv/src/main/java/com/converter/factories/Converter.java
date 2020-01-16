package com.converter.factories;

import java.util.Map;

import com.converter.factories.state.ConverterState;

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
	 * 获取转换的状态
	 * 
	 * @return
	 */
	public ConverterState getConverterState();

	/**
	 * 添加一个流输出
	 * 
	 * @param entity
	 */
	public void addOutputStreamEntity(String key, OutputStreamEntity entity);

	/**
	 * 所有流输出
	 * 
	 * @return
	 */
	public Map<String, OutputStreamEntity> allOutEntity();

	/**
	 * 移除一个流输出
	 * 
	 * @param key
	 */
	public void removeOutputStreamEntity(String key);

	/**
	 * 设置修改时间
	 * 
	 * @param updateTime
	 */
	public void setUpdateTime(long updateTime);

	/**
	 * 获取修改时间
	 * 
	 * @return
	 */
	public long getUpdateTime();

	/**
	 * 退出转换
	 */
	public void exit();

	/**
	 * 启动
	 */
	public void start();

	/**
	 * 获取输出的流
	 * 
	 * @param key
	 * @return
	 */
	public OutputStreamEntity getOutputStream(String key);
}
