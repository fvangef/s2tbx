package org.esa.s2tbx.s2msi.idepix.operators.cloudshadow;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.lang.StringUtils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ColorPaletteDef;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.ImageInfo;
import org.esa.snap.core.datamodel.IndexCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.opengis.referencing.operation.MathTransform;

import javax.media.jai.BorderExtenderConstant;
import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.util.Map;

/**
 * todo: add comment
 */
@OperatorMetadata(alias = "CCICloudShadow",
        category = "Optical",
        authors = "Grit Kirches, Michael Paperin, Olaf Danne",
        copyright = "(c) Brockmann Consult GmbH",
        version = "1.0",
        description = "Algorithm detecting cloud shadow...")
public class S2IdepixCloudShadowOp extends Operator {

    @SourceProduct(description = "The classification product.")
    private Product s2ClassifProduct;

    @SourceProduct(alias = "s2CloudBuffer", optional = true)
    private Product s2CloudBufferProduct;      // has only classifFlagBand with buffer added


    @TargetProduct
    private Product targetProduct;

    public final static String BAND_NAME_CLOUD_SHADOW = "FlagBand";

    private final static String BAND_NAME_TEST_A = "ShadowMask_TestA";
    private final static String BAND_NAME_TEST_B = "CloudID_TestB";
    private final static String BAND_NAME_TEST_C = "ShadowID_TestC";
    private final static String BAND_NAME_TEST_D = "LongShadowID_TestC";

    private final static double MAX_CLOUD_HEIGHT = 8000.;
    private final static int MAX_TILE_DIMENSION = 1400;

    private Band sourceBandClusterA;

    private Band sourceBandFlag1;
    private Band sourceBandFlag2;

    private RasterDataNode sourceSunZenith;
    private RasterDataNode sourceSunAzimuth;
    private RasterDataNode sourceLongitude;
    private RasterDataNode sourceLatitude;
    private RasterDataNode sourceAltitude;

    private Band targetBandCloudShadow;
    private Band targetBandTestA;
    private Band targetBandTestB;
    private Band targetBandTestC;
    private Band targetBandTestD;

    static String productType;
    static String productName;
    static boolean productNameContainIdepix;
    static boolean productCentralComputation;

    static int mincloudBase;
    static int maxcloudTop;
    static double scaleAltitude = 1.;
    static double spatialResolution;  //[m]
    static int SENSOR_BAND_CLUSTERING;
    static final int clusterCountDefault = 4;
    static int clusterCountDefine;
    static double OUTLIER_THRESHOLD = 0.94;
    static double Threshold_Whiteness_Darkness;
    static int CloudShadowFragmentationThreshold;
    static int GROWING_CLOUD;
    static int searchBorderRadius;
    private int sceneRasterHeight;
    private int sceneRasterWidth;

