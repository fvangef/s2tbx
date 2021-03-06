package org.esa.s2tbx.dataio.s2.tiles;

import org.esa.s2tbx.dataio.s2.S2Config;
import org.esa.snap.core.image.AbstractMatrixMosaicSubsetMultiLevelSource;
import org.esa.snap.core.image.MosaicMatrix;
import org.esa.snap.core.image.UncompressedTileOpImageCallback;
import org.esa.snap.core.util.ImageUtils;

import javax.media.jai.*;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.RenderedImage;

/**
 * Created by jcoravu on 9/1/2020.
 */
public class TileIndexMultiLevelSource extends AbstractMatrixMosaicSubsetMultiLevelSource implements UncompressedTileOpImageCallback<TileIndexBandMatrixCell> {

    private final Double mosaicOpSourceThreshold;
    private final double mosaicOpBackgroundValue;

    public TileIndexMultiLevelSource(int levelCount, MosaicMatrix mosaicMatrix, Rectangle imageMatrixReadBounds, Dimension preferredTileSize,
                                     AffineTransform imageToModelTransform, Double mosaicOpSourceThreshold, double mosaicOpBackgroundValue) {

        super(levelCount, mosaicMatrix, imageMatrixReadBounds, preferredTileSize, imageToModelTransform);

        this.mosaicOpSourceThreshold = mosaicOpSourceThreshold; // the threshold value may be null
        this.mosaicOpBackgroundValue = mosaicOpBackgroundValue;
    }

    @Override
    public PlanarImage buildTileOpImage(Rectangle imageCellReadBounds, int level, Point tileOffset, Dimension tileSize, TileIndexBandMatrixCell matrixCell) {
        int levelImageTileWidth = ImageUtils.computeLevelSize(tileSize.width, level);
        int levelImageTileHeight = ImageUtils.computeLevelSize(tileSize.height, level);
        return ConstantDescriptor.create((float)levelImageTileWidth, (float)levelImageTileHeight, new Short[]{matrixCell.getBandValue()}, null);
    }

    @Override
    protected java.util.List<RenderedImage> buildMatrixCellTileImages(int level, Rectangle imageCellReadBounds, float cellTranslateLevelOffsetX, float cellTranslateLevelOffsetY,
                                                                      MosaicMatrix.MatrixCell matrixCell) {

        TileIndexBandMatrixCell tileIndexBandMatrixCell = (TileIndexBandMatrixCell)matrixCell;
        Dimension uncompressedTileSize = new Dimension(imageCellReadBounds.width, imageCellReadBounds.height);
        return buildUncompressedTileImages(level, imageCellReadBounds, uncompressedTileSize, cellTranslateLevelOffsetX, cellTranslateLevelOffsetY, this, tileIndexBandMatrixCell);
    }

    @Override
    protected double[] getMosaicOpBackgroundValues() {
        return new double[]{this.mosaicOpBackgroundValue};
    }

    @Override
    protected double[][] getMosaicOpSourceThreshold() {
        if (this.mosaicOpSourceThreshold == null) {
            return super.getMosaicOpSourceThreshold();
        }
        return new double[][]{ {this.mosaicOpSourceThreshold.doubleValue()} };
    }
}
