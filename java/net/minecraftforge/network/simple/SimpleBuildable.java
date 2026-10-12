package net.minecraftforge.network.simple;

import net.minecraftforge.network.ChannelBuildable;
import net.minecraftforge.network.SimpleChannel;

public interface SimpleBuildable extends ChannelBuildable<Object> {
   SimpleChannel build();
}
