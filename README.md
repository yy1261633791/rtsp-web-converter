# rtsp-web-converter

转码视频编码为H.264，音频编码为AAC的视频流用于h5播放<br>
---

SpringBoot  
---
Javacv
---

测试页面：http://127.0.0.1:8081/video.html<br>

前端使用flvjs播放<br>

转码接口
---
http://127.0.0.1:8081/api/open?url=base64(视频流地址)<br>
（该接口直接响应flv格式视频流）<br>

示列
---
var flvPlayer = flvjs.createPlayer({type: 'flv',url:'http://127.0.0.1:8081/api/open?url=base64(视频流地址)',isLive: true});
		flvPlayer.attachMediaElement(document.getElementById(id));<br>
		flvPlayer.load();<br>
		flvPlayer.play();<br>
		
性能消耗
---
转码一路rtsp流在i5 7500配置下cpu占用在1%左右
