package de.canitzp.solarhelmet;

import net.minecraft.world.item.Item;

import java.util.function.Supplier;

public abstract class Platform {

    protected abstract <T extends Item> Supplier<T> registerItem(String id, Supplier<T> item);

}
