/*
 *
 *  * Copyright (C) 2016 CS ROMANIA
 *  *
 *  * This program is free software; you can redistribute it and/or modify it
 *  * under the terms of the GNU General Public License as published by the Free
 *  * Software Foundation; either version 3 of the License, or (at your option)
 *  * any later version.
 *  * This program is distributed in the hope that it will be useful, but WITHOUT
 *  * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 *  * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 *  * more details.
 *  *
 *  * You should have received a copy of the GNU General Public License along
 *  *  with this program; if not, see http://www.gnu.org/licenses/
 *
 */

package org.esa.s2tbx.radiometry;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s2tbx.radiometry.annotations.BandParameter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;

import java.awt.*;
import java.util.Map;

@OperatorMetadata(
        alias = "Ndi45Op",
        label = "NDI45 Processor",
        version="1.0",
        category = "Optical/Thematic Land Processing/Vegetation Radiometric Indices",
        description = "Normalized Difference Index using bands 4 and 5",
        authors = "Dragos Mihailescu",
        copyright = "Copyright (C) 2016 by CS ROMANIA")
public class Ndi45Op extends BaseIndexOp{

    // constants
    public static final String BAND_NAME = "ndi45";

    @Parameter(label = "Red (B4) factor", defaultValue = "1.0F", description = "The value of the red source band (B4) is multiplied by this value.")
    private float redB4Factor;

    @Parameter(label = "Red (B5) factor", defaultValue = "1.0F", description = "The value of the red source band (B5) is multiplied by this value.")
    private float redB5Factor;

    @Parameter(label = "Red source band 4",
            description = "The red band (B4) for the NDI45 computation. If not provided, the " +
                    "operator will try to find the best fitting band.",
            rasterDataNodeType = Band.class)
    @BandParameter(minWavelength = 650, maxWavelength = 680)
    private String redSourceBand4;

    @Parameter(label = "Red source band 5",
            description = "The red band (B5) for the NDI45 computation. If not provided, the " +
                    "operator will try to find the best fitting band.",
            rasterDataNodeType = Band.class)
    @BandParameter(minWavelength = 698, maxWavelength = 713)
    private String redSourceBand5;

    public Ndi45Op() {
        this.lowValueThreshold = -1f;
        this.highValueThreshold = 1f;
    }

    @Override
    public String getBandName() {
        return BAND_NAME;
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle rectangle, ProgressMonitor pm) throws OperatorException {
        pm.beginTask("Computing NDI45", rectangle.height);
        try {
            Tile redB4Tile = getSourceTile(getSourceProduct().getBand(redSourceBand4), rectangle);
            Tile redB5Tile = getSourceTile(getSourceProduct().getBand(redSourceBand5), rectangle);

            Tile ndi45 = targetTiles.get(targetProduct.getBand(BAND_NAME));
            Tile ndi45Flags = targetTiles.get(targetProduct.getBand(FLAGS_BAND_NAME));

            float ndi45Value;

            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    final float redB5 = redB5Factor * redB5Tile.getSampleFloat(x, y);
                    final float redB4 = redB4Factor * redB4Tile.getSampleFloat(x, y);

                    ndi45Value = (redB5 - redB4) / (redB5 + redB4);

                    ndi45.setSample(x, y, computeFlag(x, y, ndi45Value, ndi45Flags));
                }
                checkForCancellation();
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(Ndi45Op.class);
        }

    }

}
