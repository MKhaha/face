package com.xieguotao.face.controller;

import com.xieguotao.face.dao.FaceResult;
import com.xieguotao.face.dao.ImportPersonUrls;
import com.xieguotao.face.service.FaceRecognition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;

@RestController
@RequestMapping("/face")
public class FaceController {

    @Autowired
    private FaceRecognition faceRecognition;

    @GetMapping("/getFaceResult")
    @ResponseBody
    public FaceResult getFaceResult() {
        return faceRecognition.getFaceResult();
    }

    @PostMapping("/uploadImportPerson")
    @ResponseBody
    public ImportPersonUrls uploadImportPerson(MultipartFile file) {
        System.out.println("接收到请求");
        ImportPersonUrls result = faceRecognition.uploadImportPerson(file);
        System.out.println("result++++++++++++++++++++++++");
        System.out.println(result);
        return result;

    }

    @PostMapping("/test")
    @ResponseBody
    public String test(MultipartFile file) throws IOException {
        System.out.println("测试成功");

        File temp = new File("D:\\testXXX");
        file.transferTo(temp);

        System.out.println("测试成功");
        return "测试成功";
    }
}
