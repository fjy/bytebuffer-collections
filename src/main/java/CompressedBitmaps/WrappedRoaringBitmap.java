package CompressedBitmaps;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

import org.roaringbitmap.IntIterator;
import org.roaringbitmap.buffer.ImmutableRoaringBitmap;
import org.roaringbitmap.buffer.MutableRoaringBitmap;

public class WrappedRoaringBitmap implements GenericBitmap
{

	/**
	 * Underlying bitmap.
	 */
	public MutableRoaringBitmap core;

	/**
	 * Create a new WrappedRoaringBitmap wrapping an empty MutableRoaringBitmap
	 */
	public WrappedRoaringBitmap() {
		core = new MutableRoaringBitmap();
	}

	@Override
	public void clear() {
		core.clear();
	}

	@Override
	public void or(GenericBitmap bitmap) {
		WrappedRoaringBitmap other = (WrappedRoaringBitmap) bitmap;
		MutableRoaringBitmap othercore = other.core;
		core.or(othercore);
	}

	@Override
	public void and(GenericBitmap bitmap) {
		WrappedRoaringBitmap other = (WrappedRoaringBitmap) bitmap;
		MutableRoaringBitmap othercore = other.core;
		core.and(othercore);
	}
	

	@Override
	public void andNot(GenericBitmap bitmap) {
		WrappedRoaringBitmap other = (WrappedRoaringBitmap) bitmap;
		MutableRoaringBitmap othercore = other.core;
		core.andNot(othercore);
	}
	

	@Override
	public void xor(GenericBitmap bitmap) {
		WrappedRoaringBitmap other = (WrappedRoaringBitmap) bitmap;
		MutableRoaringBitmap othercore = other.core;
		core.xor(othercore);
	}
	
	@Override
	public int getSizeInBytes() {
		return core.serializedSizeInBytes();
	}

	@Override
	public void add(int entry) {
		core.add(entry);
	}

	@Override
	public int size() {
		return core.getCardinality();
	}

	@Override
	public void serialize(ByteBuffer buffer) {
		buffer.putInt(getSizeInBytes());
		try {
			core.serialize(new DataOutputStream(new OutputStream() {
				ByteBuffer mBB;

				OutputStream init(ByteBuffer mbb) {
					mBB = mbb;
					return this;
				}
        @Override
				public void close() {
          // unnecessary
				}

        @Override
				public void flush() {
        	// unnecessary
				}

        @Override
				public void write(int b) {
					mBB.put((byte) b);
				}

        @Override
				public void write(byte[] b) {
        	throw new RuntimeException("Should never be called");
				}

        @Override
				public void write(byte[] b, int off, int l) {
        	throw new RuntimeException("Should never be called");
				}
			}.init(buffer)));
		} catch (IOException e) {
			e.printStackTrace();// impossible in theory
		}
	}

	@Override
	public String toString() {
		return getClass().getSimpleName() + core.toString();
	}

	@Override
	public void remove(int entry) {
		core.remove(entry);
	}

	@Override
	public IntIterator iterator() {
		return core.getIntIterator();
	}

	@Override
	public boolean isEmpty() {
		return core.isEmpty();
	}

	@Override
	public ImmutableGenericBitmap union(ImmutableGenericBitmap bitmap) {
		WrappedRoaringBitmap other = (WrappedRoaringBitmap) bitmap;
		MutableRoaringBitmap othercore = other.core;
		return new WrappedImmutableRoaringBitmap(MutableRoaringBitmap.or(core, othercore));
	}

	@Override
	public ImmutableGenericBitmap intersection(ImmutableGenericBitmap bitmap) {
		WrappedRoaringBitmap other = (WrappedRoaringBitmap) bitmap;
		MutableRoaringBitmap othercore = other.core;
		return new WrappedImmutableRoaringBitmap(MutableRoaringBitmap.and(core, othercore));
	}

	@Override
	public ImmutableGenericBitmap difference(ImmutableGenericBitmap bitmap) {
		WrappedRoaringBitmap other = (WrappedRoaringBitmap) bitmap;
		MutableRoaringBitmap othercore = other.core;
		return new WrappedImmutableRoaringBitmap(MutableRoaringBitmap.andNot(core, othercore));
	}

	@Override
	public ImmutableGenericBitmap getImmutableBitmap(ByteBuffer buffer) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ImmutableGenericBitmap toImmutableGenericBitmap() {
		return new WrappedImmutableRoaringBitmap(this.core.toImmutableRoaringBitmap());
	}


}