    @Override
    public void initialize() throws OperatorException {

        productType = s2ClassifProduct.getProductType();
        productName = s2ClassifProduct.getName();
        String content = "idepix";
        productNameContainIdepix = StringUtils.containsIgnoreCase(productName, content);

        System.out.println("Product_Type:  " + productType);
        System.out.println("Product_Name:  " + productName);
        System.out.println("productNameContainIdepix:  " + productNameContainIdepix);

        sceneRasterWidth = s2ClassifProduct.getSceneRasterWidth();
        sceneRasterHeight = s2ClassifProduct.getSceneRasterHeight();
        targetProduct = new Product(productName, productType, sceneRasterWidth, sceneRasterHeight);

        ProductUtils.copyGeoCoding(s2ClassifProduct, targetProduct);

        String sourceBandNameClusterA;
        String sourceFlagName1;
        String sourceSunZenithName;
        String sourceSunAzimuthName;
        String sourceLongitudeName;
        String sourceLatitudeName;
        String sourceAltitudeName;
        if ("pyS2Ac".equals(productType)) {
            sourceBandNameClusterA = "B8a_ac";
            sourceSunZenithName = "sun_zenith";
            sourceSunAzimuthName = "sun_azimuth";
            sourceLongitudeName = "lon";
            sourceLatitudeName = "lat";
            sourceAltitudeName = "elevation";
            sourceFlagName1 = "pixel_classif_flags";
            scaleAltitude = 0.001; // altitude in[km] required
            mincloudBase = 100; // [m]
            maxcloudTop = 10000; // [m]
            SENSOR_BAND_CLUSTERING = 2;
            GROWING_CLOUD = 1;
            clusterCountDefine = clusterCountDefault;
            Threshold_Whiteness_Darkness = -1000;
            CloudShadowFragmentationThreshold = 500000;

        } else if ("S2_MSI_Level-1C".equals(productType) || ("CF-1.4".equals(productType) && productNameContainIdepix)) {
            sourceBandNameClusterA = "B8A";
            sourceSunZenithName = "sun_zenith";
            sourceSunAzimuthName = "sun_azimuth";
            sourceLongitudeName = "lon";
            sourceLatitudeName = "lat";
            sourceAltitudeName = "elevation";
            sourceFlagName1 = "pixel_classif_flags";
            scaleAltitude = 0.001; // altitude in[km] required
            mincloudBase = 100; // [m]
            maxcloudTop = 10000; // [m]
            SENSOR_BAND_CLUSTERING = 2;
            GROWING_CLOUD = 1;
            clusterCountDefine = clusterCountDefault;
            Threshold_Whiteness_Darkness = -1000;
            CloudShadowFragmentationThreshold = 500000;
        } else {
            throw new OperatorException("Product type not supported!");
        }

        sourceBandClusterA = s2ClassifProduct.getBand(sourceBandNameClusterA);

        sourceSunZenith = s2ClassifProduct.getBand(sourceSunZenithName);
        // take these. They're as good as the tile dimensions from any other band and DEFINITELY more reliable than
        // the preferred tile size of the s2ClassifProduct
        final int sourceTileWidth = sourceSunZenith.getSourceImage().getTileWidth();
        final int sourceTileHeight = sourceSunZenith.getSourceImage().getTileHeight();
        final double maximumSunZenith = sourceSunZenith.getStx().getMaximum();
        sourceSunAzimuth = s2ClassifProduct.getBand(sourceSunAzimuthName);
        sourceLatitude = s2ClassifProduct.getBand(sourceLatitudeName);
        sourceLongitude = s2ClassifProduct.getBand(sourceLongitudeName);
        sourceAltitude = s2ClassifProduct.getBand(sourceAltitudeName);

        sourceBandFlag1 = s2ClassifProduct.getBand(sourceFlagName1);
        if (s2CloudBufferProduct != null) {
            sourceBandFlag2 = s2CloudBufferProduct.getBand(sourceFlagName1);
        }

        targetBandCloudShadow = targetProduct.addBand(BAND_NAME_CLOUD_SHADOW, ProductData.TYPE_INT32);
        attachIndexCoding(targetBandCloudShadow);
        targetBandTestA = targetProduct.addBand(BAND_NAME_TEST_A, ProductData.TYPE_INT32);
        targetBandTestB = targetProduct.addBand(BAND_NAME_TEST_B, ProductData.TYPE_INT32);
        targetBandTestC = targetProduct.addBand(BAND_NAME_TEST_C, ProductData.TYPE_INT32);
        targetBandTestD = targetProduct.addBand(BAND_NAME_TEST_D, ProductData.TYPE_INT32);

        spatialResolution = determineSourceResolution();
        searchBorderRadius = (int) determineSearchBorderRadius(S2IdepixCloudShadowOp.spatialResolution, maximumSunZenith);
        final int tileWidth = determineSourceTileWidth(sceneRasterWidth, sourceTileWidth,
                                                       searchBorderRadius, searchBorderRadius);
        final int tileHeight = determineSourceTileHeight(sceneRasterHeight, sourceTileHeight,
                                                         searchBorderRadius, searchBorderRadius);

        // todo: discuss
        if (sceneRasterWidth > tileWidth || s2ClassifProduct.getSceneRasterHeight() > tileHeight) {
            final int preferredTileWidth = Math.min(sceneRasterWidth, tileWidth);
            final int preferredTileHeight = Math.min(s2ClassifProduct.getSceneRasterHeight(), tileHeight);
            targetProduct.setPreferredTileSize(preferredTileWidth, preferredTileHeight); //1500
            productCentralComputation = true;
        } else {
            targetProduct.setPreferredTileSize(sceneRasterWidth, s2ClassifProduct.getSceneRasterHeight());
            searchBorderRadius = 0;
            productCentralComputation = false;
        }
    }

    private double determineSourceResolution() throws OperatorException {
        final GeoCoding sceneGeoCoding = getSourceProduct().getSceneGeoCoding();
        if (sceneGeoCoding instanceof CrsGeoCoding) {
            final MathTransform imageToMapTransform = sceneGeoCoding.getImageToMapTransform();
            if (imageToMapTransform instanceof AffineTransform) {
                return ((AffineTransform) imageToMapTransform).getScaleX();
            }
        }
        throw new OperatorException("Invalid product: ");
    }

