package com.mamiyaotaru.voxelmap.interfaces;

import com.mamiyaotaru.voxelmap.VoxelConstants;
import com.mamiyaotaru.voxelmap.util.BiomeRepository;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.state.BlockState;

public abstract class AbstractMapData {
    protected int width;
    protected int height;
    protected final Object dataLock = new Object();
    private final Object labelLock = new Object();
    public Point[][] points;
    public ArrayList<Segment> segments;
    private final ArrayList<BiomeLabel> labels = new ArrayList<>();

    public int getWidth() {
        return this.width;
    }

    public int getHeight() {
        return this.height;
    }

    public void segmentBiomes() {
        this.points = new Point[this.width][this.height];
        this.segments = new ArrayList<>();

        for (int x = 0; x < this.width; ++x) {
            for (int z = 0; z < this.height; ++z) {
                this.points[x][z] = new Point(x, z, this.getBiome(x, z));
            }
        }

        synchronized (this.dataLock) {
            for (int x = 0; x < this.width; ++x) {
                for (int z = 0; z < this.height; ++z) {
                    if (!this.points[x][z].inSegment) {
                        long startTime = System.nanoTime();
                        if (this.points[x][z].biomeID == null) {
                            VoxelConstants.getLogger().warn("no biome segment!");
                        }

                        Segment segment = new Segment(this.points[x][z]);
                        this.segments.add(segment);
                        segment.flood();
                        if (this.points[x][z].biomeID == null) {
                            VoxelConstants.getLogger().warn("created in " + (System.nanoTime() - startTime));
                        }
                    }
                }
            }

        }
    }

    public void findCenterOfSegments(boolean horizontalBias) {
        if (this.segments != null) {
            for (Segment segment : this.segments) {
                if (segment.biomeID != null) {
                    segment.calculateCenter(horizontalBias);
                }
            }
        }

        synchronized (this.labelLock) {
            this.labels.clear();
            if (this.segments != null) {
                for (Segment segment : this.segments) {
                    if (segment.biomeID != null) {
                        BiomeLabel label = new BiomeLabel();
                        label.biomeID = segment.biomeID;
                        label.name = segment.name;
                        label.segmentSize = segment.memberPoints.size();
                        label.x = segment.centerX;
                        label.z = segment.centerZ;
                        this.labels.add(label);
                    }
                }
            }

        }
    }

    public ArrayList<BiomeLabel> getBiomeLabels() {
        synchronized (this.labelLock) {
            return new ArrayList<>(this.labels);
        }
    }

    public abstract int getHeight(int x, int z);

    public abstract BlockState getBlockstate(int x, int z);

    public abstract int getBiomeTint(int x, int z);

    public abstract int getLight(int x, int z);

    public abstract int getOceanFloorHeight(int x, int z);

    public abstract BlockState getOceanFloorBlockstate(int x, int z);

    public abstract int getOceanFloorBiomeTint(int x, int z);

    public abstract int getOceanFloorLight(int x, int z);

    public abstract int getTransparentHeight(int x, int z);

    public abstract BlockState getTransparentBlockstate(int x, int z);

    public abstract int getTransparentBiomeTint(int x, int z);

    public abstract int getTransparentLight(int x, int z);

    public abstract int getFoliageHeight(int x, int z);

    public abstract BlockState getFoliageBlockstate(int x, int z);

    public abstract int getFoliageBiomeTint(int x, int z);

    public abstract int getFoliageLight(int x, int z);

    public abstract Biome getBiome(int x, int z);

    public abstract void setHeight(int x, int z, int height);

    public abstract void setBlockstate(int x, int z, BlockState state);

    public abstract void setBiomeTint(int x, int z, int tint);

    public abstract void setLight(int x, int z, int light);

    public abstract void setOceanFloorHeight(int x, int z, int height);

    public abstract void setOceanFloorBlockstate(int x, int z, BlockState state);

    public abstract void setOceanFloorBiomeTint(int x, int z, int tint);

    public abstract void setOceanFloorLight(int x, int z, int light);

    public abstract void setTransparentHeight(int x, int z, int height);

    public abstract void setTransparentBlockstate(int x, int z, BlockState state);

