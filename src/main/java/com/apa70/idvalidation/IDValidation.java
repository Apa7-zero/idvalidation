package com.apa70.idvalidation;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.apa70.idvalidation.entity.Distance;
import com.apa70.idvalidation.enums.ErrorCode;
import com.apa70.idvalidation.enums.Sex;
import com.apa70.idvalidation.exception.IndexFileException;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Pattern;

public class IDValidation{

    /** 自定义的目录 */
    private String path;
    /** 身份证号码 */
    private String id;
    /** 是否验证成功 */
    private boolean isSuccess;
    /** 错误代码 */
    private ErrorCode errorCode;
    /** 错误信息 */
    private String errorMsg;
    /** 出生日期 */
    private Date birth;
    /** 性别 */
    private Sex sex;
    /** 性别的汉字信息 */
    private String sexMsg;
    /** 省份 */
    private String province;
    /** 市 */
    private String city;
    /** 区/县 */
    private String county;
    /** 省代码 */
    private int provinceCode;
    /** 市代码 */
    private int cityCode;
    /** 区/县代码 */
    private int countyCode;
    /** 使用的地区代码版本 */
    private int regionVersion;

    private int upExcursion;
    private int belowExcursion;

    /** 判断数字的正则表达式 */
    private String numRegular="^[0-9]*$";
    /** 身份证前17位乘的系数 */
    private int[] idWeight={7,9,10,5,8,4,2,1,6,3,7,9,10,5,8,4,2};
    /** 余数对应的第18位 */
    private String[] idRemainder={"1","0","X","9","8","7","6","5","4","3","2"};


    public IDValidation(){
        this.path=null;
    }

    public IDValidation(String path){
        if(path!=null){
            this.path=path;
        }
    }

    /**
     * 验证方法
     * @param id 身份证号码
     * @return bool 是否验证成功
     * @throws IOException
     */
    public boolean validate(String id) throws IOException{
        this.upExcursion=3;
        this.belowExcursion=3;
        return this.start(id);
    }

    /**
     * 开始验证
     * @param id 身份证号码
     * @return bool 是否验证成功
     * @throws IOException
     */
    private boolean start(String id) throws IOException{
        this.isSuccess=false;
        if(id.length()!=18){
            this.errorCode=ErrorCode.LENGTH;
            this.errorMsg="身份证号长度不正确！";
            return false;
        }

        //对身份证效验
        int idVerify=0;
        String id18="";
        for(int i=0;i<id.length();i++){
            if(i==17){//最后一位特殊处理
                id18=id.substring(i,i+1);
                continue;
            }
            if(!Pattern.matches(numRegular,id.substring(i,i+1))){
                this.errorCode=ErrorCode.FORMAT;
                this.errorMsg="身份证号格式不正确！";
                return false;
            }
            idVerify+=Integer.parseInt(id.substring(i,i+1))*idWeight[i];
        }
        if(!id18.toUpperCase().equals(idRemainder[idVerify%11])){
            this.errorCode=ErrorCode.VERIFY;
            this.errorMsg="身份证号效验失败";
            return false;
        }

        //生日
        int birthday=Integer.parseInt(id.substring(6,12));
        try {
            this.birth=(new SimpleDateFormat("yyyyMMdd")).parse(id.substring(6,14));
        } catch (ParseException e) {
            this.errorCode=ErrorCode.FORMAT;
            this.errorMsg="身份证号中的生日格式不正确！";
            return false;
        }

        //对前6位进行效验
        boolean isSuccess=this.getRegion(id.substring(0,6),birthday);
        if(!isSuccess)
            return false;

        //性别处理
        int sex=Integer.parseInt(id.substring(16,17));
        if(sex%2==0){
            this.sex=Sex.WOMAN;
            this.sexMsg="女";
        }else{
            this.sex=Sex.MAN;
            this.sexMsg="男";
        }

        //返回
        this.isSuccess=true;
        this.errorCode=ErrorCode.SUCCESS;
        this.errorMsg="";
        return true;
    }

