package com.apa70.idvalidation;

import com.alibaba.fastjson.JSON;
import com.apa70.idvalidation.enums.ErrorCode;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

public class IDValidationTest {

    private IDValidation idValidation;

    public IDValidationTest(){
        idValidation=new IDValidation();
    }

    @Test
    public void successZhiXianShi() throws IOException {
        //直辖市身份证号
        idValidation.validate("140428199705020037");
        System.out.println(JSON.toJSONString(idValidation));
        System.out.println(idValidation.getProvince());
        System.out.println(idValidation.getCity());
        System.out.println(idValidation.getCounty());
        System.out.println(idValidation.getErrorCode());
        System.out.println(idValidation.getRegionVersion());
        Assert.assertEquals(idValidation.getErrorCode(), ErrorCode.SUCCESS);
    }

    @Test
    public void successSheng() throws IOException{
        //省身份身份证号
        idValidation.validate("");
        System.out.println(idValidation.getProvince());
        System.out.println(idValidation.getCity());
        System.out.println(idValidation.getCounty());
        System.out.println(idValidation.getErrorCode());
        System.out.println(idValidation.getRegionVersion());
        Assert.assertEquals(idValidation.getErrorCode(), ErrorCode.SUCCESS);
    }

    @Test
    public void error()throws IOException{
        //长度错误
        idValidation.validate("140123");
        Assert.assertEquals(idValidation.getErrorCode(),ErrorCode.LENGTH);

        //效验错误
        idValidation.validate("140000000000000000");
        Assert.assertEquals(idValidation.getErrorCode(),ErrorCode.VERIFY);

        //格式错误
        idValidation.validate("140xxx1475xxxxxxxx");
        Assert.assertEquals(idValidation.getErrorCode(),ErrorCode.FORMAT);

        //行政代码错误
        idValidation.validate("000000000000000001");
        Assert.assertEquals(idValidation.getErrorCode(),ErrorCode.REGION);
    }
}