    public abstract void setTransparentBiomeTint(int x, int z, int tint);

    public abstract void setTransparentLight(int x, int z, int light);

    public abstract void setFoliageHeight(int x, int z, int height);

    public abstract void setFoliageBlockstate(int x, int z, BlockState state);

    public abstract void setFoliageBiomeTint(int x, int z, int tint);

    public abstract void setFoliageLight(int x, int z, int light);

    public abstract void setBiome(int x, int z, Biome biome);

    public abstract void moveX(int x);

    public abstract void moveZ(int z);

    public static class BiomeLabel {
        public Biome biomeID = null;
        public String name = "";
        public int segmentSize;
        public int x;
        public int z;
    }

    private static final class Point {
        public final int x;
        public final int z;
        public boolean inSegment;
        public boolean isCandidate;
        public int layer = -1;
        public final Biome biomeID;

        private Point(int x, int z, Biome biomeID) {
            this.x = x;
            this.z = z;

            if (biomeID == null) {
                this.inSegment = true;
            }

            this.biomeID = biomeID;
        }
    }

    public class Segment {
        public final ArrayList<Point> memberPoints;
        ArrayList<Point> currentShell;
        public final Biome biomeID;
        public String name;
        public int centerX;
        public int centerZ;

        public Segment(Point point) {
            this.biomeID = point.biomeID;
            if (this.biomeID != null) {
                this.name = BiomeRepository.getName(this.biomeID);
            }

            this.memberPoints = new ArrayList<>();
            this.memberPoints.add(point);
            this.currentShell = new ArrayList<>();
        }

        public void flood() {
            ArrayList<Point> candidatePoints = new ArrayList<>();
            candidatePoints.add(this.memberPoints.remove(0));

            while (!candidatePoints.isEmpty()) {
                Point point = candidatePoints.remove(0);
                point.isCandidate = false;
                if (point.biomeID == this.biomeID) {
                    this.memberPoints.add(point);
                    point.inSegment = true;
                    boolean edge = false;
                    if (point.x < AbstractMapData.this.width - 1) {
                        Point neighbor = AbstractMapData.this.points[point.x + 1][point.z];
                        if (!neighbor.inSegment && !neighbor.isCandidate) {
                            candidatePoints.add(neighbor);
                            neighbor.isCandidate = true;
                        }

                        if (neighbor.biomeID != point.biomeID) {
                            edge = true;
                        }
                    } else {
                        edge = true;
                    }

                    if (point.x > 0) {
                        Point neighbor = AbstractMapData.this.points[point.x - 1][point.z];
                        if (!neighbor.inSegment && !neighbor.isCandidate) {
                            candidatePoints.add(neighbor);
                            neighbor.isCandidate = true;
                        }

                        if (neighbor.biomeID != point.biomeID) {
                            edge = true;
                        }
                    } else {
                        edge = true;
                    }

                    if (point.z < AbstractMapData.this.height - 1) {
                        Point neighbor = AbstractMapData.this.points[point.x][point.z + 1];
                        if (!neighbor.inSegment && !neighbor.isCandidate) {
                            candidatePoints.add(neighbor);
                            neighbor.isCandidate = true;
                        }

                        if (neighbor.biomeID != point.biomeID) {
                            edge = true;
                        }
                    } else {
                        edge = true;
                    }

                    if (point.z > 0) {
                        Point neighbor = AbstractMapData.this.points[point.x][point.z - 1];
                        if (!neighbor.inSegment && !neighbor.isCandidate) {
                            candidatePoints.add(neighbor);
                            neighbor.isCandidate = true;
                        }

                        if (neighbor.biomeID != point.biomeID) {
                            edge = true;
                        }
                    } else {
                        edge = true;
                    }

                    if (edge) {
                        point.layer = 0;
                        this.currentShell.add(point);
                    }
                }
            }

        }

        public void calculateCenter(boolean horizontalBias) {
            this.calculateCenterOfMass();
            this.morphologicallyErode(horizontalBias);
        }

        public void calculateCenterOfMass() {
            this.calculateCenterOfMass(this.memberPoints);
        }

