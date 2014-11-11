package com.metamx.collections.bitmap;

import org.roaringbitmap.IntIterator;

import java.nio.ByteBuffer;
import java.util.BitSet;

/**
 * WrappedImmutableBitSetBitmap implements ImmutableBitmap for java.util.BitSet
 */
public class WrappedImmutableBitSetBitmap implements ImmutableBitmap
{
  protected final BitSet bitmap;

  public WrappedImmutableBitSetBitmap(BitSet bitmap)
  {
    this.bitmap = bitmap;
  }

  public WrappedImmutableBitSetBitmap()
  {
    this(new BitSet());
  }

  // WARNING: the current implementation of BitSet (1.7) copies the contents of ByteBuffer to
  // on heap!
  // TODO: make a new BitSet implementation which can use ByteBuffers properly.
  public WrappedImmutableBitSetBitmap(ByteBuffer byteBuffer){
    this(BitSet.valueOf(byteBuffer));
  }

  private class BitSetIterator implements IntIterator
  {
    private int pos = -1;

    @Override
    public boolean hasNext()
    {
      return bitmap.nextSetBit(pos + 1) >= 0;
    }

    @Override
    public int next()
    {
      pos = bitmap.nextSetBit(pos + 1);
      return pos;
    }

    @Override
    public IntIterator clone()
    {
      BitSetIterator newIt = new BitSetIterator();
      newIt.pos = pos;
      return newIt;
    }
  }

  @Override
  public IntIterator iterator()
  {
    return new BitSetIterator();
  }


  @Override
  public boolean get(int value)
  {
    return bitmap.get(value);
  }

  @Override
  public int size()
  {
    return bitmap.cardinality();
  }

  @Override
  public byte[] toBytes()
  {
    return bitmap.toByteArray();
  }


  @Override
  public int compareTo(ImmutableBitmap other)
  {
    // TODO: find out what this is supposed to even do
    BitSet otherSet = ((WrappedImmutableBitSetBitmap) other).bitmap;
    int lengthCompare = Integer.compare(otherSet.length(), bitmap.length());
    if (lengthCompare != 0) {
      return lengthCompare;
    }
    return Integer.compare(otherSet.nextSetBit(0), bitmap.nextSetBit(0));
  }


  @Override
  public boolean isEmpty()
  {
    return bitmap.isEmpty();
  }

  @Override
  public ImmutableBitmap union(ImmutableBitmap otherBitmap)
  {
    WrappedBitSetBitmap retval = new WrappedBitSetBitmap((BitSet) bitmap.clone());
    retval.or((WrappedBitSetBitmap) otherBitmap);
    return retval;
  }

  @Override
  public ImmutableBitmap intersection(ImmutableBitmap otherBitmap)
  {
    WrappedBitSetBitmap retval = new WrappedBitSetBitmap((BitSet) bitmap.clone());
    retval.and((WrappedBitSetBitmap) otherBitmap);
    return retval;
  }

  @Override
  public ImmutableBitmap difference(ImmutableBitmap otherBitmap)
  {
    WrappedBitSetBitmap retval = new WrappedBitSetBitmap((BitSet) bitmap.clone());
    retval.andNot((WrappedBitSetBitmap) otherBitmap);
    return retval;
  }

}