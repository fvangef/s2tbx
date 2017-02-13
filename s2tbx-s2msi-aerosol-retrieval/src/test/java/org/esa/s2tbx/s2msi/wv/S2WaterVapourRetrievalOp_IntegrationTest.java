package org.esa.s2tbx.s2msi.wv;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * @author Tonio Fincke
 */
public class S2WaterVapourRetrievalOp_IntegrationTest {

    @Test
    public void testS2WaterVapourRetrievalOp() throws IOException {
        final String aerosolProductFile =
                S2WaterVapourRetrievalOp_IntegrationTest.class.getResource("aerosol_subset.dim").getFile();
        final Product aerosolProduct = ProductIO.readProduct(aerosolProductFile);

        final S2WaterVapourRetrievalOp waterVapourRetrievalOp = new S2WaterVapourRetrievalOp();
        waterVapourRetrievalOp.setSourceProduct(aerosolProduct);
        waterVapourRetrievalOp.setParameterDefaultValues();
        final Product waterVapourProduct = waterVapourRetrievalOp.getTargetProduct();

        final Band waterVapourBand = waterVapourProduct.getBand("water_vapour");
        assertNotNull(waterVapourBand);
        assertEquals(21.02074f, waterVapourBand.getSampleFloat(5, 5), 1e-8);
        assertEquals(24.296015f, waterVapourBand.getSampleFloat(95, 5), 1e-8);
        assertEquals(21.63154f, waterVapourBand.getSampleFloat(5, 95), 1e-8);
        assertEquals(22.193762f, waterVapourBand.getSampleFloat(95, 95), 1e-8);
    }

}