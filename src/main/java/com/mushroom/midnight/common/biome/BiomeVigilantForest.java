package com.mushroom.midnight.common.biome;

import com.mushroom.midnight.common.world.generator.WorldGenShadowrootTrees;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkPos;
import net.minecraft.world.World;
import net.minecraft.world.gen.feature.WorldGenAbstractTree;
import net.minecraftforge.event.terraingen.DecorateBiomeEvent;
import net.minecraftforge.event.terraingen.TerrainGen;

import java.util.Random;

public class BiomeVigilantForest extends BiomeBase {

    public static final WorldGenShadowrootTrees SHADOWROOT_TREE_GEN = new WorldGenShadowrootTrees();

    public BiomeVigilantForest(BiomeProperties properties) {
        super(properties);

        this.decorator.treesPerChunk = 8;
        this.decorator.grassPerChunk = 2;
        this.decorator.deadBushPerChunk = 0;
        this.decorator.reedsPerChunk = 0;
        this.decorator.cactiPerChunk = 0;
        this.decorator.flowersPerChunk = 0;
    }

    @Override
    public WorldGenAbstractTree getRandomTreeFeature(Random rand) {
        return SHADOWROOT_TREE_GEN;
    }

    @Override
    public void decorate(World world, Random rand, BlockPos pos) {
        ChunkPos chunkPos = new ChunkPos(pos);
        if (TerrainGen.decorate(world, rand, chunkPos, DecorateBiomeEvent.Decorate.EventType.FLOWERS)) {
            this.generateCoverPlant(world, rand, pos, 1, LUMEN_GENERATOR);
            this.generateCoverPlant(world, rand, pos, 1, DOUBLE_LUMEN_GENERATOR);
        }

        super.decorate(world, rand, pos);
    }
}
