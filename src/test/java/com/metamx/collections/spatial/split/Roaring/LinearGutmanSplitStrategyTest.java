package com.metamx.collections.spatial.split.Roaring;

import CompressedBitmaps.WrappedRoaringBitmap;

import com.metamx.collections.spatial.Node;
import com.metamx.collections.spatial.Point;
import com.metamx.collections.spatial.RTree;
import com.metamx.collections.spatial.split.LinearGutmanSplitStrategy;

import junit.framework.Assert;

import org.junit.Test;

import java.util.Random;

/**
 */
public class LinearGutmanSplitStrategyTest
{
  @Test
  public void testPickSeeds() throws Exception
  {
	WrappedRoaringBitmap rb = new WrappedRoaringBitmap();
    LinearGutmanSplitStrategy strategy = new LinearGutmanSplitStrategy(0, 50, rb);
    Node node = new Node(new float[2], new float[2], true, rb);

    node.addChild(new Point(new float[]{3, 7}, 1, rb.getEmptyWrappedBitmap()));
    node.addChild(new Point(new float[]{1, 6}, 1, rb.getEmptyWrappedBitmap()));
    node.addChild(new Point(new float[]{9, 8}, 1, rb.getEmptyWrappedBitmap()));
    node.addChild(new Point(new float[]{2, 5}, 1, rb.getEmptyWrappedBitmap()));
    node.addChild(new Point(new float[]{4, 4}, 1, rb.getEmptyWrappedBitmap()));
    node.enclose();

    Node[] groups = strategy.split(node);
    Assert.assertEquals(groups[0].getMinCoordinates()[0], 1.0f);
    Assert.assertEquals(groups[0].getMinCoordinates()[1], 4.0f);
    Assert.assertEquals(groups[1].getMinCoordinates()[0], 9.0f);
    Assert.assertEquals(groups[1].getMinCoordinates()[1], 8.0f);
  }

  @Test
  public void testNumChildrenSize()
  {
	WrappedRoaringBitmap rb = new WrappedRoaringBitmap();  
    RTree tree = new RTree(2, new LinearGutmanSplitStrategy(0, 50, rb), rb);
    Random rand = new Random();
    for (int i = 0; i < 100; i++) {
      tree.insert(new float[]{rand.nextFloat(), rand.nextFloat()}, i);
    }

    Assert.assertTrue(getNumPoints(tree.getRoot()) >= tree.getSize());
  }

  private int getNumPoints(Node node)
  {
    int total = 0;
    if (node.isLeaf()) {
      total += node.getChildren().size();
    } else {
      for (Node child : node.getChildren()) {
        total += getNumPoints(child);
      }
    }
    return total;
  }
}
