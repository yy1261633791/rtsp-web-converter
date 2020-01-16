package com.converter.service.impl;

import java.io.ByteArrayOutputStream;
import java.util.UUID;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.converter.factories.Converter;
import com.converter.factories.OutputStreamEntity;
import com.converter.registration.ConverterRegistration;
import com.converter.service.IFLVService;

import lombok.extern.slf4j.Slf4j;

/**
 * FLV流转换
 * 
 * @author lizhiyong
 * @date 下午5:06:46
 */
@Slf4j
@Service
public class FLVService implements IFLVService {
	@Autowired
	private ConverterRegistration registration;

	@Override
	public void open(String url, HttpServletResponse response) {
		Converter c = registration.open(url);
		String key = UUID.randomUUID().toString();
		OutputStreamEntity outEntity = new OutputStreamEntity(new ByteArrayOutputStream(), System.currentTimeMillis(),
				key);
		c.addOutputStreamEntity(key, outEntity);
		response.setContentType("video/x-flv");
		response.setHeader("Connection", "keep-alive");
		response.setStatus(HttpServletResponse.SC_OK);
		try {
			response.flushBuffer();
			readFlvStream(c, outEntity, response);
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			c.removeOutputStreamEntity(outEntity.getKey());
		}
	}

	/**
	 * 递归读取转换好的视频流
	 * 
	 * @param c
	 * @param outEntity
	 * @param response
	 * @throws Exception
	 */
	public void readFlvStream(Converter c, OutputStreamEntity outEntity, HttpServletResponse response)
			throws Exception {
		switch (c.getConverterState()) {
		case INITIAL:
			Thread.sleep(300);
			readFlvStream(c, outEntity, response);
			break;
		case OPEN:
			Thread.sleep(400);
			readFlvStream(c, outEntity, response);
			break;
		case RUN:
			if (outEntity.getOutput().size() > 0) {
				byte[] b = outEntity.getOutput().toByteArray();
				outEntity.getOutput().reset();
				response.getOutputStream().write(b);
				outEntity.setUpdateTime(System.currentTimeMillis());
			}
			c.setUpdateTime(System.currentTimeMillis());
			Thread.sleep(200);
			readFlvStream(c, outEntity, response);
			break;
		case CLOSE:
			log.info("close");
			break;
		default:
			break;
		}
	}

}
