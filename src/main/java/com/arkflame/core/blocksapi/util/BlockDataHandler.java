package com.arkflame.core.blocksapi.util;

import com.arkflame.core.blocksapi.BlockWrapper;
import org.bukkit.block.Block;

/**
 * Internal interface for version-specific block data manipulation.
 */
public interface BlockDataHandler {
    BlockWrapper capture(Block block);
    void apply(Block block, BlockWrapper wrapper);
    boolean needsUpdate(Block block, BlockWrapper wrapper);
}