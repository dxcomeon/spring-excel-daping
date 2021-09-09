package com.excel.controller;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.ObjectUtil;
import cn.hutool.core.util.PinyinUtil;
import cn.hutool.core.util.StrUtil;
import com.excel.service.ImportService;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

@Controller
public class ImportController {

    @Autowired
    private ImportService importService;

    List<Map<String, Object>> data = new ArrayList<>();
    List<Map<String, Object>> diYaData = new ArrayList<>();
    List<Map<String, Object>> taiQuData = new ArrayList<>();
    Set<Object> selectDate = new HashSet<>();
    Set<Object> selectTaQuDate = new HashSet<>();

    @CrossOrigin
    @PostMapping(value = "/upload", consumes = "multipart/*", headers = "content-type=multipart/form-data")
    @ResponseBody
    public Map<String, Object> uploadExcel(MultipartFile file) throws Exception {

        Map<String, Object> result = new HashMap<>();
        if (file.isEmpty()) {
            result.put("stauts", 4001);
            result.put("msg", "文件不能为空");
            result.put("data", "");
            return result;
        }
        InputStream inputStream = file.getInputStream();
        List<String> title = new ArrayList<>();
        List<List<Object>> list = importService
                .getBankListByExcel(title, inputStream, file.getOriginalFilename());
        if (CollUtil.isNotEmpty(list)) {
            data = new ArrayList<>();
            selectDate = new HashSet<>();
        }
        inputStream.close();
        for (int i = 1; i < list.size(); i++) {
            List<Object> lo = list.get(i);
            Map<String, Object> map = new HashMap<>();
            for (int i1 = 0; i1 < title.size(); i1++) {
                map.put(title.get(i1), lo.get(i1));
                if (title.get(i1).equals(PinyinUtil.getPinYin("线路名称").replace("%", ""))) {
                    selectDate.add(lo.get(i1));
                }
            }
            data.add(map);
        }
        result.put("stauts", 200);
        result.put("msg", "上传成功");
        result.put("data", "");
        return result;
    }

