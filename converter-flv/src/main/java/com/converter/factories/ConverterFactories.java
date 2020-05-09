package com.converter.factories;

import java.io.ByteArrayOutputStream;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bytedeco.ffmpeg.avcodec.AVPacket;
import org.bytedeco.javacv.FFmpegFrameGrabber;
import org.bytedeco.javacv.FFmpegFrameRecorder;

import com.converter.factories.state.ConverterState;

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
	private Map<String, OutputStreamEntity> outEntitys;
	/**
	 * 当前转换器状态
	 */
	private ConverterState state = ConverterState.INITIAL;
	/**
	 * key用于表示这个转换器
	 */
	private String key;
	/**
	 * 上次更新时间<br/>
	 * 客户端读取是刷新<br/>
	 * 如果没有客户端读取，会在一分钟后销毁这个转换器
	 */
	private long updateTime;
	/**
	 * 转换队列
	 */
	private Map<String, Converter> factories;

	public ConverterFactories(String url, String key, Map<String, Converter> factories) {
		this.url = url;
		this.key = key;
		this.factories = factories;
		this.updateTime = System.currentTimeMillis();
	}

	@Override
	public void run() {
		try {
			grabber = new FFmpegFrameGrabber(url);
			if ("rtsp".equals(url.substring(0, 4))) {
				grabber.setOption("rtsp_transport", "tcp");
				grabber.setOption("stimeout", "500000");
			} else {
				grabber.setOption("timeout", "500000");
			}
			grabber.start();
			stream = new ByteArrayOutputStream();
			outEntitys = new ConcurrentHashMap<>();
			state = ConverterState.OPEN;
			recorder = new FFmpegFrameRecorder(stream, grabber.getImageWidth(), grabber.getImageWidth(),
					grabber.getAudioChannels());
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
			state = ConverterState.RUN;
			if (headers == null) {
				headers = stream.toByteArray();
				stream.reset();
				for (OutputStreamEntity o : outEntitys.values()) {
					o.getOutput().write(headers);
				}
			}
			int errorNum = 0;
			while (runing) {
				AVPacket k = grabber.grabPacket();
				if (k != null) {
					try {
						recorder.recordPacket(k);
					} catch (Exception e) {
					}
					byte[] b = stream.toByteArray();
					stream.reset();
					for (OutputStreamEntity o : outEntitys.values()) {
						if (o.getOutput().size() < (1024 * 1024)) {
							o.getOutput().write(b);
						}
					}
					errorNum = 0;
				} else {
					errorNum++;
					if (errorNum > 500) {
						break;
					}
				}
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
			state = ConverterState.ERROR;
		} finally {
			closeConverter();
			log.info("exit");
			state = ConverterState.CLOSE;
			factories.remove(this.key);
		}
	}

	/**
	 * 退出转换
	 */
	public void closeConverter() {
		try {
			recorder.stop();
			grabber.stop();
			grabber.close();
			recorder.close();
			stream.close();
			for (OutputStreamEntity o : outEntitys.values()) {
				o.getOutput().close();
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
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
	public ConverterState getConverterState() {
		return this.state;
	}

	@Override
	public void addOutputStreamEntity(String key, OutputStreamEntity entity) {
		try {
			switch (this.state) {
			case INITIAL:
				Thread.sleep(100);
				addOutputStreamEntity(key, entity);
				break;
			case OPEN:
				outEntitys.put(key, entity);
				break;
			case RUN:
				entity.getOutput().write(this.headers);
				outEntitys.put(key, entity);
				break;
			default:
				break;
			}
		} catch (Exception e) {
			log.error(e.getMessage(), e);
		}
	}

	@Override
	public void setUpdateTime(long updateTime) {
		this.updateTime = updateTime;
	}

	@Override
	public long getUpdateTime() {
		return this.updateTime;
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

	@Override
	public OutputStreamEntity getOutputStream(String key) {
		if (outEntitys.containsKey(key)) {
			return outEntitys.get(key);
		}
		return null;
	}

	@Override
	public Map<String, OutputStreamEntity> allOutEntity() {
		return this.outEntitys;
	}

	@Override
	public void removeOutputStreamEntity(String key) {
		this.outEntitys.remove(key);
	}
}
