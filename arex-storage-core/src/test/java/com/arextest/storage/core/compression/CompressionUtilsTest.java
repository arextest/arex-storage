package com.arextest.storage.core.compression;


import com.arextest.common.utils.CompressionUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

/**
 * @author jmo
 * @since 2021/11/18
 */
@RunWith(JUnit4.class)
public class CompressionUtilsTest {

    @Test
    public void compress() {
        String source = "AAAAAAA";
        String result = CompressionUtils.useZstdCompress(source);
        Assert.assertNotEquals(source, result);
        result = CompressionUtils.useZstdDecompress(result);
        Assert.assertEquals(source, result);
        result = CompressionUtils.useZstdDecompress("KLUv/SDWdQUAEgwoIkBrnAB0Y8gyKs76ZGijw" +
                "/HZdjzUxUDDKtu9F6r9zqooalxzJhD7Ryr6YakhuUog9h3JKEqu3s1Nw1i+X2gGrs7YIgzg0BfA2ra3jjFQl" +
                "+rSxtr30CJKzQsF80nNOe0bjiS4mmcMEJZESTg4fN8MKUpLgwFi5eoK7/GhiAOdiCwB1sAF3XvLMbfFTg+nrX0vD" +
                "/3EiHEiF3zacz8SalY+BQMATAlYbihjQbED");
        source = "{\"Response\":{\"Head\":{\"TransNo\":\"2213513e-2ee3-4439-9c06-8ad9d815e44b\"," +
                "\"ReplyStatus\":\"NO_RESULT\"},\"Body\":{}},\"ResponseStatus\":{\"Timestamp\":\"2021-11-19T03:42:20" +
                ".415+08:00\",\"Ack\":\"Success\",\"Errors\":[],\"Extension\":[]}}";
        Assert.assertEquals(source, result);
    }
}