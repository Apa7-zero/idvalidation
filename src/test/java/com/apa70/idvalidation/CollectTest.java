package com.apa70.idvalidation;

import org.junit.Test;

import java.io.File;
import java.io.IOException;

public class CollectTest {

    @Test
    public void add() throws IOException {
        Collect collect=new Collect();
        File file=new File("F:/img/201903011447.html");
        collect.add(file,201801,"F:/img");
    }
}