        public void calculateCenterOfMass(List<Point> points) {
            this.centerX = 0;
            this.centerZ = 0;

            for (Point point : points) {
                this.centerX += point.x;
                this.centerZ += point.z;
            }

            this.centerX /= points.size();
            this.centerZ /= points.size();
        }

        public void calculateClosestPointToCenter(List<Point> points) {
            int distanceSquared = AbstractMapData.this.width * AbstractMapData.this.width + AbstractMapData.this.height * AbstractMapData.this.height;
            Point centerPoint = null;

            for (Point point : points) {
                int pointDistanceSquared = (point.x - this.centerX) * (point.x - this.centerX) + (point.z - this.centerZ) * (point.z - this.centerZ);
                if (pointDistanceSquared < distanceSquared) {
                    distanceSquared = pointDistanceSquared;
                    centerPoint = point;
                }
            }

            this.centerX = centerPoint.x;
            this.centerZ = centerPoint.z;
        }

        public void morphologicallyErode(boolean horizontalBias) {
            float labelWidth = (VoxelConstants.getMinecraft().font.width(this.name) + 8);
            float multi = (AbstractMapData.this.width / 32f);
            float shellWidth = 2.0F;
            float labelPadding = labelWidth / 16.0F * multi / shellWidth;

            int layer;
            for (layer = 0; !this.currentShell.isEmpty() && layer < labelPadding; this.currentShell = this.getNextShell(this.currentShell, layer, horizontalBias)) {
                ++layer;
            }

            if (!this.currentShell.isEmpty()) {
                ArrayList<Point> remainingPoints = new ArrayList<>();

                for (Point point : this.memberPoints) {
                    if (point.layer < 0 || point.layer == layer) {
                        remainingPoints.add(point);
                    }
                }

                this.calculateClosestPointToCenter(remainingPoints);
            }

        }

        public ArrayList<Point> getNextShell(List<Point> pointsToCheck, int layer, boolean horizontalBias) {
            int layerWidth = horizontalBias ? 2 : 1;
            int layerHeight = horizontalBias ? 1 : 2;
            ArrayList<Point> nextShell = new ArrayList<>();

            for (Point point : pointsToCheck) {
                if (point.x < AbstractMapData.this.width - layerWidth) {
                    boolean foundEdge = false;

                    for (int t = layerWidth; t > 0; --t) {
                        Point neighbor = AbstractMapData.this.points[point.x + t][point.z];
                        if (neighbor.biomeID == point.biomeID && neighbor.layer < 0) {
                            neighbor.layer = layer;
                            if (!foundEdge) {
                                foundEdge = true;
                                nextShell.add(neighbor);
                            }
                        }
                    }
                }

                if (point.x >= layerWidth) {
                    boolean foundEdge = false;

                    for (int t = layerWidth; t > 0; --t) {
                        Point neighbor = AbstractMapData.this.points[point.x - t][point.z];
                        if (neighbor.biomeID == point.biomeID && neighbor.layer < 0) {
                            neighbor.layer = layer;
                            if (!foundEdge) {
                                foundEdge = true;
                                nextShell.add(neighbor);
                            }
                        }
                    }
                }

                if (point.z < AbstractMapData.this.height - layerHeight) {
                    boolean foundEdge = false;

                    for (int t = layerHeight; t > 0; --t) {
                        Point neighbor = AbstractMapData.this.points[point.x][point.z + t];
                        if (neighbor.biomeID == point.biomeID && neighbor.layer < 0) {
                            neighbor.layer = layer;
                            if (!foundEdge) {
                                foundEdge = true;
                                nextShell.add(neighbor);
                            }
                        }
                    }
                }

                if (point.z >= layerHeight) {
                    boolean foundEdge = false;

                    for (int t = layerHeight; t > 0; --t) {
                        Point neighbor = AbstractMapData.this.points[point.x][point.z - t];
                        if (neighbor.biomeID == point.biomeID && neighbor.layer < 0) {
                            neighbor.layer = layer;
                            if (!foundEdge) {
                                foundEdge = true;
                                nextShell.add(neighbor);
                            }
                        }
                    }
                }
            }

            if (nextShell.isEmpty()) {
                this.calculateCenterOfMass(pointsToCheck);
                this.calculateClosestPointToCenter(pointsToCheck);
            }
            return nextShell;
        }
    }
}