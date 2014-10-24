package com.metamx.collections.spatial.Concise;

import com.google.common.base.Stopwatch;
import com.google.common.base.Throwables;
import com.google.common.collect.Iterables;
import com.google.common.collect.Sets;
import com.metamx.collections.spatial.ImmutableRTree;
import com.metamx.collections.spatial.RTree;
import com.metamx.collections.spatial.CompressedBitmaps.GenericBitmap;
import com.metamx.collections.spatial.CompressedBitmaps.ImmutableGenericBitmap;
import com.metamx.collections.spatial.CompressedBitmaps.WrappedConciseBitmap;
import com.metamx.collections.spatial.CompressedBitmaps.WrappedImmutableConciseBitmap;
import com.metamx.collections.spatial.CompressedBitmaps.WrappedImmutableRoaringBitmap;
import com.metamx.collections.spatial.CompressedBitmaps.WrappedRoaringBitmap;
import com.metamx.collections.spatial.search.RadiusBound;
import com.metamx.collections.spatial.search.RectangularBound;
import com.metamx.collections.spatial.split.LinearGutmanSplitStrategy;

import junit.framework.Assert;

import org.junit.Test;
import org.roaringbitmap.IntIterator;
import org.roaringbitmap.RoaringBitmap;
import org.roaringbitmap.buffer.ImmutableRoaringBitmap;
import org.roaringbitmap.buffer.MutableRoaringBitmap;

import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Random;
import java.util.Set;

/**
 */
public class ImmutableRTreeTest
{
    @Test
    public void testToAndFromByteBuffer()
    {
    	WrappedConciseBitmap cs = new WrappedConciseBitmap();
        RTree tree = new RTree(2, new LinearGutmanSplitStrategy(0, 50, cs), cs);
        tree.insert(new float[]{0, 0}, 1);
        tree.insert(new float[]{1, 1}, 2);
        tree.insert(new float[]{2, 2}, 3);
        tree.insert(new float[]{3, 3}, 4);
        tree.insert(new float[]{4, 4}, 5);
        ImmutableRTree firstTree = ImmutableRTree.newImmutableFromMutable(tree);
        ByteBuffer buffer = ByteBuffer.wrap(firstTree.toBytes());
        ImmutableGenericBitmap ics = cs.toImmutableGenericBitmap();
        ImmutableRTree secondTree = new ImmutableRTree(buffer, ics);
        Iterable<ImmutableGenericBitmap> points = secondTree.search(new RadiusBound(new float[]{0, 0}, 10));
        ImmutableGenericBitmap finalSet = ics.union(points);
        
        Assert.assertTrue(finalSet.size() >= 5);

        Set<Integer> expected = Sets.newHashSet(1, 2, 3, 4, 5);
        IntIterator iter = finalSet.iterator();
        while (iter.hasNext()) {
            Assert.assertTrue(expected.contains(iter.next()));
        }
    }

    @Test
    public void testSearchNoSplit()
    {
    	WrappedConciseBitmap cs = new WrappedConciseBitmap();
        RTree tree = new RTree(2, new LinearGutmanSplitStrategy(0, 50, cs), cs);
        tree.insert(new float[]{0, 0}, 1);
        tree.insert(new float[]{10, 10}, 10);
        tree.insert(new float[]{1, 3}, 2);
        tree.insert(new float[]{27, 34}, 20);
        tree.insert(new float[]{106, 19}, 30);
        tree.insert(new float[]{4, 2}, 3);
        tree.insert(new float[]{5, 0}, 4);
        tree.insert(new float[]{4, 72}, 40);
        tree.insert(new float[]{-4, -3}, 5);
        tree.insert(new float[]{119, -78}, 50);

        Assert.assertEquals(tree.getRoot().getChildren().size(), 10);
        
        ImmutableGenericBitmap ics = cs.toImmutableGenericBitmap();
        ImmutableRTree searchTree = ImmutableRTree.newImmutableFromMutable(tree);
        Iterable<ImmutableGenericBitmap> points = searchTree.search(new RadiusBound(new float[]{0, 0}, 5));
        ImmutableGenericBitmap finalSet = ics.union(points);
        
        Assert.assertTrue(finalSet.size() >= 5);

        Set<Integer> expected = Sets.newHashSet(1, 2, 3, 4, 5);
        IntIterator iter = finalSet.iterator();
        while (iter.hasNext()) {
            Assert.assertTrue(expected.contains(iter.next()));
        }
    }

