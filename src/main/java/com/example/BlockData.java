package com.example;

import net.minecraft.block.Block;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class BlockData {
    public final Block block;
    public final String owner;
    public final LocalDateTime timestamp;
    public String dimension;

    public BlockData(Block block, String owner, LocalDateTime timestamp, String dimension) {
        this.block = block;
        this.owner = owner;
        this.timestamp = timestamp;
        this.dimension = dimension;  // Add this line

    }

    public String getFormattedTimestamp() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return this.timestamp.format(formatter);
    }

    public Text getFormattedDisplay() {
        String format = Config.getInstance().getDisplayFormat();
        String formattedText = format.replace("{Date}", getFormattedTimestamp())
                .replace("{Player}", owner)
                .replace("{Block}", block.getTranslationKey());

        return Text.literal(formattedText);
    }
}