package com.converter.factories.output;

import com.alibaba.fastjson.util.IOUtils;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileOutputStream;

/**
 * 字节保存为文件，如果文件存在就追加
 *
 * @author lizhiyong
 * @date 下午3:17:46
 */
@Slf4j
public class FLVOutputFile implements Runnable {

    private byte[] b;
    private String fileName;
    private FileOutputStream out;

    public FLVOutputFile(byte[] b, String fileName) {
        this.b = b;
        this.fileName = fileName;
    }

    @Override
    public void run() {
        File file = new File(fileName);
        try {
            if (file.exists()) {
                // 文件追加保存
                out = new FileOutputStream(fileName, true);
            } else {
                file.createNewFile();// 如果文件不存在，就创建该文件
                out = new FileOutputStream(file);// 首次写入获取
            }
            out.write(b);
            out.flush();
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        } finally {
            IOUtils.close(out);
        }
    }

}
