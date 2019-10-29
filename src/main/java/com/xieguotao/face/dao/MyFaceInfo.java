package com.xieguotao.face.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class MyFaceInfo {
    String info;
    String facePhotoUrl;
    String similarImportPersonUrl;
    float similarScore;
}
