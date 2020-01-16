package com.converter.controller;

import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import com.converter.service.IFLVService;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;

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

	@ApiOperation(value = "转换流并输出flv格式的视频流")
	@ApiImplicitParam(value = "流地址", name = "url", required = true)
	@GetMapping(value = "/api/open")
	public void open(String url, HttpServletResponse response) {
		service.open(url, response);
	}

}
