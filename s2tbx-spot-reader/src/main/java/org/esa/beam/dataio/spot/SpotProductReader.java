package org.esa.beam.dataio.spot;

import org.esa.beam.dataio.VirtualDirEx;
import org.esa.beam.dataio.readers.GeoTiffBasedReader;
import org.esa.beam.dataio.spot.dimap.SpotDimapMetadata;
import org.esa.beam.dataio.spot.dimap.SpotSceneMetadata;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;

/**
 * This is the base class for SPOT DIMAP readers, which regroups common
 * methods in order to avoid code duplication.
 *
 * @author  Cosmin Cara
 */
public abstract class SpotProductReader extends GeoTiffBasedReader<SpotDimapMetadata> {

    protected SpotSceneMetadata wrappingMetadata;

    protected SpotProductReader(ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }

    public void setProductDirectory(VirtualDirEx productDirectory) {
        this.productDirectory = productDirectory;
    }

    public void setMetadata(SpotSceneMetadata metadata) {
        this.wrappingMetadata = metadata;
    }

    protected ProductData createProductData(int dataType, int size) {
        ProductData buffer;
        switch (dataType) {
            case ProductData.TYPE_UINT8:
                buffer = ProductData.createUnsignedInstance(new byte[size]);
                break;
            case ProductData.TYPE_INT8:
                buffer = ProductData.createInstance(new byte[size]);
                break;
            case ProductData.TYPE_UINT16:
                buffer = ProductData.createUnsignedInstance(new short[size]);
                break;
            case ProductData.TYPE_INT16:
                buffer = ProductData.createInstance(new short[size]);
                break;
            case ProductData.TYPE_INT32:
                buffer = ProductData.createInstance(new int[size]);
                break;
            case ProductData.TYPE_UINT32:
                buffer = ProductData.createUnsignedInstance(new int[size]);
                break;
            case ProductData.TYPE_FLOAT32:
                buffer = ProductData.createInstance(new float[size]);
                break;
            default:
                buffer = ProductData.createUnsignedInstance(new byte[size]);
                break;
        }
        return buffer;
    }

    @SuppressWarnings("UnusedParameters")
    protected void readBandStatistics(Band band, int bandIndex, SpotDimapMetadata componentMetadata) {
        // TODO: uncomment when finding out how to compute a histogram
        /*if (band != null && componentMetadata != null) {
            HashMap<String, Double> statistics = componentMetadata.getStatistics(bandIndex);
            if (statistics != null) {
                Stx stx = new StxFactory().withMinimum(statistics.get(SpotConstants.TAG_STX_MIN))
                        .withMaximum(statistics.get(SpotConstants.TAG_STX_MAX))
                        .withMean(statistics.get(SpotConstants.TAG_STX_MEAN))
                        .withStandardDeviation(statistics.get(SpotConstants.TAG_STX_STDV))
                        .withHistogramBins(new int[0])
                        .create();
                if (stx != null) {
                    band.setStx(stx);
                }
            }
        }*/
    }

}
