package com.apa70.idvalidation;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.apa70.idvalidation.error.*;
import okhttp3.*;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.net.ConnectException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;

public class Collect {

    /** 判断数字的正则表达式 */
    private String numRegular="-?[1-9]\\d*";

    /**
     * 添加一个新的数据到某个目录
     * @param url 数据的url
     * @param version 版本号
     * @param path 存储地址
     * @throws IOException
     */
    public void add(String url, int version, String path) throws IOException{
        //一些变量
        String administrativeCode = "";
        Map<String,String> administrativeCodeMap= new HashMap<>();
        path+="/administrative-code-data/";
        File indexFile=null;
        File administrativeCodeFile=null;
        File administrativeCodeDataFile=null;
        Writer administrativeCodeWriter=null;
        Writer indexWriter=null;

        try {
            //通过okHttp获取html的字符串
            OkHttpClient okHttpClient = new OkHttpClient();
            Request request = new Request.Builder().url(url).get().build();
            Call call = okHttpClient.newCall(request);
            String htmlString= Objects.requireNonNull(call.execute().body()).string();

            //使用Jsoup解析这些字符串
            int trsI=0;
            Elements tables = Jsoup.parse(htmlString).select("table");
            for(Element tableElement:tables){
                Elements trs=tableElement.select("tr");
                for(Element trElement:trs){
                    if(trsI<3){trsI++;continue;}
                    for(Element tdElement:trElement.select("td")) {
                        if(tdElement.text().equals(""))continue;
                        //判断正则判断是否为数字
                        if(Pattern.matches(numRegular,tdElement.text())){
                            //是数字，则为行政代码
                            administrativeCode=tdElement.text();
                        }else{
                            //不是则为行政名
                            if(administrativeCode.equals(""))continue;
                            administrativeCodeMap.put(administrativeCode,tdElement.text());
                            administrativeCode="";
                        }
                    }
                }
            }
        }catch(ConnectException e){
            throw new GetUrlTextNullException("连接超时！或获取内容为空"+e.getMessage());
        }catch(NullPointerException e){
            throw new UrlInvalidException("链接无效！"+e.getMessage());
        }catch(NumberFormatException e){
            throw new UrlTextCannotUnableAnalysisException("链接内容无法解析"+e.getMessage());
        }

        if(administrativeCodeMap.size()<=0){
            throw new GetInfoException("获取信息失败！");
        }

        try {
            //判断administrative-code-data文件夹是否存在
            administrativeCodeDataFile=new File(path);
            if(!administrativeCodeDataFile.isDirectory()){
                //不存在创建一个
                boolean isSuccess=administrativeCodeDataFile.mkdir();
                if(!isSuccess)
                    throw new CreateFolderException("创建文件夹"+path+"失败！");
            }

            //读取索引文件
            indexFile=new File(path+"index.json");
//            JSONObject index=new JSONObject();
            JSONArray index=new JSONArray();
            if(indexFile.exists()){
                //索引文件存在
                String indexString=getFileText(indexFile);
                index=JSONObject.parseArray(indexString);
            }else{
                //索引文件不存在，添加一个
                boolean isSuccess=indexFile.createNewFile();
                if(!isSuccess)
                    throw new CreateFileException("创建index.json文件失败！");
            }

            index.add(version);
            //把行政代码转为json存储到文件夹中
            administrativeCodeFile=new File(path+version+".json");
            if(!administrativeCodeFile.exists()){
                //不存在创建一个
                boolean isSuccess=administrativeCodeFile.createNewFile();
                if(!isSuccess)
                    throw new CreateFileException("创建"+version+".json文件失败！");
            }

            //写入行政代码文件
            administrativeCodeWriter= new BufferedWriter(new OutputStreamWriter(new FileOutputStream(administrativeCodeFile,false), StandardCharsets.UTF_8));
            administrativeCodeWriter.write(JSON.toJSONString(administrativeCodeMap));
            administrativeCodeWriter.close();
            //写入索引文件
            indexWriter=new BufferedWriter (new OutputStreamWriter (new FileOutputStream (indexFile,false), StandardCharsets.UTF_8));
            indexWriter.write(JSON.toJSONString(index));

        }finally{
            if(administrativeCodeWriter!=null)
                administrativeCodeWriter.close();
            if(indexWriter!=null)
                indexWriter.close();
        }


    }

    public String getNumRegular() {
        return numRegular;
    }

    public void setNumRegular(String numRegular) {
        this.numRegular = numRegular;
    }

    private String getFileText(File file) throws IOException{
        BufferedReader reader = null;
        StringBuilder sbf = new StringBuilder();
        try {
            reader = new BufferedReader(new FileReader(file));
            String tempStr;
            while ((tempStr = reader.readLine()) != null) {
                sbf.append(tempStr);
            }
            reader.close();
            return sbf.toString();
        }finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
        }
    }
}
