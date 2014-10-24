package com.metamx.collections.spatial;

import com.google.common.base.Preconditions;
import com.metamx.collections.spatial.CompressedBitmaps.GenericBitmap;
import com.metamx.collections.spatial.CompressedBitmaps.ImmutableGenericBitmap;
import com.metamx.collections.spatial.split.LinearGutmanSplitStrategy;
import com.metamx.collections.spatial.split.SplitStrategy;

import java.util.Arrays;

import org.roaringbitmap.buffer.MutableRoaringBitmap;

/**
 * This RTree has been optimized to work with Roaring bitmaps.
 * <p/>
 * This code will probably make a lot more sense if you read:
 * http://www.sai.msu.su/~megera/postgres/gist/papers/gutman-rtree.pdf
 */
public class RTree
{
    private final int numDims;
    private final SplitStrategy splitStrategy;
    private Node root;
    private volatile int size;

    public RTree(GenericBitmap bitmap)
    {
        this(0, new LinearGutmanSplitStrategy(0, 0, bitmap), bitmap);
    }

    public RTree(int numDims, SplitStrategy splitStrategy, GenericBitmap bitmap)
    {
        this.numDims = numDims;
        this.splitStrategy = splitStrategy;
        this.root = buildRoot(true, bitmap);
    }

    /**
     * This description is from the original paper.
     * <p/>
     * Algorithm Insert: Insert a new index entry E into an R-tree.
     * <p/>
     * I1. [Find position for new record]. Invoke {@link #chooseLeaf(com.metamx.collections.spatial.Node, com.metamx.collections.spatial.Point)} to select
     * a leaf node L in which to place E.
     * <p/>
     * I2. [Add records to leaf node]. If L has room for another entry, install E. Otherwise invoke
     * {@link com.metamx.collections.spatial.split.SplitStrategy} split methods to obtain L and LL containing E and all the old entries of L.
     * <p/>
     * I3. [Propagate changes upward]. Invoke {@link #adjustTree(com.metamx.collections.spatial.Node, com.metamx.collections.spatial.Node)} on L, also passing LL if a split was
     * performed.
     * <p/>
     * I4. [Grow tree taller]. If node split propagation caused the root to split, create a new record whose
     * children are the two resulting nodes.
     *
     * @param coords - the coordinates of the entry
     * @param entry  - the integer to insert
     */
    public void insert(float[] coords, int entry)
    {
        Preconditions.checkArgument(coords.length == numDims);
        insertInner(new Point(coords, entry, this.root.getBitmap().getEmptyWrappedBitmap()));
    }

    public void insert(float[] coords, GenericBitmap entry)
    {
        Preconditions.checkArgument(coords.length == numDims);
        insertInner(new Point(coords, entry));
    }

    /**
     * Not yet implemented.
     *
     * @param coords - the coordinates of the entry
     * @param entry  - the integer to insert
     *
     * @return - whether the operation completed successfully
     */
    public boolean delete(double[] coords, int entry)
    {
        throw new UnsupportedOperationException();
    }

    public int getSize()
    {
        return size;
    }

    public int getNumDims()
    {
        return numDims;
    }

    public SplitStrategy getSplitStrategy()
    {
        return splitStrategy;
    }

    public Node getRoot()
    {
        return root;
    }

    private Node buildRoot(boolean isLeaf, GenericBitmap bitmap)
    {
        float[] initMinCoords = new float[numDims];
        float[] initMaxCoords = new float[numDims];
        Arrays.fill(initMinCoords, -Float.MAX_VALUE);
        Arrays.fill(initMaxCoords, Float.MAX_VALUE);

        return new Node(initMinCoords, initMaxCoords, isLeaf, bitmap);
    }

    private void insertInner(Point point)
    {
        Node node = chooseLeaf(root, point);
        node.addChild(point);

        if (splitStrategy.needToSplit(node)) {
            Node[] groups = splitStrategy.split(node);
            adjustTree(groups[0], groups[1]);
        } else {
            adjustTree(node, null);
        }

        size++;
    }


    /**
     * This description is from the original paper.
     * <p/>
     * Algorithm ChooseLeaf. Select a leaf node in which to place a new index entry E.
     * <p/>
     * CL1. [Initialize]. Set N to be the root node.
     * <p/>
     * CL2. [Leaf check]. If N is a leaf, return N.
     * <p/>
     * CL3. [Choose subtree]. If N is not a leaf, let F be the entry in N whose rectangle
     * FI needs least enlargement to include EI. Resolve ties by choosing the entry with the rectangle
     * of smallest area.
     * <p/>
     * CL4. [Descend until a leaf is reached]. Set N to be the child node pointed to by Fp and repeated from CL2.
     *
     * @param node  - current node to evaluate
     * @param point - point to insert
     *
     * @return - leafNode where point can be inserted
     */
    private Node chooseLeaf(Node node, Point point)
    {
        node.addToBitmap(point);

        if (node.isLeaf()) {
            return node;
        }

        double minCost = Double.MAX_VALUE;
        Node optimal = null;
        for (Node child : node.getChildren()) {
            double cost = RTreeUtils.getExpansionCost(child, point);
            if (cost < minCost) {
                minCost = cost;
                optimal = child;
            } else if (cost == minCost) {
                // Resolve ties by choosing the entry with the rectangle of smallest area
                if (child.getArea() < optimal.getArea()) {
                    optimal = child;
                }
            }
        }

        return chooseLeaf(optimal, point);
    }

    /**
     * This description is from the original paper.
     * <p/>
     * AT1. [Initialize]. Set N=L. If L was split previously, set NN to be the resulting second node.
     * <p/>
     * AT2. [Check if done]. If N is the root, stop.
     * <p/>
     * AT3. [Adjust covering rectangle in parent entry]. Let P be the parent node of N, and let Ev(N)I be N's entry in P.
     * Adjust Ev(N)I so that it tightly encloses all entry rectangles in N.
     * <p/>
     * AT4. [Propagate node split upward]. If N has a partner NN resulting from an earlier split, create a new entry
     * Ev(NN) with Ev(NN)p pointing to NN and Ev(NN)I enclosing all rectangles in NN. Add Ev(NN) to p is there is room.
     * Otherwise, invoke {@link com.metamx.collections.spatial.split.SplitStrategy} split to product p and pp containing Ev(NN) and all p's old entries.
     *
     * @param n  - first node to adjust
     * @param nn - optional second node to adjust
     */
    private void adjustTree(Node n, Node nn)
    {
        // special case for root
        if (n == root) {
            if (nn != null) {
                root = buildRoot(false, n.getBitmap().getEmptyWrappedBitmap());
                root.addChild(n);
                root.addChild(nn);
            }
            root.enclose();
            return;
        }

        boolean updateParent = n.enclose();

        if (nn != null) {
            nn.enclose();
            updateParent = true;

            if (splitStrategy.needToSplit(n.getParent())) {
                Node[] groups = splitStrategy.split(n.getParent());
                adjustTree(groups[0], groups[1]);
            }
        }

        if (n.getParent() != null && updateParent) {
            adjustTree(n.getParent(), null);
        }
    }
}
