package com.apa70.idvalidation;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
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
        Map<String,Map<String,String>> codeMap=null;
        Map<String,Map<String,String>> pathCodeMap=null;

        //从resources读取出code.json
        InputStream indexInputStream=this.getClass().getResourceAsStream("/administrative-code-data/code.json");
        codeMap=this.getIndexFileDistance(indexInputStream,birthday,true);

        //查看是否有自定义目录
        if(this.path!=null&&!this.path.equals("")){
            File pathIndexIndex=new File(this.path);
            if(!pathIndexIndex.exists()){
                this.path=null;
            }else{
                //有的话读取自定义的code
                pathCodeMap=this.getIndexFileDistance(this.path+"/administrative-code-data/code.json",birthday,false);
            }
        }

        //循环合并自定义文件
        if(pathCodeMap!=null){//自定义文件存在
            //初始化变量
            Map<String,String> forDateMap=null;
            Map<String,String> forPathDateMap=null;

            //循环自定义
            for (String key:pathCodeMap.keySet()) {
                forPathDateMap=pathCodeMap.get(key);

                //判断自带的是否有这个代码
                if(codeMap.containsKey(key))//有则获取
                    forDateMap=codeMap.get(key);
                else//没有则new一个
                    forDateMap=new HashMap<>();

                //循环自定义里面的时间
                for(String k:forPathDateMap.keySet())
                    forDateMap.put(k,forPathDateMap.get(k));//覆盖自带的

                codeMap.put(key,forDateMap);
            }
        }

        //判断是否有此代码
        if(!codeMap.containsKey(id6)){
            this.errorCode=ErrorCode.REGION;
            this.errorMsg="身份证前六位没有找到相应的省市区！";
            return false;
        }

        //循环找出符合的
        Map<String,String> codeDateMap=codeMap.get(id6);
        int positiveDifference=0,negativeDifference=0;
        String positiveKey="",negativeKey="";
        for(String k:codeDateMap.keySet()){
            int kInt=Integer.parseInt(k);
            int difference=birthday-kInt;
            if(difference==0){
                positiveDifference=difference;
                positiveKey=k;
                break;
            }else if(difference>0){
                if(difference<positiveDifference||positiveDifference==0){
                    positiveDifference=difference;
                    positiveKey=k;
                }
            }else{
                if(difference>negativeDifference||negativeDifference==0){
                    negativeDifference=difference;
                    negativeKey=k;
                }
            }
        }
        String dateKey=(!positiveKey.equals(""))?positiveKey:negativeKey;

        //判断日期是否存在
        if(dateKey.equals("")){
            this.errorCode=ErrorCode.REGION;
            this.errorMsg="身份证前六位没有找到相应的省市区！！";
            return false;
        }

        //准备县级
        String county=codeDateMap.get(dateKey);

        //准备省级
        String provinceCode= id6.substring(0,2)+"0000";
        if(!codeMap.get(provinceCode).containsKey(dateKey)){
            this.errorCode=ErrorCode.REGION;
            this.errorMsg="身份证前六位没有找到相应的省市区！！！！";
            return false;
        }
        String province=codeMap.get(provinceCode).get(dateKey);

        //准备市级
        String cityCode=id6.substring(0,4)+"00";
        String city="";
        if(!codeMap.containsKey(cityCode)){
            //市级单位不存在有两种情况,分别为省直辖县或者直辖市
            if(province.substring(province.length()-1).equals("市"))//直辖市
                city=province;
            else
                city="省直辖县";
        }else{
            if(!codeMap.get(cityCode).containsKey(dateKey)){
                this.errorCode=ErrorCode.REGION;
                this.errorMsg="身份证前六位没有找到相应的省市区！！！";
                return false;
            }
            city=codeMap.get(cityCode).get(dateKey);
        }

        //准备返回
        this.province=province;
        this.provinceCode=Integer.parseInt(provinceCode);
        this.city=city;
        this.cityCode=Integer.parseInt(cityCode);
        this.county=county;
        this.countyCode=Integer.parseInt(id6);
        this.regionVersion=Integer.parseInt(dateKey);

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
    private Map<String, Map<String, String>> getIndexFileDistance(String url,int birthday,boolean isResources) throws IOException {
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
    private Map<String, Map<String, String>> getIndexFileDistance(InputStream inputStream, int birthday, boolean isResources) throws IOException {
        String indexIndexString=this.getFileText(inputStream);
        if(indexIndexString.equals(""))
            throw new IndexFileException("城市文件为空！");

        return JSON.parseObject(indexIndexString,new TypeReference<Map<String, Map<String, String>>>(){});
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