    @Test
    public void testSearchWithSplit()
    {
    	WrappedConciseBitmap cs = new WrappedConciseBitmap();
        RTree tree = new RTree(2, new LinearGutmanSplitStrategy(0, 50, cs), cs);
        tree.insert(new float[]{0, 0}, 1);
        tree.insert(new float[]{1, 3}, 2);
        tree.insert(new float[]{4, 2}, 3);
        tree.insert(new float[]{5, 0}, 4);
        tree.insert(new float[]{-4, -3}, 5);

        Random rand = new Random();
        for (int i = 0; i < 95; i++) {
            tree.insert(
                    new float[]{(float) (rand.nextDouble() * 10 + 10.0), (float) (rand.nextDouble() * 10 + 10.0)},
                    i
            );
        }

        ImmutableGenericBitmap ics = cs.toImmutableGenericBitmap();
        ImmutableRTree searchTree = ImmutableRTree.newImmutableFromMutable(tree);
        Iterable<ImmutableGenericBitmap> points = searchTree.search(new RadiusBound(new float[]{0, 0}, 5));
        ImmutableGenericBitmap finalSet = ics.union(points);
        
        Assert.assertTrue(finalSet.size() >= 5);

        Set<Integer> expected = Sets.newHashSet(1, 2, 3, 4, 5);
        IntIterator iter = finalSet.iterator();
        while (iter.hasNext()) {
            Assert.assertTrue(expected.contains(iter.next()));
        }
    }

    @Test
    public void testSearchWithSplit2()
    {
    	WrappedConciseBitmap cs = new WrappedConciseBitmap();
        RTree tree = new RTree(2, new LinearGutmanSplitStrategy(0, 50, cs), cs);
        tree.insert(new float[]{0.0f, 0.0f}, 0);
        tree.insert(new float[]{1.0f, 3.0f}, 1);
        tree.insert(new float[]{4.0f, 2.0f}, 2);
        tree.insert(new float[]{7.0f, 3.0f}, 3);
        tree.insert(new float[]{8.0f, 6.0f}, 4);

        Random rand = new Random();
        for (int i = 5; i < 5000; i++) {
            tree.insert(
                    new float[]{(float) (rand.nextDouble() * 10 + 10.0), (float) (rand.nextDouble() * 10 + 10.0)},
                    i
            );
        }

        ImmutableGenericBitmap ics = cs.toImmutableGenericBitmap();
        ImmutableRTree searchTree = ImmutableRTree.newImmutableFromMutable(tree);
        Iterable<ImmutableGenericBitmap> points = searchTree.search(
                new RectangularBound(
                    new float[]{0, 0},
                    new float[]{9, 9}
                )
            );
        ImmutableGenericBitmap finalSet = ics.union(points);
       
        Assert.assertTrue(finalSet.size() >= 5);

        Set<Integer> expected = Sets.newHashSet(0, 1, 2, 3, 4);
        IntIterator iter = finalSet.iterator();
        while (iter.hasNext()) {
            Assert.assertTrue(expected.contains(iter.next()));
        }
    }

    @Test
    public void testSearchWithSplit3()
    {
    	WrappedConciseBitmap cs = new WrappedConciseBitmap();
        RTree tree = new RTree(2, new LinearGutmanSplitStrategy(0, 50, cs), cs);
        tree.insert(new float[]{0.0f, 0.0f}, 0);
        tree.insert(new float[]{1.0f, 3.0f}, 1);
        tree.insert(new float[]{4.0f, 2.0f}, 2);
        tree.insert(new float[]{7.0f, 3.0f}, 3);
        tree.insert(new float[]{8.0f, 6.0f}, 4);

        Random rand = new Random();
        for (int i = 5; i < 5000; i++) {
            tree.insert(
                    new float[]{(float) (rand.nextFloat() * 10 + 10.0), (float) (rand.nextFloat() * 10 + 10.0)},
                    i
            );
        }
        ImmutableGenericBitmap ics = cs.toImmutableGenericBitmap();
        ImmutableRTree searchTree = ImmutableRTree.newImmutableFromMutable(tree);
        Iterable<ImmutableGenericBitmap> points = searchTree.search(
                new RadiusBound(new float[]{0.0f, 0.0f}, 5)
        	    );
        ImmutableGenericBitmap finalSet = ics.union(points);
       
        Assert.assertTrue(finalSet.size() >= 3);

        Set<Integer> expected = Sets.newHashSet(0, 1, 2);
        IntIterator iter = finalSet.iterator();
        while (iter.hasNext()) {
            Assert.assertTrue(expected.contains(iter.next()));
        }
    }
    

