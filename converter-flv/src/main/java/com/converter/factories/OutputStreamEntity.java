package com.converter.factories;

import java.io.ByteArrayOutputStream;

import lombok.Data;

/**
 * 用于输出视频流
 * 
 * @author lizhiyong
 * @date 上午10:43:00
 */
@Data
public class OutputStreamEntity {

	public OutputStreamEntity(ByteArrayOutputStream output, long updateTime, String key) {
		super();
		this.output = output;
		this.updateTime = updateTime;
		this.key = key;
	}

	private ByteArrayOutputStream output;
	private long updateTime;
	private String key;
}
