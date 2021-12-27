package com.converter.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import com.converter.service.IFLVService;

import io.swagger.annotations.Api;

/**
 * FLV流转换
 * 
 * @author lizhiyong
 * @date 下午5:06:25
 */
@Api(tags = "flv")
@RestController
public class FLVController {

	@Autowired
	private IFLVService service;

	/**
	 * 打开一个流转换
	 * 
	 * @param url      base64加密后的流地址
	 * @param response
	 * @param request
	 */
	@GetMapping(value = "/live/{url}/live.flv")
	public void open2(@PathVariable(value = "url") String url, HttpServletResponse response,
			HttpServletRequest request) {
		String realUrl = service.decode(url);
		service.open(realUrl, response, request);
	}

	@GetMapping(value = "/api/open")
	public void open(String url, HttpServletResponse response, HttpServletRequest request) {
		String realUrl = service.decode(url);
		service.open(realUrl, response, request);
	}

	/**
	 * 加密地址
	 * 
	 * @param url
	 * @return
	 */
	@PostMapping(value = "/url/encode")
	public String encode(String url) {
		return service.encode(url);
	}

	/**
	 * 加密地址
	 * 
	 * @param url
	 * @return
	 */
	@PostMapping(value = "/url/decode")
	public String decode(String urlHex) {
		return service.decode(urlHex);
	}

}
