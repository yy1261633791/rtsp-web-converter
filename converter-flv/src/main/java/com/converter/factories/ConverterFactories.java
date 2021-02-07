package com.converter.factories;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.servlet.AsyncContext;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.ffmpeg.global.avcodec;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import com.alibaba.fastjson.util.IOUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * javacv转包装<br/>
 * 无须转码，更低的资源消耗，更低的延迟<br/>
 * 确保流来源视频H264格式,音频AAC格式
 * 
 * @author lizhiyong
 * @date 下午5:02:22
 */
@Slf4j
public class ConverterFactories extends Thread implements Converter {
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

	public ConverterFactories(String url, String key, Map<String, Converter> factories, List<AsyncContext> outEntitys) {
		this.url = url;
		this.key = key;
		this.factories = factories;
		this.outEntitys = outEntitys;
	}

	@Override
	public void run() {
		boolean isCloseGrabberAndResponse = true;
		try {
			grabber = new FFmpegFrameGrabber(url);
			if ("rtsp".equals(url.substring(0, 4))) {
				grabber.setOption("rtsp_transport", "tcp");
				grabber.setOption("stimeout", "5000000");
			}
			grabber.start();
			if (avcodec.AV_CODEC_ID_H264 == grabber.getVideoCodec()
					&& (grabber.getAudioChannels() == 0 || avcodec.AV_CODEC_ID_AAC == grabber.getAudioCodec())) {
				log.info("this url:{} converterFactories start", url);
				// 来源视频H264格式,音频AAC格式
				// 无须转码，更低的资源消耗，更低的延迟
				stream = new ByteArrayOutputStream();
				recorder = new FFmpegFrameRecorder(stream, grabber.getImageWidth(), grabber.getImageHeight(),
						grabber.getAudioChannels());
				recorder.setInterleaved(true);
				recorder.setVideoOption("preset", "ultrafast");
				recorder.setVideoOption("tune", "zerolatency");
				recorder.setVideoOption("crf", "25");
				recorder.setFrameRate(grabber.getFrameRate());
				recorder.setSampleRate(grabber.getSampleRate());
				if (grabber.getAudioChannels() > 0) {
					recorder.setAudioChannels(grabber.getAudioChannels());
					recorder.setAudioBitrate(grabber.getAudioBitrate());
					recorder.setAudioCodec(grabber.getAudioCodec());
				}
				recorder.setFormat("flv");
				recorder.setVideoBitrate(grabber.getVideoBitrate());
				recorder.setVideoCodec(grabber.getVideoCodec());
				recorder.start(grabber.getFormatContext());
				if (headers == null) {
					headers = stream.toByteArray();
					stream.reset();
					writeResponse(headers);
				}
				int nullNumber = 0;
				while (runing) {
					AVPacket k = grabber.grabPacket();
					if (k != null) {
						try {
							recorder.recordPacket(k);
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
						avcodec.av_packet_unref(k);
					} else {
						nullNumber++;
						if (nullNumber > 200) {
							break;
						}
					}
					Thread.sleep(5);
				}
			} else {
				isCloseGrabberAndResponse = false;
				// 需要转码为视频H264格式,音频AAC格式
				ConverterTranFactories c = new ConverterTranFactories(url, key, factories, outEntitys, grabber);
				factories.put(key, c);
				c.start();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		} finally {
			closeConverter(isCloseGrabberAndResponse);
			completeResponse(isCloseGrabberAndResponse);
			log.info("this url:{} converterFactories exit", url);

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
	public void closeConverter(boolean isCloseGrabberAndResponse) {
		if (isCloseGrabberAndResponse) {
			IOUtils.close(grabber);
			factories.remove(this.key);
		}
		IOUtils.close(recorder);
		IOUtils.close(stream);
	}

	/**
	 * 关闭异步响应
	 * 
	 * @param isCloseGrabberAndResponse
	 */
	public void completeResponse(boolean isCloseGrabberAndResponse) {
		if (isCloseGrabberAndResponse) {
			Iterator<AsyncContext> it = outEntitys.iterator();
			while (it.hasNext()) {
				AsyncContext o = it.next();
				o.complete();
			}
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
