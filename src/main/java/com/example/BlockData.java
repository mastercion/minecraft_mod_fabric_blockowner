package com.example;

import net.minecraft.block.Block;

public class BlockData {
    public final Block block;
    public final String owner;

    public BlockData(Block block, String owner) {
        this.block = block;
        this.owner = owner;
    }
}