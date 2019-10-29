package com.xieguotao.face.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FaceResult {
    boolean haveFace;
    boolean haveImportPerson;
    String originalPhotoUrl;
    List<MyFaceInfo> myFaceInfoList;
}
