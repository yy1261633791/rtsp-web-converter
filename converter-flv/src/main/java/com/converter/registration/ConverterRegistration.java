package com.converter.registration;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Resource;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.support.CronTrigger;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import com.converter.factories.Converter;
import com.converter.factories.ConverterFactories;
import com.converter.factories.OutputStreamEntity;

import lombok.extern.slf4j.Slf4j;

/**
 * FLV流转换
 * 
 * @author lizhiyong
 * @date 下午5:10:16
 */
@Slf4j
@Component
public class ConverterRegistration implements ApplicationRunner {

	/**
	 * 转换队列
	 */
	private Map<String, Converter> factories;
	@Resource(name = "taskScheduler")
	private TaskScheduler scheduler;

	/**
	 * 开始一个转换<br/>
	 * 如果已存在这个流的转换就直接返回已存在的转换器
	 * 
	 * @param url
	 * @return
	 */
	public Converter open(String url) {
		Assert.notNull(url, "url不能为空");
		Converter c = isExist(url);
		if (null == c) {
			String key = UUID.randomUUID().toString();
			c = new ConverterFactories(url, key, factories);
			factories.put(key, c);
			c.start();
		}
		return c;
	}

	/**
	 * 获取一个转换
	 * 
	 * @param key
	 * @return
	 */
	public Converter getConverter(String key) {
		if (factories.containsKey(key)) {
			return factories.get(key);
		}
		return null;
	}

	/**
	 * 停止一个流转换
	 * 
	 * @param key
	 */
	public void stopConverter(String key) {
		if (factories.containsKey(key)) {
			Converter c = factories.get(key);
			c.exit();
			factories.remove(key);
		}
	}

	/**
	 * 如果流已存在，就共用一个
	 * 
	 * @param url
	 * @return
	 */
	public Converter isExist(String url) {
		for (Converter c : factories.values()) {
			if (url.equals(c.getUrl())) {
				return c;
			}
		}
		return null;
	}

	/**
	 * springboot启动完成后执行
	 */
	@Override
	public void run(ApplicationArguments args) throws Exception {
		log.info("FLV Converter init");
		factories = new ConcurrentHashMap<>();
		// 每分钟清理一次无效的转换器和流输出
		CronTrigger cron = new CronTrigger("0 0/1 * * * ?");
		scheduler.schedule(new Runnable() {

			@Override
			public void run() {
				Iterator<Entry<String, Converter>> ct = factories.entrySet().iterator();
				while (ct.hasNext()) {
					Entry<String, Converter> en = ct.next();
					Converter c = en.getValue();
					long second = (System.currentTimeMillis() - c.getUpdateTime()) / 1000;
					if ((second / 60) > 1) {
						// 这个转换器上次更新时间大于1分
						// 关闭转换
						c.exit();
						ct.remove();
						log.info("remove converter");
						continue;
					}
					Map<String, OutputStreamEntity> o = c.allOutEntity();
					Iterator<Entry<String, OutputStreamEntity>> it = o.entrySet().iterator();
					while (it.hasNext()) {
						Entry<String, OutputStreamEntity> n = it.next();
						long sec = (System.currentTimeMillis() - n.getValue().getUpdateTime()) / 1000;
						if (sec > 30) {
							// 这个流输出上次更新时间大于30秒
							// 关闭输出
							it.remove();
							log.info("remove outEntity");
							continue;
						}
					}
				}
			}
		}, cron);
	}

}