    private int determineSourceTileWidth(int rasterWidth, int tileWidth,
                                         int rightBorderExtension, int leftBorderExtension) {
        return determineSourceTileSize(rasterWidth, tileWidth, rightBorderExtension, leftBorderExtension);
    }

    private int determineSourceTileHeight(int rasterHeight, int tileHeight,
                                          int topBorderExtension, int bottomBorderExtension) {
        return determineSourceTileSize(rasterHeight, tileHeight, topBorderExtension, bottomBorderExtension);
    }

    int determineSourceTileSize(int rasterSize, int tileSize, int borderExtension1, int borderExtension2) {
        int maxTileSize = Math.min(MAX_TILE_DIMENSION - borderExtension1 - borderExtension2, 2 * tileSize);
        final int minNumTiles = (int) Math.floor(rasterSize / maxTileSize * 1.);
        int bestTileSize = Integer.MIN_VALUE;
        int smallestDiff = Integer.MAX_VALUE;
        for (int i = minNumTiles; i >= 1; i++) {
            if (rasterSize % i == 0) {
                final int candidateDiff = Math.abs(tileSize - rasterSize / i);
                if (candidateDiff > smallestDiff) {
                    break;
                }
                bestTileSize = rasterSize / i;
                smallestDiff = Math.abs(tileSize - bestTileSize);
            }
        }
        if (smallestDiff < Integer.MAX_VALUE) {
            return bestTileSize;
        }
        return maxTileSize;
    }

    double determineSearchBorderRadius(double spatialResolution, double maxSunZenith) {
        final double maxCloudDistance = MAX_CLOUD_HEIGHT / Math.tan(Math.toRadians(90. - maxSunZenith));
        return maxCloudDistance / spatialResolution;
    }

    int getRightBorderExtension(double searchBorderRadius, double minSunAzimuth, double maxSunAzimuth) {
        return (int) Math.ceil(searchBorderRadius * Math.max(0, Math.max(Math.sin(Math.toRadians(minSunAzimuth)),
                                                                         Math.sin(Math.toRadians(maxSunAzimuth)))));
    }

    int getLeftBorderExtension(double searchBorderRadius, double minSunAzimuth, double maxSunAzimuth) {
        return (int) Math.ceil(searchBorderRadius * Math.max(0, Math.max(Math.sin(Math.toRadians(minSunAzimuth)) * -1,
                                                                         Math.sin(Math.toRadians(maxSunAzimuth)) * -1)));
    }

    int getTopBorderExtension(double searchBorderRadius, double minSunAzimuth, double maxSunAzimuth) {
        return (int) Math.ceil(searchBorderRadius *
                                       Math.max(0, Math.max(Math.cos(Math.toRadians(minSunAzimuth)), Math.cos(Math.toRadians(maxSunAzimuth)))));
    }

