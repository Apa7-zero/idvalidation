# 二代身份证号码效验

## 1.Maven安装
```
<dependency>
	<groupId>com.apa70</groupId>
	<artifactId>idvalidation</artifactId>
	<version>1.0.0</version>
</dependency>
```

## 2.简单的使用
```

import com.apa70.idvalidation.IDValidation;
import com.apa70.idvalidation.enums.ErrorCode;
import com.apa70.idvalidation.enums.Sex;

import java.io.IOException;
import java.text.SimpleDateFormat;

public class Test {
    
    public static void main(String[] strings) throws IOException {
        IDValidation idValidation=new IDValidation();
        boolean isSuccess=idValidation.validate("验证的二代身份证号码");
        System.out.println("是否验证成功："+idValidation.isSuccess());
        if(isSuccess){
            System.out.println("省份："+idValidation.getProvince());
            System.out.println("城市："+idValidation.getCity());
            System.out.println("区县："+idValidation.getCounty());
            System.out.println("省份代码："+idValidation.getProvinceCode());
            System.out.println("城市代码："+idValidation.getCityCode());
            System.out.println("区县代码："+idValidation.getCountyCode());
            System.out.println("性别："+((idValidation.getSex()== Sex.MAN)?"男":"女"));
            //或者
            //System.out.println("性别："+idValidation.getSexMsg());
            System.out.println("出生日期："+new SimpleDateFormat("yyyy-MM-dd" ).format(idValidation.getBirth()));
            System.out.println("使用地区代码的版本："+idValidation.getRegionVersion());
        }else{
            ErrorCode errorCode=idValidation.getErrorCode();
            switch(errorCode){
                case LENGTH:
                    System.out.println("身份证号长度不正确");
                    break;
                case FORMAT:
                    System.out.println("身份证号格式不正确");
                    break;
                case VERIFY:
                    System.out.println("身份证号效验失败");
                    break;
                case REGION:
                    System.out.println("身份证前六位没有找到相应的省市区");
                    break;
            }

            //或者
            System.out.println(idValidation.getErrorMsg());
        }
    }
}
```

## 3.效验原理
效验原理如下：
1. 判断身份证号码是否足够18位

	> 二代身份证号码为固定的18位
	
2. 判断身份证号码前17位是否为数字

	> 二代身份证号码第18位有可能为X而前17位为固定的数字
	
3. 效验第18位号码
	
	> 二代身份证号码前17位通过一个特定的算法可算出第18位具体如下
	>> 1. 前17位与固定的数字相乘后相加具体数字为：第一位×7+第二位×9+第三位×10+第四位5+第五位×8+第六位×4+第七位×2+第八位×1+第九位×6+第十位×3+第十一位×7+第二十位×9+第十三位×10+第十四位×5+第十五位×8+第十六位×4+第十七位×2
	>> 2. 通过上面的公式相加等到的数字在除以11其余数必定为0-10。
		>>> + 如果余数为0则第十八位为1
		>>> + 如果余数为1则第十八位为0
		>>> + 如果余数为2则第十八位为X
		>>> + 如果余数为3则第十八位为9
		>>> + 如果余数为4则第十八位为8
		>>> + 如果余数为5则第十八位为7
		>>> + 如果余数为6则第十八位为6
		>>> + 如果余数为7则第十八位为5
		>>> + 如果余数为8则第十八位为4
		>>> + 如果余数为9则第十八位为3
		>>> + 如果余数为10则第十八位为2
4. 地区代码效验
	
	>身份证号码的前6位代表户籍所在地精准到区/县，一般来说只要把这些信息保存到本地就好。**但是**！地区代码并不是一直不会变！比如2019年莱芜市撤市，1997年重庆变为直辖
	>这些对应的行政代码都会改变！
	
	>我收集了1980到现在（2019年10月）的地区代码，效验方式为通过出生日期找到其相应年份的代码并寻找，如果往后翻3年，如果在找不到再往前翻3年，在找不到的话就判断为无效（比如
	>一个人为2000年出生那么会找到2000年的地区代码寻找如果找不到会在到 1999年、1998年、1997年、2001年、2002年、2003年的代码再去寻找，在找不到则判断为无效）

## 4.如何扩展地区代码

具体如下
	
```
	
import com.apa70.idvalidation.Collect;

import java.io.File;
import java.io.IOException;

public class Test {
    
    public void test() throws IOException {
        Collect collect =new Collect();

        File file=new File("html路径所在地");
        //第二个参数为版本号具体为 年+月，如果没有月份直接 年+01。第二个参数也可以传html字符串代码
        collect.add(file,201901,"保存路径");
    }
}
```

数据通过国家民政部网站获取

使用：

```

import com.apa70.idvalidation.IDValidation;
import com.apa70.idvalidation.enums.ErrorCode;
import com.apa70.idvalidation.enums.Sex;

import java.io.IOException;
import java.text.SimpleDateFormat;

public class Test {

    public static void main(String[] strings) throws IOException {
        IDValidation idValidation=new IDValidation("扩展的路径");
        boolean isSuccess=idValidation.validate("验证的二代身份证号码");
        System.out.println("是否验证成功："+isSuccess);
    }
}
```

+ **注意：扩展路径为 administrative-code-data目录的同级路径**
+ 加入扩展路径后，会优先使用扩展路径的数据

## 5.具体API

IDValidation类：

|方法名|参数|返回值|说明|
|:-|:-:|-:|-:|
|构造方法|-|-|无参数构造方法|
|构造方法|path(String类型，扩展路径)|-|string类型的构造方法|
|validate|id(String类型，身份证号码)|boolean类型，是否效验成功|效验身份证号码的方法|
|getPath|-|String类型，扩展路径|获取扩展路径|
|setPath|path(String类型，扩展路径)|-|定义扩展路径|
|isSuccess|-|boolean类型，是否成功|效验的身份证号码是否成功
|getErrorCode|-|com.apa70.idvalidation.enums.ErrorCode枚举类型，错误代码|ErrorCode.SUCCESS为成功<br>ErrorCode.LENGTH为长度不正确<br>ErrorCode.FORMAT为身份证号格式不正确<br>ErrorCode.VERIFY为身份证号效验失败<br>ErrorCode.REGION为身份证前六位没有找到相应的省市区|
|getErrorMsg|-|String类型，错误信息的汉字描述|获取错误信息的汉字描述|
|getBirth|-|java.util.Date类型，出生日期|出生日期具体为 Y-m-d
|getSex|-|com.apa70.idvalidation.enums.Sex枚举类型，性别|Sex.MAN为男，Sex.WOMAN为女|
|getSexMsg|-|String类型，性别的汉字描述|固定为男或者女
|getProvince|-|String类型，身份证号所在省|-|
|getCity|-|String类型，身份证号所在市|-|
|getCounty|-|String类型，身份证号所在区/县|-|
|getProvinceCode|-|int类型，身份证号所在省代码|-|
|getCityCode|-|int类型，身份证号所在市代码|-|
|getCountyCode|-|int类型，身份证号所在区/县代码|-|
|getRegionVersion|-|int类型，使用的地区代码版本|如：201901|

Collect类:

|方法名|参数|返回值|说明|
|:-|:-:|-:|-:|
|add|file(File类型，html文件的file对象)<br>version(int类型，版本号如201901)<br>path(String类型，保存路径)|-|-|
|add|htmlString(String类型，html字符串代码)<br>version(int类型，版本号如201901)<br>path(String类型，保存路径)|-|-|

## 6.数据地址
[中华人民共和国民政部](http://www.mca.gov.cn "中华人民共和国民政部")