    /**
     * 身份证号前六位效验
     * @param id6 身份证号码
     * @param birthday 生日精准到“月”如 201912
     * @return bool
     * @throws IOException
     */
    private boolean getRegion(String id6,int birthday) throws IOException {
        //一些变量
        Distance resourceIndexDistance=null;
        Distance pathIndexDistance=null;

        //从resources读取出index.json
        InputStream indexInputStream=this.getClass().getResourceAsStream("/administrative-code-data/index.json");
        resourceIndexDistance=this.getIndexFileDistance(indexInputStream,birthday,true);

        //查看是否有自定义目录
        if(this.path!=null&&!this.path.equals("")){
            File pathIndexIndex=new File(this.path);
            if(!pathIndexIndex.exists()){
                this.path=null;
            }else{
                //有的话读取索引文件
                pathIndexDistance=this.getIndexFileDistance(this.path+"/administrative-code-data/index.json",birthday,false);
            }
        }

        //准备循环读取相应的文件
        Map<Integer, Integer> indexPositiveNumber = new TreeMap<>(resourceIndexDistance.getPositiveNumber());
        Map<Integer, Integer> indexMinusSign = new TreeMap<>(resourceIndexDistance.getMinusSign());
        //合并相应的map
        if(pathIndexDistance!=null){
            indexPositiveNumber.putAll(pathIndexDistance.getPositiveNumber());
            indexMinusSign.putAll(pathIndexDistance.getMinusSign());
        }

        //循环读取相应文件
        int i=1;
        boolean isInfo=false;
        for(int v:indexPositiveNumber.values()){
            if(i>=this.belowExcursion)
                break;

            if(v%100==0){
                //没有余数表明是自定义路径的
                isInfo=this.getRegionInfo(this.path+"/administrative-code-data/"+(v/100)+".json",id6);
                if(isInfo){
                    this.regionVersion=v/100;
                    break;
                }
            }else{
                //有余数表明为resources的路径的
                isInfo=this.getRegionInfo(this.getClass().getResourceAsStream("/administrative-code-data/"+v+".json"),id6);
                if(isInfo){
                    this.regionVersion=v;
                    break;
                }
            }
            i++;
        }
        if(!isInfo){
            i=1;
            ListIterator<Map.Entry<Integer,Integer>> ii = new ArrayList<Map.Entry<Integer,Integer>>(indexMinusSign.entrySet()).listIterator(indexMinusSign.size());
            while(ii.hasPrevious()){
                if(i>=this.upExcursion)
                    break;

                Map.Entry<Integer, Integer> entry = ii.previous();
                if(entry.getValue()%100==0){
                    //没有余数表明是自定义路径的
                    isInfo=this.getRegionInfo(this.path+"/administrative-code-data/"+(entry.getValue()/100)+".json",id6);
                    if(isInfo){
                        this.regionVersion=entry.getValue()/100;
                        break;
                    }
                }else{
                    //有余数表明为resources的路径的
                    isInfo=this.getRegionInfo(this.getClass().getResourceAsStream("/administrative-code-data/"+entry.getValue()+".json"),id6);
                    if(isInfo){
                        this.regionVersion=entry.getValue();
                        break;
                    }
                }

                i++;
            }
        }

        if(!isInfo){
            this.errorCode=ErrorCode.REGION;
            this.errorMsg="身份证前六位没有找到相应的省市区！";
            return false;
        }

        return true;
    }

    /**
     * 获取索引文件
     * @param url 文件路径
     * @param birthday 生日精准到“月”如 201912
     * @param isResources 是否为resources的路径
     * @return Distance
     * @throws IOException
     */
    private Distance getIndexFileDistance(String url,int birthday,boolean isResources) throws IOException {
        File IndexFile=new File(url);
        String IndexString=this.getFileText(IndexFile);
        if(IndexString.equals(""))
            throw new IndexFileException("索引文件为空！");

        return this.getIndexFileDistance(new FileInputStream(IndexFile),birthday,isResources);
    }

