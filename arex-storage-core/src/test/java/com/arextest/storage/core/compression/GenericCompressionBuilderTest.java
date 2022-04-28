package com.arextest.storage.core.compression;

import com.arextest.storage.model.mocker.impl.SoaMainMocker;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.lang.reflect.Field;
import java.util.List;

/**
 * @author jmo
 * @since 2021/11/19
 */
@RunWith(JUnit4.class)
public class GenericCompressionBuilderTest {

    @Test
    public void testDiscoverCompression() {
        List<Field> fieldList = GenericCompressionBuilder.DEFAULT.discoverCompression(SoaMainMocker.class);
        Assert.assertNotNull(fieldList);
        Assert.assertEquals(2, fieldList.size());
    }
}