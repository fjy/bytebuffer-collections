package com.metamx.collections.spatial.search;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.common.base.Preconditions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Floats;
import com.metamx.collections.spatial.ImmutableNode;
import com.metamx.collections.spatial.ImmutablePoint;
import com.metamx.collections.spatial.RoaringImmutableNode;
import com.metamx.collections.spatial.RoaringImmutablePoint;

import java.nio.ByteBuffer;

/**
 */
public class RoaringRectangularBound implements RoaringBound
{
    private static final byte CACHE_TYPE_ID = 0x0;

  private final float[] minCoords;
  private final float[] maxCoords;
  private final int limit;
  private final int numDims;

  @JsonCreator
  public RoaringRectangularBound(
          @JsonProperty("minCoords") float[] minCoords,
          @JsonProperty("maxCoords") float[] maxCoords,
          @JsonProperty("limit") int limit
  )
  {
    Preconditions.checkArgument(minCoords.length == maxCoords.length);

    this.numDims = minCoords.length;

    this.minCoords = minCoords;
    this.maxCoords = maxCoords;
    this.limit = limit;
  }

  public RoaringRectangularBound(
          float[] minCoords,
          float[] maxCoords
  )
  {
    this(minCoords, maxCoords, 0);
  }

  @JsonProperty
  public float[] getMinCoords()
  {
    return minCoords;
  }

  @JsonProperty
  public float[] getMaxCoords()
  {
    return maxCoords;
  }

  @JsonProperty
  public int getLimit()
  {
    return limit;
  }

  @Override
  public int getNumDims()
  {
    return numDims;
  }

  @Override
  public boolean overlaps(RoaringImmutableNode node)
  {
    final float[] nodeMinCoords = node.getMinCoordinates();
    final float[] nodeMaxCoords = node.getMaxCoordinates();

    for (int i = 0; i < numDims; i++) {
      if (nodeMaxCoords[i] < minCoords[i] || nodeMinCoords[i] > maxCoords[i]) {
        return false;
      }
    }

    return true;
  }

  @Override
  public boolean contains(float[] coords)
  {
    for (int i = 0; i < numDims; i++) {
      if (coords[i] < minCoords[i] || coords[i] > maxCoords[i]) {
        return false;
      }
    }

    return true;
  }

  @Override
  public Iterable<RoaringImmutablePoint> filter(Iterable<RoaringImmutablePoint> points)
  {
    return Iterables.filter(
        points,
        new Predicate<RoaringImmutablePoint>()
        {
          @Override
          public boolean apply(RoaringImmutablePoint immutablePoint)
          {
            return contains(immutablePoint.getCoords());
          }
        }
    );
  }

  @Override
  public byte[] getCacheKey()
  {
    ByteBuffer minCoordsBuffer = ByteBuffer.allocate(minCoords.length * Floats.BYTES);
    minCoordsBuffer.asFloatBuffer().put(minCoords);
    final byte[] minCoordsCacheKey = minCoordsBuffer.array();

    ByteBuffer maxCoordsBuffer = ByteBuffer.allocate(maxCoords.length * Floats.BYTES);
    maxCoordsBuffer.asFloatBuffer().put(maxCoords);
    final byte[] maxCoordsCacheKey = maxCoordsBuffer.array();

    final ByteBuffer cacheKey = ByteBuffer.allocate(1 + minCoordsCacheKey.length + maxCoordsCacheKey.length)
                                          .put(minCoordsCacheKey)
                                          .put(maxCoordsCacheKey)
                                          .put(CACHE_TYPE_ID);
    return cacheKey.array();
  }
}