    /**
     * 获取索引文件
     * @param inputStream 索引文件的inputStream对象
     * @param birthday 生日精准到“月”如 201912
     * @param isResources 是否为resources的路径
     * @return Distance
     * @throws IOException
     */
    private Distance getIndexFileDistance(InputStream inputStream,int birthday,boolean isResources) throws IOException {
        String indexIndexString=this.getFileText(inputStream);
        if(indexIndexString.equals(""))
            throw new IndexFileException("索引文件为空！");
        List<Integer> IndexArray= JSONArray.parseArray(indexIndexString,Integer.class);

        //循环index
        Map<Integer,Integer> IndexDistancePositiveNumber=new TreeMap<>();
        Map<Integer,Integer> IndexDistanceMinusSign=new TreeMap<>();
        for(int v:IndexArray){
            if(0<=birthday-v)
                IndexDistancePositiveNumber.put(birthday-v,(isResources)?v:v*100);
            else
                IndexDistanceMinusSign.put(birthday-v,(isResources)?v:v*100);
        }

        //返回
        Distance distance=new Distance();
        distance.setPositiveNumber(IndexDistancePositiveNumber);
        distance.setMinusSign(IndexDistanceMinusSign);
        return distance;
    }

    /**
     * 获取相应行政代码json文件的信息
     * @param url 文件的url
     * @param id6 身份证号码的前六位
     * @return boolean
     * @throws IOException
     */
    private boolean getRegionInfo(String url,String id6) throws IOException {
        //判断文件是否存在
        File file=new File(url);
        if(!file.exists()){
            return false;
        }

        return this.getRegionInfo(new FileInputStream(file),id6);
    }

    /**
     * 获取相应行政代码json文件的信息
     * @param inputStream 文件的inputStream对象
     * @param id6 身份证号码的前六位
     * @return boolean
     * @throws IOException
     */
    private boolean getRegionInfo(InputStream inputStream,String id6) throws IOException {
        //读取文件并检查是否有该地区内容
        String fileTextString=this.getFileText(inputStream);
        if(fileTextString.equals(""))
            return false;
        JSONObject jsonObject=JSONObject.parseObject(fileTextString);
        if(!jsonObject.containsKey(id6))
            return false;

        //获取相关的地区信息
        String cityCode= id6.substring(0,4)+"00";
        String provinceCode= id6.substring(0,3)+"000";
        this.county=jsonObject.getString(id6);
        this.province=jsonObject.getString(provinceCode);
        if(jsonObject.containsKey(cityCode))
            this.city=jsonObject.getString(cityCode);
        else
            this.city=jsonObject.getString(provinceCode);

        this.provinceCode=Integer.parseInt(provinceCode);
        this.cityCode=Integer.parseInt(cityCode);
        this.countyCode=Integer.parseInt(id6);

        return true;
    }

    /**
     * 读取文件
     * @param file File对象
     * @return string 文件的内容
     * @throws IOException
     */
    private String getFileText(File file) throws IOException {
        return this.getFileText(new FileInputStream(file));
    }

    /**
     * 读取文件
     * @param inputStream InputStream对象
     * @return string 文件的内容
     * @throws IOException
     * */
    private String getFileText(InputStream inputStream) throws IOException {
        StringBuilder sbf = new StringBuilder();
        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            String line;
            while ((line = br.readLine()) != null) {
                sbf.append(line);
            }
            return sbf.toString();
        }finally {
            if (inputStream != null) {
                inputStream.close();
            }
        }
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getId() {
        return id;
    }

    public boolean isSuccess() {
        return isSuccess;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }

    public String getErrorMsg() {
        return errorMsg;
    }

    public Date getBirth() {
        return birth;
    }

    public Sex getSex() {
        return sex;
    }

    public String getSexMsg() {
        return sexMsg;
    }

    public String getProvince() {
        return province;
    }

    public String getCity() {
        return city;
    }

    public String getCounty() {
        return county;
    }

    public int getProvinceCode() {
        return provinceCode;
    }

    public int getCityCode() {
        return cityCode;
    }

    public int getCountyCode() {
        return countyCode;
    }

    public int getRegionVersion(){
        return regionVersion;
    }
}