    int getBottomBorderExtension(double searchBorderRadius, double minSunAzimuth, double maxSunAzimuth) {
        return (int) Math.ceil(searchBorderRadius * Math.max(0, Math.max(Math.cos(Math.toRadians(minSunAzimuth)) * -1,
                                                                         Math.cos(Math.toRadians(maxSunAzimuth)) * -1)));
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        Rectangle sourceRectangle = new Rectangle(targetRectangle);
        sourceRectangle.grow(searchBorderRadius, searchBorderRadius);

        Tile sourceTileSunZenith = getSourceTile(sourceSunZenith, sourceRectangle, new BorderExtenderConstant(new double[]{Double.NaN}));
        Tile sourceTileSunAzimuth = getSourceTile(sourceSunAzimuth, sourceRectangle, new BorderExtenderConstant(new double[]{Double.NaN}));
        Tile sourceTileAltitude = getSourceTile(sourceAltitude, sourceRectangle, new BorderExtenderConstant(new double[]{Double.NaN}));

        Tile sourceTileFlag1 = getSourceTile(sourceBandFlag1, sourceRectangle, new BorderExtenderConstant(new double[]{Double.NaN}));
        Tile sourceTileFlag2 = null;
        if (sourceBandFlag2 != null) {
            sourceTileFlag2 = getSourceTile(sourceBandFlag2, sourceRectangle, new BorderExtenderConstant(new double[]{Double.NaN}));
        }
        Tile sourceTileClusterA = getSourceTile(sourceBandClusterA, sourceRectangle, new BorderExtenderConstant(new double[]{Double.NaN}));

        Tile targetTileCloudShadow = targetTiles.get(targetBandCloudShadow);
        Tile targetTileTestA = targetTiles.get(targetBandTestA);
        Tile targetTileTestB = targetTiles.get(targetBandTestB);
        Tile targetTileTestC = targetTiles.get(targetBandTestC);
        Tile targetTileTestD = targetTiles.get(targetBandTestD);

        int sourceWidth = sourceRectangle.width;
        int sourceHeight = sourceRectangle.height;
        int sourceLength = sourceRectangle.width * sourceRectangle.height;

        final int[] flagArray = new int[sourceLength];
        final int[] cloudShadowArray = new int[sourceLength];
        //will be filled in SegmentationCloudClass Arrays.fill(cloudIdArray, ....);
        final int[] cloudIDArray = new int[sourceLength];
        final int[] cloudShadowIDArray = new int[sourceLength];
        final int[] cloudLongShadowIDArray = new int[sourceLength];

        final float[] sourceSunZenith = sourceTileSunZenith.getSamplesFloat();
        final float[] sourceSunAzimuth = sourceTileSunAzimuth.getSamplesFloat();
        final float[] sourceAltitude = sourceTileAltitude.getSamplesFloat();
        final float[] sourceClusterA = sourceTileClusterA.getSamplesFloat();
        final float[] sourceClusterB = sourceTileClusterA.getSamplesFloat();

        float[] sourceLatitudes;
        float[] sourceLongitudes;
        if (sourceLatitude != null && sourceLongitude != null) {
            Tile sourceTileLatitude = getSourceTile(sourceLatitude, sourceRectangle,
                                                    new BorderExtenderConstant(new double[]{Double.NaN}));
            Tile sourceTileLongitude = getSourceTile(sourceLongitude, sourceRectangle,
                                                     new BorderExtenderConstant(new double[]{Double.NaN}));
            sourceLatitudes = sourceTileLatitude.getSamplesFloat();
            sourceLongitudes = sourceTileLongitude.getSamplesFloat();
        } else if (getSourceProduct().getSceneGeoCoding() instanceof CrsGeoCoding) {
            sourceLatitudes = new float[(int) (sourceRectangle.getWidth() * sourceRectangle.getHeight())];
            sourceLongitudes = new float[(int) (sourceRectangle.getWidth() * sourceRectangle.getHeight())];
            ((CrsGeoCoding) getSourceProduct().getSceneGeoCoding()).getPixels((int) sourceRectangle.getMinX(),
                                                                              (int) sourceRectangle.getMinY(),
                                                                              (int) sourceRectangle.getWidth(),
                                                                              (int) sourceRectangle.getHeight(),
                                                                              sourceLatitudes,
                                                                              sourceLongitudes);
        } else {
            throw new OperatorException("Could not determine geographic position");
        }

        FlagDetector flagDetector = new FlagDetectorSentinel2(sourceTileFlag1, sourceTileFlag2, sourceRectangle);

        PreparationMaskBand.prepareMaskBand(
                s2ClassifProduct.getSceneRasterWidth(),
                s2ClassifProduct.getSceneRasterHeight(),
                sourceRectangle,
                flagArray,
                flagDetector);

        int counterTable = SegmentationCloud.computeCloudID(sourceWidth, sourceHeight, flagArray, cloudIDArray);

        //todo assessment of the order of processing steps
            /*double solarFluxNir = sourceBandHazeNir.getSolarFlux();
            HazeDetection detectHaze = new HazeDetection();
            detectHaze.calculatePotentialHazeAreas(sourceRectangle, sourceTileHazeBlue,
                    sourceTileHazeRed,
                    sourceTileHazeNir,
                    sourceWidth,
                    sourceHeight,
                    flagArray,
                    solarFluxNir);   */

        //makeFilledBand(flagArray, targetTileCloudShadow.getRectangle(), targetTileCloudShadow, searchBorderRadius);

        if (counterTable != 0) {
            int[][] cloudShadowIdBorderRectangle;


            if (productCentralComputation) {
                cloudShadowIdBorderRectangle =
                        PotentialCloudShadowAreasPathCentralPixel.
                                makedCloudShadowArea(s2ClassifProduct, targetProduct, sourceRectangle,
                                                     targetRectangle, sourceSunZenith, sourceSunAzimuth,
                                                     sourceLatitudes, sourceLongitudes, sourceAltitude,
                                                     flagArray, cloudShadowArray,
                                                     cloudIDArray, cloudShadowIDArray, counterTable);
            } else {
                cloudShadowIdBorderRectangle =
                        PotentialCloudShadowAreas.
                                makedCloudShadowArea(s2ClassifProduct, targetProduct, sourceRectangle,
                                                     targetRectangle, sourceSunZenith, sourceSunAzimuth,
                                                     sourceLatitudes, sourceLongitudes, sourceAltitude,
                                                     flagArray, cloudShadowArray,
                                                     cloudIDArray, cloudShadowIDArray, counterTable);
            }


            AnalyzeCloudShadowIDAreas.identifyCloudShadowArea(s2ClassifProduct, sourceRectangle,
                                                              sourceClusterA, sourceClusterB,
                                                              flagArray, cloudShadowIDArray,
                                                              cloudLongShadowIDArray,
                                                              cloudShadowIdBorderRectangle,
                                                              counterTable);


            GrowingCloudShadow.computeCloudShadowBorder(sourceWidth, sourceHeight, flagArray);

            makeFilledBand(flagArray, targetTileCloudShadow.getRectangle(), targetTileCloudShadow, searchBorderRadius);
            makeFilledBand(cloudShadowArray, targetTileTestA.getRectangle(), targetTileTestA, searchBorderRadius);
            makeFilledBand(cloudIDArray, targetTileTestB.getRectangle(), targetTileTestB, searchBorderRadius);
            makeFilledBand(cloudShadowIDArray, targetTileTestC.getRectangle(), targetTileTestC, searchBorderRadius);
            makeFilledBand(cloudLongShadowIDArray, targetTileTestD.getRectangle(), targetTileTestD, searchBorderRadius);
        }
    }

