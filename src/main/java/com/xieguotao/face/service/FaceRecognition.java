package com.xieguotao.face.service;

import com.arcsoft.face.*;
import com.arcsoft.face.enums.DetectMode;
import com.arcsoft.face.enums.DetectOrient;
import com.arcsoft.face.enums.ErrorInfo;
import com.arcsoft.face.enums.ImageFormat;
import com.arcsoft.face.toolkit.ImageInfo;
import com.xieguotao.face.dao.FaceResult;
import com.xieguotao.face.dao.ImportPersonUrls;
import com.xieguotao.face.dao.MyFaceInfo;
import com.xieguotao.face.util.ImageUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

import static com.arcsoft.face.toolkit.ImageFactory.getGrayData;
import static com.arcsoft.face.toolkit.ImageFactory.getRGBData;

@Service("faceRecognition")
@Slf4j
public class FaceRecognition implements InitializingBean, DisposableBean {

    // 对外静态文件地址前缀
    private String addressAndPort = "http://172.16.0.212:8083/static/";
    private String faceLocal = System.getenv("FACELOCAL");
    // 本地存储对外文件地址前缀
    private String localFilePrefix = faceLocal + "static/";

    private volatile FaceEngine gFaceEngine = null;
    private FaceResult faceResult = new FaceResult();
    private String facePhotoPath = localFilePrefix + "facePhoto/";
    private String facePhotoUrlPrefix = addressAndPort + "facePhoto/";
    private String gOriginalPhoto = localFilePrefix + "original/";
    private String originalPhotoUrlPrefix = addressAndPort + "original/";
    // 原图片抓拍路径
    private String oriPath = faceLocal + "test/";

    // 本地重点人员图片存储地址
    private String importantPersonLocal = localFilePrefix + "importantPerson/";
    // 重点人员图片url前缀
    private String importantPersonUrlPrefix = addressAndPort + "importantPerson/";
    // 保持重点人员图片url
    private List<String> importantPersonPhotoUrls = new ArrayList<>();
    // 保存重点人员图片特征信息
    private List<FaceFeature> importantPersonFaceFeature = new ArrayList<>();

    @Autowired
    private ImageUtil imageUtil;

    /**
     * 从原图片中切出小图，存储，并返回图片相对地址；
     *
     * @return
     */
    private String setFacePhoto(
            FaceInfo faceInfo,
            int i,
            String originPhoto,
            String localPath,
            String urlPath) {

        if (faceInfo == null) {
            return null;
        }

        String originalFileName = (new File(originPhoto)).getName();

        String fileName = "cut" + i + originalFileName;

        int left = faceInfo.getRect().left;
        int top = faceInfo.getRect().top;

        imageUtil.cutImage(
                originPhoto,
                localPath + fileName,
                left,
                top,
                faceInfo.getRect().right - left,
                faceInfo.getRect().bottom - top);

        return urlPath + fileName;
    }