    @CrossOrigin
    @PostMapping(value = "/get")
    @ResponseBody
    public Map<String, Object> search(@RequestBody Map<String, String> search) {
        List<Map<String, Object>> searchData = new ArrayList<>();

        for (Map<String, Object> datum : data) {
            List<Boolean> flag = new ArrayList<>();
            for (String key : search.keySet()) {
                if (search.get(key).equals(datum.get(key))) {
                    flag.add(true);
                }
            }
            if (flag.size() == search.keySet().size()) {
                searchData.add(datum);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("stauts", 200);
        result.put("msg", "");
        result.put("data", searchData);
        return result;
    }

    @CrossOrigin
    @GetMapping(value = "/select")
    @ResponseBody
    public Map<String, Object> select() {
        Map<String, Object> result = new HashMap<>();
        result.put("stauts", 200);
        result.put("msg", "");
        result.put("data", selectDate);
        return result;
    }

    @CrossOrigin
    @PostMapping(value = "/proportion")
    @ResponseBody
    public Map<String, Object> proportion(@RequestBody Map<String, String> search) {
        List<Map<String, Object>> searchData = new ArrayList<>();

        for (Map<String, Object> datum : data) {
            Map<String, Object> proportionData = new HashMap<>();
            double up = 0;
            if (ObjectUtil
                    .isNotEmpty(datum.get(PinyinUtil.getPinYin("上期线损率（%）").replace("%", "")))) {
                up = Double.parseDouble(String.valueOf(
                        datum.get(PinyinUtil.getPinYin("上期线损率（%）").replace("%", ""))));
                BigDecimal b = new BigDecimal(up);
                up = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            }

            double now = 0;
            if (ObjectUtil
                    .isNotEmpty(datum.get(PinyinUtil.getPinYin("上期线损率（%）").replace("%", "")))) {
                now = Double.parseDouble(String.valueOf(
                        datum.get(PinyinUtil.getPinYin("本月线损率（%）").replace("%", ""))));
                BigDecimal b = new BigDecimal(now);
                now = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            }

            proportionData.put(PinyinUtil.getPinYin("线路名称").replace("%", ""),
                    datum.get(PinyinUtil.getPinYin("线路名称").replace("%", "")));
            proportionData.put(PinyinUtil.getPinYin("上期线损率（%）").replace("%", ""),
                    datum.get(PinyinUtil.getPinYin("上期线损率（%）").replace("%", "")));
            proportionData.put(PinyinUtil.getPinYin("本月线损率（%）").replace("%", ""),
                    datum.get(PinyinUtil.getPinYin("本月线损率（%）").replace("%", "")));
            proportionData.put(PinyinUtil.getPinYin("统计时间").replace("%", ""),
                    DateUtil.now());
            if (search.get("colume").equals("1")) {
                if (up >= 5) {
                    double v = now - up;
                    if (v >= 50) {
                        BigDecimal b = new BigDecimal(v);
                        v = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                        proportionData.put("yuzhi",v);
                        searchData.add(proportionData);
                    }
                }
            } else {
                if (up < 5) {
                    double v = now - up;
                    if (v >= 30) {
                        BigDecimal b = new BigDecimal(v);
                        v = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                        proportionData.put("yuzhi",v);
                        searchData.add(proportionData);
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("stauts", 200);
        result.put("msg", "");
        result.put("data", searchData);
        return result;
    }

    @CrossOrigin
    @PostMapping(value = "/count")
    @ResponseBody
    public Map<String, Object> count(@RequestBody Map<String, String> search) {
        List<Map<String, Object>> searchData = new ArrayList<>();
        Map<String, Double> countMap = new HashMap<>();

        for (Map<String, Object> datum : data) {
            List<Boolean> flag = new ArrayList<>();
            for (String key : search.keySet()) {
                if (search.get(key).equals(datum.get(key))) {
                    flag.add(true);
                }
            }
            if (flag.size() == search.keySet().size()) {
                searchData.add(datum);
            }
        }
        if (CollUtil.isNotEmpty(searchData)) {
            List<String> countList = new ArrayList<>();
            countList.add("本月供电量（kWh）");
            countList.add("本月售电量（kWh）");
            countList.add("本月专变售电量（kWh）");
            countList.add("本月损失电量（kWh）");
            countList.add("本月线损率（%）");
            countList.add("累计供电量（kWh）");
            countList.add("累计售电量（kWh）");
            countList.add("上期线损率（%）");

            for (Map<String, Object> searchDatum : searchData) {
                for (String s : countList) {
                    String key = PinyinUtil.getPinYin(s).replace("%", "");
                    if (null == countMap.get(key)) {
                        Double aDouble = Double.valueOf(
                                ObjectUtil.isEmpty(searchDatum.get(key)) ? "0"
                                        : searchDatum.get(key).toString());
                        BigDecimal b = new BigDecimal(aDouble);
                        aDouble = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                        countMap.put(key, aDouble);
                    } else {
                        double v = countMap.get(key) + Double.parseDouble(
                                ObjectUtil.isEmpty(searchDatum.get(key)) ? "0"
                                        : searchDatum.get(key).toString());
                        BigDecimal b = new BigDecimal(v);
                        v = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                        countMap.put(key, v);
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("stauts", 200);
        result.put("msg", "");
        result.put("data", countMap);
        return result;
    }

    @CrossOrigin
    @PostMapping(value = "/diyazhuanbian/upload", consumes = "multipart/*", headers = "content-type=multipart/form-data")
    @ResponseBody
    public Map<String, Object> diyazhuanbianUploadExcel(MultipartFile file) throws Exception {

        Map<String, Object> result = new HashMap<>();
        if (file.isEmpty()) {
            result.put("stauts", 4001);
            result.put("msg", "文件不能为空");
            result.put("data", "");
            return result;
        }
        InputStream inputStream = file.getInputStream();
        List<String> title = new ArrayList<>();
        List<List<Object>> list = importService
                .getBankListByExcel(title, inputStream, file.getOriginalFilename());
        if (CollUtil.isNotEmpty(list)) {
            diYaData = new ArrayList<>();
            selectDate = new HashSet<>();
        }
        inputStream.close();
        for (int i = 1; i < list.size(); i++) {
            List<Object> lo = list.get(i);
            Map<String, Object> map = new HashMap<>();
            for (int i1 = 0; i1 < title.size(); i1++) {
                map.put(title.get(i1), lo.get(i1));
                if (title.get(i1).equals(PinyinUtil.getPinYin("线路名称").replace("%", ""))) {
                    selectDate.add(lo.get(i1));
                }
            }
            diYaData.add(map);
        }
        result.put("stauts", 200);
        result.put("msg", "上传成功");
        result.put("data", "");
        return result;
    }

    @CrossOrigin
    @PostMapping(value = "/diyazhuanbian/get")
    @ResponseBody
    public Map<String, Object> diyazhuanbianSearch(@RequestBody Map<String, String> search) {
        List<Map<String, Object>> searchData = new ArrayList<>();

        for (Map<String, Object> datum : diYaData) {
            List<Boolean> flag = new ArrayList<>();
            for (String key : search.keySet()) {
                if (search.get(key).equals(datum.get(key))) {
                    flag.add(true);
                }
            }
            if (flag.size() == search.keySet().size()) {
                searchData.add(datum);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("stauts", 200);
        result.put("msg", "");
        result.put("data", searchData);
        return result;
    }

    @CrossOrigin
    @PostMapping(value = "/diyazhuanbian/count")
    @ResponseBody
    public Map<String, Object> diyazhuanbianCount(@RequestBody Map<String, String> search) {
        List<Map<String, Object>> searchData = new ArrayList<>();
        Map<String, Double> countMap = new HashMap<>();

        for (Map<String, Object> datum : diYaData) {
            List<Boolean> flag = new ArrayList<>();
            for (String key : search.keySet()) {
                if (search.get(key).equals(datum.get(key))) {
                    flag.add(true);
                }
            }
            if (flag.size() == search.keySet().size()) {
                searchData.add(datum);
            }
        }
        if (CollUtil.isNotEmpty(searchData)) {
            List<String> countList = new ArrayList<>();
            countList.add("计费电量");
            countList.add("免费电量");
            countList.add("有功总电量");
            countList.add("退补电量");
            countList.add("变损电量");
            countList.add("线损电量");
            countList.add("总电费");
            countList.add("电度电费");

            for (Map<String, Object> searchDatum : searchData) {
                for (String s : countList) {
                    String key = PinyinUtil.getPinYin(s).replace("%", "");
                    if (null == countMap.get(key)) {
                        Double aDouble = Double.valueOf(
                                ObjectUtil.isEmpty(searchDatum.get(key)) ? "0"
                                        : searchDatum.get(key).toString());
                        BigDecimal b = new BigDecimal(aDouble);
                        aDouble = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                        countMap.put(key, aDouble);
                    } else {
                        double v = countMap.get(key) + Double.parseDouble(
                                ObjectUtil.isEmpty(searchDatum.get(key)) ? "0"
                                        : searchDatum.get(key).toString());
                        BigDecimal b = new BigDecimal(v);
                        v = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                        countMap.put(key, v);
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("stauts", 200);
        result.put("msg", "");
        result.put("data", countMap);
        return result;
    }


    @CrossOrigin
    @PostMapping(value = "/taiqu/upload", consumes = "multipart/*", headers = "content-type=multipart/form-data")
    @ResponseBody
    public Map<String, Object> taiQuUploadExcel(MultipartFile file) throws Exception {

        Map<String, Object> result = new HashMap<>();
        if (file.isEmpty()) {
            result.put("stauts", 4001);
            result.put("msg", "文件不能为空");
            result.put("data", "");
            return result;
        }
        InputStream inputStream = file.getInputStream();
        List<String> title = new ArrayList<>();
        List<List<Object>> list = importService
                .getBankListByExcel(title, inputStream, file.getOriginalFilename());
        if (CollUtil.isNotEmpty(list)) {
            taiQuData = new ArrayList<>();
        }
        inputStream.close();
        for (int i = 1; i < list.size(); i++) {
            List<Object> lo = list.get(i);
            Map<String, Object> map = new HashMap<>();
            for (int i1 = 0; i1 < title.size(); i1++) {
                map.put(title.get(i1), lo.get(i1));
                if (title.get(i1).equals(PinyinUtil.getPinYin("线路名称").replace("%", ""))) {
                    selectTaQuDate.add(lo.get(i1));
                }
            }
            taiQuData.add(map);
        }
        result.put("stauts", 200);
        result.put("msg", "上传成功");
        result.put("data", "");
        return result;
    }

    @CrossOrigin
    @PostMapping(value = "/taiqu/get")
    @ResponseBody
    public Map<String, Object> taiQuSearch(@RequestBody Map<String, String> search) {
        List<Map<String, Object>> searchData = new ArrayList<>();

        for (Map<String, Object> datum : taiQuData) {
            List<Boolean> flag = new ArrayList<>();
            for (String key : search.keySet()) {
                if (search.get(key).equals(datum.get(key))) {
                    flag.add(true);
                }
            }
            if (flag.size() == search.keySet().size()) {
                searchData.add(datum);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("stauts", 200);
        result.put("msg", "");
        result.put("data", searchData);
        return result;
    }

    @CrossOrigin
    @PostMapping(value = "/taiqu/proportion")
    @ResponseBody
    public Map<String, Object> taiQuProportion(@RequestBody Map<String, String> search) {
        List<Map<String, Object>> searchData = new ArrayList<>();

        for (Map<String, Object> datum : taiQuData) {
            Map<String, Object> proportionData = new HashMap<>();
            double up = 0;
            if (ObjectUtil
                    .isNotEmpty(datum.get(PinyinUtil.getPinYin("上期线损率（%）").replace("%", "")))) {
                up = Double.parseDouble(String.valueOf(
                        datum.get(PinyinUtil.getPinYin("上期线损率（%）").replace("%", ""))));
                BigDecimal b = new BigDecimal(up);
                up = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            }

            double now = 0;
            if (ObjectUtil
                    .isNotEmpty(datum.get(PinyinUtil.getPinYin("上期线损率（%）").replace("%", "")))) {
                now = Double.parseDouble(String.valueOf(
                        datum.get(PinyinUtil.getPinYin("本月线损率（%）").replace("%", ""))));
                BigDecimal b = new BigDecimal(now);
                now = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
            }

            proportionData.put(PinyinUtil.getPinYin("线路名称").replace("%", ""),
                    datum.get(PinyinUtil.getPinYin("线路名称").replace("%", "")));
            proportionData.put(PinyinUtil.getPinYin("上期线损率（%）").replace("%", ""),
                    datum.get(PinyinUtil.getPinYin("上期线损率（%）").replace("%", "")));
            proportionData.put(PinyinUtil.getPinYin("本月线损率（%）").replace("%", ""),
                    datum.get(PinyinUtil.getPinYin("本月线损率（%）").replace("%", "")));
            proportionData.put(PinyinUtil.getPinYin("统计时间").replace("%", ""),
                    DateUtil.now());
            if (search.get("colume").equals("1")) {
                if (up >= 5) {
                    double v = now - up;
                    if (v >= 50) {
                        BigDecimal b = new BigDecimal(v);
                        v = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                        proportionData.put("yuzhi",v);
                        searchData.add(proportionData);
                    }
                }
            } else {
                if (up < 5) {
                    double v = now - up;
                    if (v >= 30) {
                        BigDecimal b = new BigDecimal(v);
                        v = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                        proportionData.put("yuzhi",v);
                        searchData.add(proportionData);
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("stauts", 200);
        result.put("msg", "");
        result.put("data", searchData);
        return result;
    }

    @CrossOrigin
    @PostMapping(value = "/taiqu/count")
    @ResponseBody
    public Map<String, Object> taiQuCount(@RequestBody Map<String, String> search) {
        List<Map<String, Object>> searchData = new ArrayList<>();
        Map<String, Double> countMap = new HashMap<>();

        for (Map<String, Object> datum : taiQuData) {
            List<Boolean> flag = new ArrayList<>();
            for (String key : search.keySet()) {
                if (search.get(key).equals(datum.get(key))) {
                    flag.add(true);
                }
            }
            if (flag.size() == search.keySet().size()) {
                searchData.add(datum);
            }
        }
        if (CollUtil.isNotEmpty(searchData)) {
            List<String> countList = new ArrayList<>();
            countList.add("本月供电量（kWh）");
            countList.add("本月售电量（kWh）");
            countList.add("本月专变售电量（kWh）");
            countList.add("本月损失电量（kWh）");
            countList.add("本月线损率（%）");
            countList.add("累计供电量（kWh）");
            countList.add("累计售电量（kWh）");
            countList.add("上期线损率（%）");

            for (Map<String, Object> searchDatum : searchData) {
                for (String s : countList) {
                    String key = PinyinUtil.getPinYin(s).replace("%", "");
                    if (null == countMap.get(key)) {
                        Double aDouble = Double.valueOf(
                                ObjectUtil.isEmpty(searchDatum.get(key)) ? "0"
                                        : searchDatum.get(key).toString());
                        BigDecimal b = new BigDecimal(aDouble);
                        aDouble = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                        countMap.put(key, aDouble);
                    } else {
                        double v = countMap.get(key) + Double.parseDouble(
                                ObjectUtil.isEmpty(searchDatum.get(key)) ? "0"
                                        : searchDatum.get(key).toString());
                        BigDecimal b = new BigDecimal(v);
                        v = b.setScale(2, BigDecimal.ROUND_HALF_UP).doubleValue();
                        countMap.put(key, v);
                    }
                }
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("stauts", 200);
        result.put("msg", "");
        result.put("data", countMap);
        return result;
    }

    @CrossOrigin
    @GetMapping(value = "/taiqu/select")
    @ResponseBody
    public Map<String, Object> taiQuSelect() {
        Map<String, Object> result = new HashMap<>();
        result.put("stauts", 200);
        result.put("msg", "");
        result.put("data", selectTaQuDate);
        return result;
    }


}
