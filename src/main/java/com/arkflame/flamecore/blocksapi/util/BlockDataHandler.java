package com.arkflame.flamecore.blocksapi.util;

import org.bukkit.block.Block;

import com.arkflame.flamecore.blocksapi.BlockWrapper;

/**
 * Internal interface for version-specific block data manipulation.
 */
public interface BlockDataHandler {
    BlockWrapper capture(Block block);
    void apply(Block block, BlockWrapper wrapper);
    boolean needsUpdate(Block block, BlockWrapper wrapper);
}