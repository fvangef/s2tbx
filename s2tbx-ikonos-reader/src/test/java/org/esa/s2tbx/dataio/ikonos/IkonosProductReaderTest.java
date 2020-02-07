package org.esa.s2tbx.dataio.ikonos;

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.utils.TestUtil;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assume.assumeTrue;


public class IkonosProductReaderTest {

    private static final String PRODUCTS_FOLDER = "_ikonos" + File.separator;

    @Test
    public void testReadProduct() throws IOException {
        assumeTrue(TestUtil.testdataAvailable());

        File productFile = TestUtil.getTestFile(PRODUCTS_FOLDER + "IK2_OPER_OSA_GEO_1P_20080820T092600_N38-054_E023-986_0001.SIP" + File.separator + "IK2_OPER_OSA_GEO_1P_20080820T092600_N38-054_E023-986_0001.MD.XML");

        IkonosProductReader reader = buildProductReader();

        Product product = reader.readProductNodes(productFile, null);
        assertNotNull(product.getFileLocation());
        assertNotNull(product.getName());
        assertEquals("IK2_OPER_OSA_GEO_1P_20080820T092600_N38-054_E023-986_0001", product.getName());
        assertNotNull(product.getPreferredTileSize());
        assertNotNull(product.getProductReader());
        assertEquals(product.getProductReader(), reader);
        assertEquals(200, product.getSceneRasterWidth());
        assertEquals(200, product.getSceneRasterHeight());
        assertEquals("Ikonos Product", product.getProductType());
        assertEquals("20-AUG-2008 09:26:00.000000", product.getStartTime().toString());
        assertEquals("20-AUG-2008 09:26:00.000000", product.getEndTime().toString());
        assertEquals("metadata", product.getMetadataRoot().getName());

        GeoCoding geoCoding = product.getSceneGeoCoding();
        assertNotNull(geoCoding);
        CoordinateReferenceSystem coordinateReferenceSystem = geoCoding.getGeoCRS();
        assertNotNull(coordinateReferenceSystem);
        assertNotNull(coordinateReferenceSystem.getName());
        assertEquals("World Geodetic System 1984", coordinateReferenceSystem.getName().getCode());

        assertEquals(0, product.getMaskGroup().getNodeCount());

        assertEquals(5, product.getBands().length);

        Band band = product.getBandAt(0);
        assertNotNull(band);
        assertEquals(21, band.getDataType());
        assertEquals(2500, band.getNumDataElems());
        assertEquals("Red", band.getName());
        assertEquals(50, band.getRasterWidth());
        assertEquals(50, band.getRasterHeight());

        assertEquals(0.3423f, band.getSampleFloat(0, 0), 0.0f);
        assertEquals(0.2793f, band.getSampleFloat(22, 20), 0.0f);
        assertEquals(0.26460f, band.getSampleFloat(21, 11), 0.0f);
        assertEquals(0.52080f, band.getSampleFloat(11, 29), 0.0f);
        assertEquals(0.3528f, band.getSampleFloat(23, 23), 0.0f);
        assertEquals(0.273f, band.getSampleFloat(23, 47), 0.0f);
        assertEquals(0.32865f, band.getSampleFloat(21, 20), 0.0f);
        assertEquals(0.3234f, band.getSampleFloat(13, 44), 0.0f);
        assertEquals(0.28035f, band.getSampleFloat(42, 49), 0.0f);
        assertEquals(0.3423f, band.getSampleFloat(5, 17), 0.0f);
        assertEquals(0.36645f, band.getSampleFloat(16, 13), 0.0f);
        assertEquals(0.35595f, band.getSampleFloat(41, 14), 0.0f);
        assertEquals(0.3801f, band.getSampleFloat(10, 10), 0.0f);
        assertEquals(0.3108f, band.getSampleFloat(32, 44), 0.0f);
        assertEquals(0.0f, band.getSampleFloat(50, 50), 0.0f);
    }

