package com.metamx.collections.spatial;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Preconditions;
import com.google.common.base.Throwables;

/**
 */
public class RTreeUtils
{
    private static ObjectMapper jsonMapper = new ObjectMapper();

    public static double getEnclosingArea(Node groups, Node nextToAssign)
    {
        Preconditions.checkArgument(groups.getNumDims() == nextToAssign.getNumDims());

        double[] minCoords = new double[groups.getNumDims()];
        double[] maxCoords = new double[groups.getNumDims()];

        for (int i = 0; i < minCoords.length; i++) {
            minCoords[i] = Math.min(groups.getMinCoordinates()[i], nextToAssign.getMinCoordinates()[i]);
            maxCoords[i] = Math.max(groups.getMaxCoordinates()[i], nextToAssign.getMaxCoordinates()[i]);
        }

        double area = 1.0;
        for (int i = 0; i < minCoords.length; i++) {
            area *= (maxCoords[i] - minCoords[i]);
        }

        return area;
    }

    public static double getExpansionCost(Node node, Point point)
    {
        Preconditions.checkArgument(node.getNumDims() == point.getNumDims());

        if (node.contains(point.getCoords())) {
            return 0;
        }

        double expanded = 1.0;
        for (int i = 0; i < node.getNumDims(); i++) {
            double min = Math.min(point.getCoords()[i], node.getMinCoordinates()[i]);
            double max = Math.max(point.getCoords()[i], node.getMinCoordinates()[i]);
            expanded *= (max - min);
        }

        return (expanded - node.getArea());
    }

    public static void enclose(Node[] nodes)
    {
        for (Node node : nodes) {
            node.enclose();
        }
    }

    public static void print(RTree tree)
    {
        System.out.printf("numDims : %d%n", tree.getNumDims());
        try {
            printRTreeNode(tree.getRoot(), 0);
        }
        catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static void print(ImmutableRTree tree)
    {
        System.out.printf("numDims : %d%n", tree.getNumDims());
        try {
            printNode(tree.getRoot(), 0);
        }
        catch (Exception e) {
            throw Throwables.propagate(e);
        }
    }

    public static void printRTreeNode(Node node, int level) throws Exception
    {
        System.out.printf(
                "%sminCoords: %s, maxCoords: %s, numChildren: %d, isLeaf:%s%n",
                makeDashes(level),
                jsonMapper.writeValueAsString(node.getMinCoordinates()),
                jsonMapper.writeValueAsString(
                        node.getMaxCoordinates()
                ),
                node.getChildren().size(),
                node.isLeaf()
        );
        if (node.isLeaf()) {
            for (Node child : node.getChildren()) {
                Point point = (Point) (child);
                System.out
                        .printf(
                                "%scoords: %s, conciseSet: %s%n",
                                makeDashes(level),
                                jsonMapper.writeValueAsString(point.getCoords()),
                                point.getRoaringBitmap()
                        );
            }
        } else {
            level++;
            for (Node child : node.getChildren()) {
                printRTreeNode(child, level);
            }
        }
    }

    public static boolean verifyEnclose(Node node)
    {
        for (Node child : node.getChildren()) {
            for (int i = 0; i < node.getNumDims(); i++) {
                if (child.getMinCoordinates()[i] < node.getMinCoordinates()[i]
                        || child.getMaxCoordinates()[i] > node.getMaxCoordinates()[i]) {
                    return false;
                }
            }
        }

        if (!node.isLeaf()) {
            for (Node child : node.getChildren()) {
                if (!verifyEnclose(child)) {
                    return false;
                }
            }
        }

        return true;
    }

    public static boolean verifyEnclose(ImmutableNode node)
    {
        for (ImmutableNode child : node.getChildren()) {
            for (int i = 0; i < node.getNumDims(); i++) {
                if (child.getMinCoordinates()[i] < node.getMinCoordinates()[i]
                        || child.getMaxCoordinates()[i] > node.getMaxCoordinates()[i]) {
                    return false;
                }
            }
        }

        if (!node.isLeaf()) {
            for (ImmutableNode child : node.getChildren()) {
                if (!verifyEnclose(child)) {
                    return false;
                }
            }
        }

        return true;
    }

    private static void printNode(ImmutableNode node, int level) throws Exception
    {
        System.out.printf(
                "%sminCoords: %s, maxCoords: %s, numChildren: %d, isLeaf: %s%n",
                makeDashes(level),
                jsonMapper.writeValueAsString(node.getMinCoordinates()),
                jsonMapper.writeValueAsString(
                        node.getMaxCoordinates()
                ),
                node.getNumChildren(),
                node.isLeaf()
        );
        if (node.isLeaf()) {
            for (ImmutableNode immutableNode : node.getChildren()) {
                ImmutablePoint point = new ImmutablePoint(immutableNode);
                System.out
                        .printf(
                                "%scoords: %s, conciseSet: %s%n",
                                makeDashes(level),
                                jsonMapper.writeValueAsString(point.getCoords()),
                                point.getImmutableRoaringBitmap()
                        );
            }
        } else {
            level++;
            for (ImmutableNode immutableNode : node.getChildren()) {
                printNode(immutableNode, level);
            }
        }
    }

    private static String makeDashes(int level)
    {
        String retVal = "";
        for (int i = 0; i < level; i++) {
            retVal += "-";
        }
        return retVal;
    }
}
