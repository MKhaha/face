package com.xieguotao.face.controller;

import com.xieguotao.face.dao.FaceResult;
import com.xieguotao.face.dao.ImportPersonUrls;
import com.xieguotao.face.service.FaceRecognition;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

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
        return faceRecognition.uploadImportPerson(file);

    }
}
