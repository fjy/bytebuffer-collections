package com.metamx.collections.spatial.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Floats;
import com.metamx.collections.spatial.ImmutableNode;
import com.metamx.collections.spatial.ImmutablePoint;

import java.nio.ByteBuffer;
import java.util.List;

/**
 * Created by jinsheng on 16/4/26.
 */
public class PolygonBound extends RectangularBound
{

    private static final byte CACHE_TYPE_ID = 0x0;
    private final float[] abscissa;
    private final float[] ordinate;

    private static float[] getMinCoords(float[] abscissa, float[] ordinate)
    {
        float[] retVal = new float[2];
        retVal[0] = abscissa[0];
        retVal[1] = ordinate[0];


        for (int i = 1; i < abscissa.length; i ++)
        {
            if (abscissa[i] < retVal[0])
                retVal[0] = abscissa[i];

            if (ordinate[i] < retVal[1])
                retVal[1] = ordinate[i];

        }

        return retVal;

    }

    private static float[] getMaxCoords(float[] abscissa, float[] ordinate)
    {
        float[] retVal = new float[2];
        retVal[0] = abscissa[0];
        retVal[1] = ordinate[0];
        for (int i = 1; i < abscissa.length; i ++)
        {

            if (abscissa[i] > retVal[0])
                retVal[0] = abscissa[i];

            if (ordinate[i] > retVal[1])
                retVal[1] = ordinate[i];
        }

        return retVal;
    }

    @JsonCreator
    public PolygonBound(
        @JsonProperty("abscissa") float[] abscissa,
        @JsonProperty("ordinate") float[] ordinate,
        @JsonProperty("limit") int limit
    )
    {
        super(getMinCoords(abscissa, ordinate), getMaxCoords(abscissa, ordinate), limit);
        Preconditions.checkArgument(abscissa.length == ordinate.length);
        Preconditions.checkArgument(abscissa.length >= 3);
        //todo check if is a legal polygon
        this.abscissa = abscissa;
        this.ordinate = ordinate;
    }

    public PolygonBound(float[] abscissa, float[] ordinate)
    {
        this(abscissa, ordinate, 0);
    }


    @Override
    public boolean contains(float[] coords)
    {
        int polyCorners = abscissa.length;
        int j = polyCorners - 1;
        boolean oddNodes = false;
        for (int i = 0; i < polyCorners; i ++)
        {
            if ((ordinate[i]< coords[1] && ordinate[j]>=coords[1]
                 ||   ordinate[j]< coords[1] && ordinate[i]>=coords[1])
                 &&  (abscissa[i]<=coords[0] || abscissa[j]<=coords[0]))
            {
                if (abscissa[i]+(coords[1]-ordinate[i])/(ordinate[j]-ordinate[i])*(abscissa[j]-abscissa[i])<coords[0])
                {
                    oddNodes=!oddNodes;
                }
            }
            j = i;
        }
        return oddNodes;
    }

    @Override
    public Iterable<ImmutablePoint> filter(Iterable<ImmutablePoint> points)
    {
        return Iterables.filter(
            points,
            new Predicate<ImmutablePoint>()
            {
                @Override
                public boolean apply(ImmutablePoint immutablePoint)
                {
                    return contains(immutablePoint.getCoords());
                }
            }
        );
    }
}
