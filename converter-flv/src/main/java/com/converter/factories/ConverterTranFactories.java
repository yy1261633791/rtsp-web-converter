package com.converter.factories;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.AsyncContext;

import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;
import org.bytedeco.javacv.Frame;

import com.alibaba.fastjson.util.IOUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * javacv转码<br/>
 * 流来源不是视频H264格式,音频AAC格式 转码为视频H264格式,音频AAC格式
 * 
 * @author lizhiyong
 * @date 下午5:02:22
 */
@Slf4j
public class ConverterTranFactories extends Thread implements Converter {
	public volatile boolean runing = true;
	/**
	 * 读流器
	 */
	private FFmpegFrameGrabber grabber;
	/**
	 * 转码器
	 */
	private FFmpegFrameRecorder recorder;
	/**
	 * 转FLV格式的头信息<br/>
	 * 如果有第二个客户端播放首先要返回头信息
	 */
	private byte[] headers;
	/**
	 * 保存转换好的流
	 */
	private ByteArrayOutputStream stream;
	/**
	 * 流地址，h264,aac
	 */
	private String url;
	/**
	 * 流输出
	 */
	private List<AsyncContext> outEntitys;

	/**
	 * key用于表示这个转换器
	 */
	private String key;

	/**
	 * 转换队列
	 */
	private Map<String, Converter> factories;

	public ConverterTranFactories(String url, String key, Map<String, Converter> factories,
			List<AsyncContext> outEntitys, FFmpegFrameGrabber grabber) {
		this.url = url;
		this.key = key;
		this.factories = factories;
		this.outEntitys = outEntitys;
		this.grabber = grabber;
	}

	@Override
	public void run() {
		try {
			log.info("this url:{} converterTranFactories start", url);
			grabber.setFrameRate(25);
			if (grabber.getImageWidth() > 1920) {
				grabber.setImageWidth(1920);
			}
			if (grabber.getImageHeight() > 1080) {
				grabber.setImageHeight(1080);
			}
			stream = new ByteArrayOutputStream();
			recorder = new FFmpegFrameRecorder(stream, grabber.getImageWidth(), grabber.getImageWidth(),
					grabber.getAudioChannels());
			recorder.setInterleaved(true);
			recorder.setVideoOption("preset", "ultrafast");
			recorder.setVideoOption("tune", "zerolatency");
			recorder.setVideoOption("crf", "25");
			recorder.setGopSize(50);
			recorder.setFrameRate(25);
			recorder.setSampleRate(grabber.getSampleRate());
			if (grabber.getAudioChannels() > 0) {
				recorder.setAudioChannels(grabber.getAudioChannels());
				recorder.setAudioBitrate(grabber.getAudioBitrate());
				recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC);
			}
			recorder.setFormat("flv");
			recorder.setVideoBitrate(grabber.getVideoBitrate());
			recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264);
			recorder.start();
			if (headers == null) {
				headers = stream.toByteArray();
				stream.reset();
				writeResponse(headers);
			}
			while (runing) {
				// 抓取一帧
				Frame f = grabber.grab();
				if (f != null) {
					try {
						// 转码
						recorder.record(f);
					} catch (Exception e) {
					}
					if (stream.size() > 0) {
						byte[] b = stream.toByteArray();
						stream.reset();
						writeResponse(b);
						if (outEntitys.isEmpty()) {
							log.info("没有输出退出");
							break;
						}
					}
				}
				Thread.sleep(5);
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			closeConverter();
			completeResponse();
			log.info("this url:{} converterTranFactories exit", url);
			factories.remove(this.key);
		}
	}

	/**
	 * 输出FLV视频流
	 * 
	 * @param b
	 */
	public void writeResponse(byte[] b) {
		Iterator<AsyncContext> it = outEntitys.iterator();
		while (it.hasNext()) {
			AsyncContext o = it.next();
			try {
				o.getResponse().getOutputStream().write(b);
			} catch (Exception e) {
				log.info("移除一个输出");
				it.remove();
			}
		}
	}

	/**
	 * 退出转换
	 */
	public void closeConverter() {
		IOUtils.close(grabber);
		IOUtils.close(recorder);
		IOUtils.close(stream);
	}

	/**
	 * 关闭异步响应
	 */
	public void completeResponse() {
		Iterator<AsyncContext> it = outEntitys.iterator();
		while (it.hasNext()) {
			AsyncContext o = it.next();
			o.complete();
		}
	}

	@Override
	public String getKey() {
		return this.key;
	}

	@Override
	public String getUrl() {
		return this.url;
	}

	@Override
	public void addOutputStreamEntity(String key, AsyncContext entity) throws IOException {
		if (headers == null) {
			outEntitys.add(entity);
		} else {
			entity.getResponse().getOutputStream().write(headers);
			entity.getResponse().getOutputStream().flush();
			outEntitys.add(entity);
		}
	}

	@Override
	public void exit() {
		this.runing = false;
		try {
			this.join();
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

}
