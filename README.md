# rtsp-web-converter

转码RTSP，RTMP或其他格式的流用于h5播放<br>
成品下载：http://119.84.133.13:8090/converter-flv-2.0.5.RELEASE.jar
---

SpringBoot  
---
Javacv
---

测试页面：http://127.0.0.1:8081/video.html<br>

前端使用flvjs播放<br>

转码接口
---
http://127.0.0.1:8081/live/{url}/live.flv     url=base64(视频流地址)<br>
（该接口直接响应flv格式视频流）<br>

示列
---
var flvPlayer = flvjs.createPlayer({type: 'flv',url:'http://127.0.0.1:8081/live/{base64加密后的流地址}/live.flv',isLive: true},{isLive: true,enableStashBuffer: false,enableWorker: true,stashInitialSize: 128,videoStateMonitorInterval: 1000,decreaseDurationStep: 1,enableDurationMonitor: true,enableVideoFrozenMonitor: true});
		flvPlayer.attachMediaElement(document.getElementById(id));<br>
		flvPlayer.load();<br>
		flvPlayer.play();<br>
		
性能消耗
---
对于转包装方式一路流在i5 7500配置下cpu占用在1%左右

2020-07-17<br>
更新了转码的方法，现在对输入的流没有任何要求了，对于符合H264,AAC的流会使用低性能消耗的转包装方式进行，其他格式会先进行转码操作