    @Test
    public void testSearchWithSplitLimitedBound()
    {
    	WrappedConciseBitmap cs = new WrappedConciseBitmap();
        RTree tree = new RTree(2, new LinearGutmanSplitStrategy(0, 50, cs), cs);
        tree.insert(new float[]{0, 0}, 1);
        tree.insert(new float[]{1, 3}, 2);
        tree.insert(new float[]{4, 2}, 3);
        tree.insert(new float[]{5, 0}, 4);
        tree.insert(new float[]{-4, -3}, 5);

        Random rand = new Random();
        for (int i = 0; i < 4995; i++) {
            tree.insert(
                    new float[]{(float) (rand.nextDouble() * 10 + 10.0), (float) (rand.nextDouble() * 10 + 10.0)},
                    i
            );
        }

        ImmutableGenericBitmap ics = new WrappedImmutableConciseBitmap(null);
        ImmutableRTree searchTree = ImmutableRTree.newImmutableFromMutable(tree);
        Iterable<ImmutableGenericBitmap> points = searchTree.search(new RadiusBound(new float[]{0, 0}, 5, 2));
        ImmutableGenericBitmap finalSet = ics.union(points);
       
        Assert.assertTrue(finalSet.size() >= 5);

        Set<Integer> expected = Sets.newHashSet(1, 2, 3, 4, 5);
        IntIterator iter = finalSet.iterator();
        while (iter.hasNext()) {
            Assert.assertTrue(expected.contains(iter.next()));
        }
    }

    //@Test
    
    public void showBenchmarks()
    {
        final int start = 1;
        final int factor = 10;
        final int end = 10000000;
        final int radius = 10;

        for (int numPoints = start; numPoints <= end; numPoints *= factor) {
            try {
            	WrappedConciseBitmap cs = new WrappedConciseBitmap();
                RTree tree = new RTree(2, new LinearGutmanSplitStrategy(0, 50, cs), cs);

                Stopwatch stopwatch = new Stopwatch().start();
                Random rand = new Random();
                for (int i = 0; i < numPoints; i++) {
                    tree.insert(new float[]{(float) (rand.nextDouble() * 100), (float) (rand.nextDouble() * 100)}, i);
                }
                long stop = stopwatch.elapsedMillis();
                System.out.printf("[%,d]: insert = %,d ms%n", numPoints, stop);

                stopwatch.reset().start();
                ImmutableGenericBitmap irb = cs.toImmutableGenericBitmap();
                ImmutableRTree searchTree = ImmutableRTree.newImmutableFromMutable(tree);
                stop = stopwatch.elapsedMillis();
                System.out.printf("[%,d]: size = %,d bytes%n", numPoints, searchTree.toBytes().length);
                System.out.printf("[%,d]: buildImmutable = %,d ms%n", numPoints, stop);

                stopwatch.reset().start();

                Iterable<ImmutableGenericBitmap> points = searchTree.search(
                        new RectangularBound(
                                new float[]{40, 40},
                                new float[]{60, 60},
                                100
                            )
                        );

                Iterables.size(points);
                stop = stopwatch.elapsedMillis();

                System.out.printf("[%,d]: search = %,dms%n", numPoints, stop);

                stopwatch.reset().start();

                ImmutableGenericBitmap finalSet = irb.union(points);

                stop = stopwatch.elapsedMillis();
                System.out.printf("[%,d]: union of %,d points in %,d ms%n", numPoints, finalSet.size(), stop);
            }
            catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }

    //@Test
    public void showBenchmarksBoundWithLimits()
    {
        //final int start = 1;
        final int start = 10000000;
        final int factor = 10;
        final int end = 10000000;
        //final int end = 10;

        for (int numPoints = start; numPoints <= end; numPoints *= factor) {
            try {
            	GenericBitmap cs = new WrappedConciseBitmap();
                RTree tree = new RTree(2, new LinearGutmanSplitStrategy(0, 50,cs), cs);

                Stopwatch stopwatch = new Stopwatch().start();
                Random rand = new Random();
                for (int i = 0; i < numPoints; i++) {
                    tree.insert(new float[]{(float) (rand.nextDouble() * 100), (float) (rand.nextDouble() * 100)}, i);
                }
                long stop = stopwatch.elapsedMillis();
                System.out.printf("[%,d]: insert = %,d ms%n", numPoints, stop);

                stopwatch.reset().start();
                ImmutableGenericBitmap ics = cs.toImmutableGenericBitmap();
                ImmutableRTree searchTree = ImmutableRTree.newImmutableFromMutable(tree);
                stop = stopwatch.elapsedMillis();
                System.out.printf("[%,d]: size = %,d bytes%n", numPoints, searchTree.toBytes().length);
                System.out.printf("[%,d]: buildImmutable = %,d ms%n", numPoints, stop);

                stopwatch.reset().start();

                Iterable<ImmutableGenericBitmap> points = searchTree.search(
                        new RectangularBound(
                                new float[]{40, 40},
                                new float[]{60, 60},
                                100
                        )
                );

                Iterables.size(points);
                stop = stopwatch.elapsedMillis();

                System.out.printf("[%,d]: search = %,dms%n", numPoints, stop);

                stopwatch.reset().start();

                ImmutableGenericBitmap finalSet = ics.union(points);

                stop = stopwatch.elapsedMillis();
                System.out.printf("[%,d]: union of %,d points in %,d ms%n", numPoints, finalSet.size(), stop);
            }
            catch (Exception e) {
                throw Throwables.propagate(e);
            }
        }
    }
}