    private void copyFile(String src, String des) {
        //先创建字节输入输出流对象，赋值为null
        FileOutputStream fos = null;
        FileInputStream fis = null;

        //在读写时可能会出现异常，使用try捕获处理
        try {
            //根据传入的路径给输入输出流赋值
            fis = new FileInputStream(src);
            fos = new FileOutputStream(des);
            //定义一个变量来记录获得的字节
            int len = -1;
            while ((len = fis.read()) != -1) {
                fos.write(len);
                fos.flush();
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {

            try {
                //读取完后关闭流
                if (fos != null) fos.close();
                if (fis != null) fis.close();

            } catch (Exception e2) {
                e2.printStackTrace();
            }
        }
    }

    /**
     * 设置原图相对地址
     *
     * @return
     */
    private void setOriginalPhoto(String originalPhoto) {
        String fileName = (new File(originalPhoto)).getName();
        String savePath = gOriginalPhoto + fileName;
        copyFile(originalPhoto, savePath);
        faceResult.setOriginalPhotoUrl(originalPhotoUrlPrefix + fileName);
    }

    @Override
    public void destroy() throws Exception {
        if (gFaceEngine != null) {
            // 引擎卸载
            int unInitCode = gFaceEngine.unInit();
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        initFaceEngine();
    }

    public void initFaceEngine() {
        String appId = "4GDCFPjtDcWwcsUxmpA9M2T6xq4bpsUB2dCYQ7H9sgKi";
        String sdkKey = "7rKvRkiwJKhoyVuTxw7gs4WNxz5PxRSTRvdWdkaN6wU5";

        FaceEngine faceEngine = new FaceEngine(faceLocal +  "arcsoft-lib");
        //激活引擎
        int activeCode = faceEngine.activeOnline(appId, sdkKey);

        if (activeCode != ErrorInfo.MOK.getValue() && activeCode != ErrorInfo.MERR_ASF_ALREADY_ACTIVATED.getValue()) {
            System.out.println("引擎激活失败");
            return;
        }

        //引擎配置
        EngineConfiguration engineConfiguration = new EngineConfiguration();
        engineConfiguration.setDetectMode(DetectMode.ASF_DETECT_MODE_IMAGE);
        engineConfiguration.setDetectFaceOrientPriority(DetectOrient.ASF_OP_0_ONLY);

        //功能配置
        FunctionConfiguration functionConfiguration = new FunctionConfiguration();
        functionConfiguration.setSupportAge(true);
        functionConfiguration.setSupportFace3dAngle(true);
        functionConfiguration.setSupportFaceDetect(true);
        functionConfiguration.setSupportFaceRecognition(true);
        functionConfiguration.setSupportGender(true);
        functionConfiguration.setSupportLiveness(true);
        functionConfiguration.setSupportIRLiveness(true);
        engineConfiguration.setFunctionConfiguration(functionConfiguration);


        //初始化引擎
        int initCode = faceEngine.init(engineConfiguration);

        if (initCode != ErrorInfo.MOK.getValue()) {
            System.out.println("初始化引擎失败");
        } else {
            gFaceEngine = faceEngine;
        }

    }

    private void setNoFace() {
        synchronized (faceResult) {
            faceResult.setHaveFace(false);
            faceResult.setOriginalPhotoUrl(null);
            faceResult.setMyFaceInfoList(null);
        }
    }

    private void setMyFaceInfo(
            String imagePath,
            ImageInfo imageInfo,
            List<FaceInfo> faceInfoList,
            List<GenderInfo> genderInfoList,
            int genderCode,
            List<AgeInfo> ageInfoList,
            int ageCode,
            List<Face3DAngle> face3DAngleList,
            int face3dCode,
            List<LivenessInfo> livenessInfoList,
            int livenessCode) {

        synchronized (faceResult) {

            faceResult.setHaveFace(true);
            faceResult.setHaveImportPerson(false);
            setOriginalPhoto(imagePath);

            if (faceResult.getMyFaceInfoList() == null) {
                faceResult.setMyFaceInfoList(new ArrayList<>());
            } else {
                faceResult.getMyFaceInfoList().clear();
            }

            for (int i = 0; i < faceInfoList.size(); i++) {
                System.out.println("打印人脸识别信息：" + "i:" + faceInfoList.get(i));
                MyFaceInfo myFaceInfo = new MyFaceInfo();
                faceResult.getMyFaceInfoList().add(myFaceInfo);
                String url = setFacePhoto(faceInfoList.get(i), i, imagePath, facePhotoPath, facePhotoUrlPrefix);
                myFaceInfo.setFacePhotoUrl(url);
                myFaceInfo.setInfo("");

                if (genderCode == ErrorInfo.MOK.getValue() && ageInfoList.size() != 0) {
                    myFaceInfo.setInfo(
                            myFaceInfo.getInfo() +
                                    "性别：" +
                                    (genderInfoList.get(0).getGender() == 0 ? "男" : "女") +
                                    "\n");
                }

                if (ageCode == ErrorInfo.MOK.getValue() && ageInfoList.size() != 0) {
                    myFaceInfo.setInfo(myFaceInfo.getInfo() + "年龄：" + ageInfoList.get(0).getAge() + "\n");
                }

                if (face3dCode == ErrorInfo.MOK.getValue() && face3DAngleList.size() != 0) {
                    myFaceInfo.setInfo(myFaceInfo.getInfo() +
                            "3D角度：" +
                            Math.round(face3DAngleList.get(0).getPitch()) + "," +
                            Math.round(face3DAngleList.get(0).getRoll()) + "," +
                            Math.round(face3DAngleList.get(0).getYaw()) + "\n");
                }

                if (livenessCode == ErrorInfo.MOK.getValue() && livenessInfoList.size() != 0) {
                    myFaceInfo.setInfo(myFaceInfo.getInfo() + "活体：" +
                            (livenessInfoList.get(0).getLiveness() == 1 ? "是" : "否") +
                            "\n");
                }
            }

            if (importantPersonFaceFeature.size() != 0) {
                synchronized (importantPersonFaceFeature) {
                    if (importantPersonFaceFeature.size() != 0) {
                        log.info("重点人员特征表不为空，开始比对图片中是否有重点人员信息");
                        for (int i = 0; i < faceInfoList.size(); i++) {

                            //特征提取
                            FaceFeature faceFeature = new FaceFeature();
                            int extractCode = gFaceEngine.extractFaceFeature(
                                    imageInfo.getImageData(),
                                    imageInfo.getWidth(),
                                    imageInfo.getHeight(),
                                    ImageFormat.CP_PAF_BGR24,
                                    faceInfoList.get(i),
                                    faceFeature);
                            if (extractCode != ErrorInfo.MOK.getValue()) {
                                log.info("一般人脸特征提取失败");
                                continue;
                            } else {
                                log.info("一般人脸特征值大小" + faceFeature.getFeatureData().length);
                            }

                            //特征比对
                            FaceFeature targetFaceFeature = new FaceFeature();
                            targetFaceFeature.setFeatureData(faceFeature.getFeatureData());
                            FaceFeature sourceFaceFeature = new FaceFeature();

                            for (int j = 0; j < importantPersonFaceFeature.size(); j++) {
                                FaceFeature faceFeatureImportant = importantPersonFaceFeature.get(j);
                                sourceFaceFeature.setFeatureData(faceFeatureImportant.getFeatureData());
                                FaceSimilar faceSimilar = new FaceSimilar();
                                int compareCode = gFaceEngine.compareFaceFeature(targetFaceFeature, sourceFaceFeature, faceSimilar);
                                if (compareCode == ErrorInfo.MOK.getValue()) {
                                    log.info("一般人脸与重点人员相识度大小" + faceSimilar.getScore());
                                    if (faceSimilar.getScore() > 0.9) {
                                        faceResult.setHaveImportPerson(true);
                                        faceResult.getMyFaceInfoList().get(i).setSimilarImportPersonUrl(importantPersonPhotoUrls.get(j));
                                        faceResult.getMyFaceInfoList().get(i).setSimilarScore(Math.round(faceSimilar.getScore() * 100));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    private void clearImportantPersonFaceFeature() {
        synchronized (importantPersonFaceFeature) {
            importantPersonPhotoUrls.clear();
            importantPersonFaceFeature.clear();
        }
    }

    /**
     * 每20秒更新一次重要人员图片信息
     */
    @Scheduled(fixedRate = 4000)
    public void getImportantPersonFaceFeature() {

        log.info("getImportantPersonFaceFeature：执行任务");

        // 获取重要人员图片文件
        File importPersonPath = new File(importantPersonLocal);
        if (!importPersonPath.exists()) {
            log.info("重要人员文件夹不存在，创建文件夹");
            importPersonPath.mkdirs();
            clearImportantPersonFaceFeature();
            return;
        }
        File[] validFile = importPersonPath.listFiles(pathname -> !pathname.isDirectory() && pathname.canRead());
        if (validFile == null || validFile.length == 0) {
            log.info("没有重点人员文件");
            clearImportantPersonFaceFeature();
            return;
        }

        // 保持重点人员图片url
        List<String> tempImportantPersonPhotoUrls = new ArrayList<>();
        // 保存重点人员图片特征信息
        List<FaceFeature> tempImportantPersonFaceFeature = new ArrayList<>();


        for (File importantPerson : validFile) {

            //人脸检测2
            ImageInfo imageInfo2;
            try {
                imageInfo2 = getRGBData(importantPerson);
                if (imageInfo2 == null) {
                    log.info("重点人员文件中获取图片信息失败，请确认是否为图片文件");
                    continue;
                }
            } catch (NullPointerException e) {
                continue;
            }
            List<FaceInfo> faceInfoList2 = new ArrayList<FaceInfo>();
            int detectCode2 = gFaceEngine.detectFaces(
                    imageInfo2.getImageData(),
                    imageInfo2.getWidth(),
                    imageInfo2.getHeight(),
                    ImageFormat.CP_PAF_BGR24,
                    faceInfoList2);
            if (detectCode2 != ErrorInfo.MOK.getValue()) {
                log.info("重点人员图片中检测人脸信息失败");
                continue;
            }

            //特征提取2
            FaceFeature faceFeature2 = new FaceFeature();
            int extractCode2 = gFaceEngine.extractFaceFeature(
                    imageInfo2.getImageData(),
                    imageInfo2.getWidth(),
                    imageInfo2.getHeight(),
                    ImageFormat.CP_PAF_BGR24,
                    faceInfoList2.get(0),
                    faceFeature2);
            if (extractCode2 != ErrorInfo.MOK.getValue()) {
                log.info("提取重点人员图片特征信息失败");
                continue;
            }
            tempImportantPersonPhotoUrls.add(importantPersonUrlPrefix + importantPerson.getName());
            tempImportantPersonFaceFeature.add(faceFeature2);
        }

        if (tempImportantPersonFaceFeature.size() != 0) {
            synchronized (importantPersonFaceFeature) {
                importantPersonPhotoUrls.clear();
                importantPersonFaceFeature.clear();
                importantPersonPhotoUrls.addAll(tempImportantPersonPhotoUrls);
                importantPersonFaceFeature.addAll(tempImportantPersonFaceFeature);
            }
        }
    }

    @Scheduled(fixedRate = 5000)
    public void faceRecognitionTask() {
        log.info("开始任务" + (new Date()).toString());
        File[] files = (new File(oriPath)).listFiles();
        if (files == null || files.length == 0) {
            log.warn("没有文件");
            return;
        }
        List<File> fileList = Arrays.asList(files);
        fileList.sort((o1, o2) -> {
            long diff = o1.lastModified() - o2.lastModified();
            if (diff > 0)
                return -1;
            else if (diff == 0)
                return 0;
            else
                //排序就会是递减
                return 1;
        });
        String latestFile = fileList.get(0).getPath();
        faceRecognition(latestFile);
    }

    private void faceRecognition(String imagePath) {
        if (gFaceEngine == null) {
            initFaceEngine();
            if (gFaceEngine == null) {
                return;
            }
        }

        //人脸检测
        ImageInfo imageInfo = getRGBData(new File(imagePath));
        List<FaceInfo> faceInfoList = new ArrayList<FaceInfo>();
        int detectCode = gFaceEngine.detectFaces(imageInfo.getImageData(), imageInfo.getWidth(), imageInfo.getHeight(), ImageFormat.CP_PAF_BGR24, faceInfoList);
        System.out.println(faceInfoList);

        if (faceInfoList.size() == 0) {
            setNoFace();
            return;
        }

        //人脸属性检测
        FunctionConfiguration configuration = new FunctionConfiguration();
        configuration.setSupportAge(true);
        configuration.setSupportFace3dAngle(true);
        configuration.setSupportGender(true);
        configuration.setSupportLiveness(true);
        int processCode = gFaceEngine.process(imageInfo.getImageData(), imageInfo.getWidth(), imageInfo.getHeight(), ImageFormat.CP_PAF_BGR24, faceInfoList, configuration);


        //性别检测
        List<GenderInfo> genderInfoList = new ArrayList<GenderInfo>();
        int genderCode = gFaceEngine.getGender(genderInfoList);
        if (genderCode != ErrorInfo.MOK.getValue()) {
            System.out.println("性别检测失败");
        } else {
            System.out.println("性别：" + genderInfoList.get(0).getGender() + "\n");
        }

        //年龄检测
        List<AgeInfo> ageInfoList = new ArrayList<AgeInfo>();
        int ageCode = gFaceEngine.getAge(ageInfoList);
        if (ageCode != ErrorInfo.MOK.getValue()) {
            System.out.println("年龄检测失败");
        } else {
            System.out.println("年龄：" + ageInfoList.get(0).getAge() + "\n");
        }

        //3D信息检测
        List<Face3DAngle> face3DAngleList = new ArrayList<Face3DAngle>();
        int face3dCode = gFaceEngine.getFace3DAngle(face3DAngleList);
        if (face3DAngleList.size() != 0) {
            System.out.println("3D角度：" + face3DAngleList.get(0).getPitch() + "," + face3DAngleList.get(0).getRoll() + "," + face3DAngleList.get(0).getYaw() + "\n");
        }

        //活体检测
        List<LivenessInfo> livenessInfoList = new ArrayList<LivenessInfo>();
        int livenessCode = gFaceEngine.getLiveness(livenessInfoList);
        if (livenessInfoList.size() != 0) {
            System.out.println("活体：" + livenessInfoList.get(0).getLiveness() + "\n");
        }

        setMyFaceInfo(
                imagePath,
                imageInfo,
                faceInfoList,
                genderInfoList,
                genderCode,
                ageInfoList,
                ageCode,
                face3DAngleList,
                face3dCode,
                livenessInfoList,
                livenessCode);

        //IR属性处理
        ImageInfo imageInfoGray = getGrayData(new File(imagePath));
        List<FaceInfo> faceInfoListGray = new ArrayList<FaceInfo>();
        int detectCodeGray = gFaceEngine.detectFaces(imageInfoGray.getImageData(), imageInfoGray.getWidth(), imageInfoGray.getHeight(), ImageFormat.CP_PAF_GRAY, faceInfoListGray);

        FunctionConfiguration configuration2 = new FunctionConfiguration();
        configuration2.setSupportIRLiveness(true);
        int processCode2 = gFaceEngine.processIr(imageInfoGray.getImageData(), imageInfoGray.getWidth(), imageInfoGray.getHeight(), ImageFormat.CP_PAF_GRAY, faceInfoListGray, configuration2);
        //IR活体检测
        List<IrLivenessInfo> irLivenessInfo = new ArrayList<IrLivenessInfo>();
        int livenessIr = gFaceEngine.getLivenessIr(irLivenessInfo);
        if (irLivenessInfo.size() != 0) {
            System.out.println("IR活体：" + irLivenessInfo.get(0).getLiveness());
        }

        //设置活体检测参数
        int paramCode = gFaceEngine.setLivenessParam(0.8f, 0.8f);


        //获取激活文件信息
        ActiveFileInfo activeFileInfo = new ActiveFileInfo();
        int activeFileCode = gFaceEngine.getActiveFileInfo(activeFileInfo);

    }

    public FaceResult getFaceResult() {
        synchronized (faceResult) {
            return faceResult;
        }
    }

    private ImportPersonUrls getImportPersonUrls() {
        ImportPersonUrls importPersonUrls = new ImportPersonUrls();

        File importPersonDir = new File(importantPersonLocal);
        File[] files = importPersonDir.listFiles(File::isFile);
        if (files == null || files.length == 0) {
            importPersonUrls.setInfo("没有重点关注人员图片");
            return importPersonUrls;
        }

        // 准备结果
        List<String> importantPersonUrls = new ArrayList<>();
        for (File temp : files) {
            importantPersonUrls.add(importantPersonUrlPrefix + temp.getName());
        }
        importPersonUrls.setInfo("重点关注人员图片如下：");
        importPersonUrls.setImportantPersonUrls(importantPersonUrls);
        return importPersonUrls;
    }

    public ImportPersonUrls uploadImportPerson(MultipartFile file) {
        ImportPersonUrls importPersonUrls;
        if (gFaceEngine == null) {
            importPersonUrls = getImportPersonUrls();
            importPersonUrls.setInfo("没有配置人脸识别引擎");
            return importPersonUrls;
        }

        FaceEngine faceEngine = gFaceEngine;

        // 将MultipartFile转换为临时文件
        File tempFile;
        if (file == null || file.getOriginalFilename() == null) {
            importPersonUrls = getImportPersonUrls();
            importPersonUrls.setInfo("上传文件名错误");
            return importPersonUrls;
        }
        String fileSuffix = file.getOriginalFilename().substring(file.getOriginalFilename().lastIndexOf("."));
        try {
            tempFile = File.createTempFile("temp", fileSuffix);
            System.out.println("临时文件地址如下：");
            System.out.println(tempFile.getAbsolutePath());
        } catch (IOException e) {
            importPersonUrls = getImportPersonUrls();
            importPersonUrls.setInfo("无法创建临时文件");
            return importPersonUrls;
        }
        try {
            file.transferTo(tempFile);
        } catch (Exception e) {
            importPersonUrls = getImportPersonUrls();
            importPersonUrls.setInfo("前端传递文件转换为普通文件出错");
            return importPersonUrls;
        }

        //人脸检测
        ImageInfo imageInfo = getRGBData(tempFile);
        List<FaceInfo> faceInfoList = new ArrayList<>();
        int detectCode = faceEngine.detectFaces(
                imageInfo.getImageData(),
                imageInfo.getWidth(),
                imageInfo.getHeight(),
                ImageFormat.CP_PAF_BGR24,
                faceInfoList);

        if (detectCode != ErrorInfo.MOK.getValue() || faceInfoList.size() == 0) {
            importPersonUrls = getImportPersonUrls();
            importPersonUrls.setInfo("前端传递文件转换为普通文件出错");
            return importPersonUrls;
        }

        for (int i = 0; i < faceInfoList.size(); i++) {
            setFacePhoto(
                    faceInfoList.get(i),
                    i,
                    tempFile.getAbsolutePath(),
                    importantPersonLocal,
                    importantPersonUrlPrefix);
        }

        return getImportPersonUrls();
    }
}
