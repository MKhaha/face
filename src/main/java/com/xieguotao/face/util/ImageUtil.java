package com.xieguotao.face.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import javax.imageio.ImageReadParam;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.Arrays;

@Component("imageUtil")
@Slf4j
public class ImageUtil {

    /**
     * <p>Title: cutImage</p>
     * <p>Description:  根据原图与裁切size截取局部图片</p>
     *
     * @param srcImg 源图片
     * @param output 图片输出流
     * @param rect   需要截取部分的坐标和大小
     */
    public void cutImage(File srcImg, OutputStream output, java.awt.Rectangle rect) {
        if (srcImg.exists()) {
            java.io.FileInputStream fis = null;
            ImageInputStream iis = null;
            try {
                fis = new FileInputStream(srcImg);
                // ImageIO 支持的图片类型 : [BMP, bmp, jpg, JPG, wbmp, jpeg, png, PNG, JPEG, WBMP, GIF, gif]
                String types = Arrays.toString(ImageIO.getReaderFormatNames()).replace("]", ",");
                String suffix = null;
                // 获取图片后缀
                if (srcImg.getName().contains(".")) {
                    suffix = srcImg.getName().substring(srcImg.getName().lastIndexOf(".") + 1);
                }
                // 类型和图片后缀全部小写，然后判断后缀是否合法
                if (suffix == null || !types.toLowerCase().contains(suffix.toLowerCase() + ",")) {
                    log.error("Sorry, the image suffix is illegal. the standard image suffix is {}." + types);
                    return;
                }
                // 将FileInputStream 转换为ImageInputStream
                iis = ImageIO.createImageInputStream(fis);
                // 根据图片类型获取该种类型的ImageReader
                ImageReader reader = ImageIO.getImageReadersBySuffix(suffix).next();
                reader.setInput(iis, true);
                ImageReadParam param = reader.getDefaultReadParam();
                param.setSourceRegion(rect);
                BufferedImage bi = reader.read(0, param);
                ImageIO.write(bi, suffix, output);
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fis != null) fis.close();
                    if (iis != null) iis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else {
            log.warn("the src image is not exist.");
        }
    }

    private String cutImage(File srcImg, String destImgPath, java.awt.Rectangle rect) {
        try {
            cutImage(srcImg, new java.io.FileOutputStream(destImgPath), rect);
            return destImgPath;
        } catch (FileNotFoundException e) {
            log.warn("the dest image is not exist.");
            return null;
        }
    }

    public String cutImage(String srcImg, String destImg, int x, int y, int width, int height) {
        return cutImage(new File(srcImg), destImg, new java.awt.Rectangle(x, y, width, height));
    }
}
