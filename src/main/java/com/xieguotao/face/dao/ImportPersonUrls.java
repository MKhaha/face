package com.xieguotao.face.dao;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ImportPersonUrls {
    String info;
    List<String> importantPersonUrls;
}