    @Test
    public void testReadProductSubset() throws IOException {
        assumeTrue(TestUtil.testdataAvailable());

        File productFile = TestUtil.getTestFile(PRODUCTS_FOLDER + "IK2_OPER_OSA_GEO_1P_20080820T092600_N38-054_E023-986_0001.SIP" + File.separator + "IK2_OPER_OSA_GEO_1P_20080820T092600_N38-054_E023-986_0001.MD.XML");

        IkonosProductReader reader = buildProductReader();

        ProductSubsetDef subsetDef = new ProductSubsetDef();
        subsetDef.setNodeNames(new String[] { "Pan", "Red", "Green" } );
        subsetDef.setRegion(new Rectangle(12, 15, 30, 25));
        subsetDef.setSubSampling(1, 1);

        Product product = reader.readProductNodes(productFile, subsetDef);
        assertNotNull(product.getFileLocation());
        assertNotNull(product.getName());
        assertEquals("IK2_OPER_OSA_GEO_1P_20080820T092600_N38-054_E023-986_0001", product.getName());
        assertNotNull(product.getPreferredTileSize());
        assertNotNull(product.getProductReader());
        assertEquals(product.getProductReader(), reader);
        assertEquals(30, product.getSceneRasterWidth());
        assertEquals(25, product.getSceneRasterHeight());
        assertEquals("Ikonos Product", product.getProductType());
        assertEquals("20-AUG-2008 09:26:00.000000", product.getStartTime().toString());
        assertEquals("20-AUG-2008 09:26:00.000000", product.getEndTime().toString());
        assertEquals("metadata", product.getMetadataRoot().getName());

        GeoCoding geoCoding = product.getSceneGeoCoding();
        assertNotNull(geoCoding);
        CoordinateReferenceSystem coordinateReferenceSystem = geoCoding.getGeoCRS();
        assertNotNull(coordinateReferenceSystem);
        assertNotNull(coordinateReferenceSystem.getName());
        assertEquals("World Geodetic System 1984", coordinateReferenceSystem.getName().getCode());

        assertEquals(0, product.getMaskGroup().getNodeCount());

        assertEquals(3, product.getBands().length);

        Band bandRed = product.getBandAt(0);
        assertNotNull(bandRed);
        assertEquals(21, bandRed.getDataType());
        assertEquals(42, bandRed.getNumDataElems());
        assertEquals("Red", bandRed.getName());
        assertEquals(7, bandRed.getRasterWidth());
        assertEquals(6, bandRed.getRasterHeight());

        assertEquals(0.34965f, bandRed.getSampleFloat(0, 0), 0.0f);
        assertEquals(0.45255f, bandRed.getSampleFloat(1, 2), 0.0f);
        assertEquals(0.30135f, bandRed.getSampleFloat(3, 1), 0.0f);
        assertEquals(0.29925f, bandRed.getSampleFloat(6, 4), 0.0f);

        Band bandPan = product.getBandAt(1);
        assertNotNull(bandPan);
        assertEquals(21, bandPan.getDataType());
        assertEquals(750, bandPan.getNumDataElems());
        assertEquals("Pan", bandPan.getName());
        assertEquals(30, bandPan.getRasterWidth());
        assertEquals(25, bandPan.getRasterHeight());

        assertEquals(0.524145f, bandPan.getSampleFloat(0, 0), 0.0f);
        assertEquals(0.449445f, bandPan.getSampleFloat(22, 20), 0.0f);
        assertEquals(0.459405f, bandPan.getSampleFloat(21, 11), 0.0f);
        assertEquals(0.412095f, bandPan.getSampleFloat(11, 21), 0.0f);
        assertEquals(0.519165f, bandPan.getSampleFloat(23, 23), 0.0f);
        assertEquals(0.526635f, bandPan.getSampleFloat(20, 24), 0.0f);
        assertEquals(0.422055f, bandPan.getSampleFloat(21, 20), 0.0f);
        assertEquals(0.45318f, bandPan.getSampleFloat(13, 14), 0.0f);
        assertEquals(0.392175f, bandPan.getSampleFloat(12, 19), 0.0f);
        assertEquals(0.53037f, bandPan.getSampleFloat(5, 17), 0.0f);
        assertEquals(0.498f, bandPan.getSampleFloat(16, 13), 0.0f);
        assertEquals(0.444465f, bandPan.getSampleFloat(21, 14), 0.0f);
        assertEquals(0.489285f, bandPan.getSampleFloat(20, 20), 0.0f);
        assertEquals(0.41832f, bandPan.getSampleFloat(10, 23), 0.0f);
        assertEquals(0.0f, bandPan.getSampleFloat(30, 25), 0.0f);
    }

    private static IkonosProductReader buildProductReader() {
        IkonosProductReaderPlugin plugin = new IkonosProductReaderPlugin();
        return new IkonosProductReader(plugin);
    }
}