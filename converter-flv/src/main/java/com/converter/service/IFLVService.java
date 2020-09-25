package com.converter.service;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public interface IFLVService {

	/**
	 * 打开一个流地址
	 * 
	 * @param url
	 * @param response
	 */
	public void open(String url, HttpServletResponse response, HttpServletRequest request);
	/**
	 * 流地址加密
	 * @param url
	 * @return
	 */
	public String encode(String url);
	
	/**
	 * 流地址解密
	 * @param url
	 * @return
	 */
	public String decode(String url);

}