    private void attachIndexCoding(Band targetBandCloudShadow) {
        IndexCoding cloudCoding = new IndexCoding("cloudCoding");
        cloudCoding.addIndex("nothing", 0, "");
        cloudCoding.addIndex("ocean", 1, "");
        cloudCoding.addIndex("land", 10, "");
        cloudCoding.addIndex("cloudShadow", 100, "");
        cloudCoding.addIndex("ocean_cloud_shadow", 101, "");
        cloudCoding.addIndex("land_cloud_shadow", 110, "");
        cloudCoding.addIndex("cloud", 1000, "");
        cloudCoding.addIndex("ocean_cloud", 1001, "");
        cloudCoding.addIndex("land_cloud", 1010, "");
        cloudCoding.addIndex("water_pot_haze", 5001, "");
        cloudCoding.addIndex("land_pot_haze", 5010, "");
        cloudCoding.addIndex("invalid", 10000, "");
        targetBandCloudShadow.setSampleCoding(cloudCoding);
        targetBandCloudShadow.getProduct().getIndexCodingGroup().add(cloudCoding);

        final int sampleCount = cloudCoding.getSampleCount();
        ColorPaletteDef.Point[] points = new ColorPaletteDef.Point[sampleCount];
        points[0] = new ColorPaletteDef.Point(0, Color.WHITE);
        points[1] = new ColorPaletteDef.Point(1, Color.BLUE);
        points[2] = new ColorPaletteDef.Point(10, Color.GREEN);
        points[3] = new ColorPaletteDef.Point(100, Color.DARK_GRAY);
        points[4] = new ColorPaletteDef.Point(101, Color.DARK_GRAY);
        points[5] = new ColorPaletteDef.Point(110, Color.DARK_GRAY);
        points[6] = new ColorPaletteDef.Point(1000, Color.YELLOW);
        points[7] = new ColorPaletteDef.Point(1001, Color.YELLOW);
        points[8] = new ColorPaletteDef.Point(1010, Color.YELLOW);
        points[9] = new ColorPaletteDef.Point(5001, Color.CYAN);
        points[10] = new ColorPaletteDef.Point(5010, Color.LIGHT_GRAY);
        points[11] = new ColorPaletteDef.Point(10000, Color.RED);
        targetBandCloudShadow.setImageInfo(new ImageInfo(new ColorPaletteDef(points, points.length)));
    }

    private static void makeFilledBand(int[] inputData, Rectangle targetRectangle, Tile targetTileOutputBand, int mkr) {

        int xLocation = targetRectangle.x;
        int yLocation = targetRectangle.y;
        int inputDataWidth = targetRectangle.width + 2 * mkr;
        int inputDataHeight = targetRectangle.height + 2 * mkr;


        for (int y = mkr; y < inputDataHeight - mkr; y++) {
            for (int x = mkr; x < inputDataWidth - mkr; x++) {
                targetTileOutputBand.setSample(x - mkr + xLocation, y - mkr + yLocation, inputData[y * (inputDataWidth) + x]);
            }
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(S2IdepixCloudShadowOp.class);
        }
    }

}
