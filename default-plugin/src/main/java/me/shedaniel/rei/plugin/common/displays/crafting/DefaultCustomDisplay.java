/*
 * This file is licensed under the MIT License, part of Roughly Enough Items.
 * Copyright (c) 2018, 2019, 2020, 2021 shedaniel
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package me.shedaniel.rei.plugin.common.displays.crafting;

import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.registry.RecipeManagerContext;
import me.shedaniel.rei.api.common.transfer.info.MenuInfo;
import me.shedaniel.rei.api.common.transfer.info.MenuSerializationContext;
import me.shedaniel.rei.api.common.transfer.info.simple.SimpleGridMenuInfo;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.crafting.Recipe;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Optional;

public class DefaultCustomDisplay extends DefaultCraftingDisplay<Recipe<?>> {
    private int width;
    private int height;
    
    public DefaultCustomDisplay(@Nullable Recipe<?> possibleRecipe, List<EntryIngredient> input, List<EntryIngredient> output) {
        super(input, output, Optional.ofNullable(possibleRecipe));
        BitSet row = new BitSet(3);
        BitSet column = new BitSet(3);
        for (int i = 0; i < 9; i++)
            if (i < input.size()) {
                EntryIngredient stacks = input.get(i);
                if (stacks.stream().anyMatch(stack -> !stack.isEmpty())) {
                    row.set((i - (i % 3)) / 3);
                    column.set(i % 3);
                }
            }
        this.width = column.cardinality();
        this.height = row.cardinality();
    }
    
    public static DefaultCustomDisplay simple(List<EntryIngredient> input, List<EntryIngredient> output, Optional<ResourceLocation> location) {
        Recipe<?> optionalRecipe = location.flatMap(resourceLocation -> RecipeManagerContext.getInstance().getRecipeManager().byKey(resourceLocation))
                .orElse(null);
        return new DefaultCustomDisplay(optionalRecipe, input, output);
    }
    
    @Override
    public List<EntryIngredient> getInputEntries(MenuSerializationContext<?, ?, ?> context, MenuInfo<?, ?> info, boolean fill) {
        if (fill && info instanceof SimpleGridMenuInfo) {
            List<EntryIngredient> out = new ArrayList<>();
            int craftingWidth = ((SimpleGridMenuInfo<AbstractContainerMenu, ?>) info).getCraftingWidth(context.getMenu());
            int craftingHeight = ((SimpleGridMenuInfo<AbstractContainerMenu, ?>) info).getCraftingHeight(context.getMenu());
            for (int i = 0; i < 9; i++) {
                if (i < inputs.size()) {
                    int x = i % 3;
                    if (x < craftingWidth) {
                        out.add(inputs.get(i));
                        if (out.size() > craftingWidth * craftingHeight) break;
                    }
                }
            }
            while (out.size() < craftingWidth * craftingHeight) out.add(EntryIngredient.empty());
            return out;
        }
        
        return super.getInputEntries(context, info, fill);
    }
    
    @Override
    public int getWidth() {
        return width;
    }
    
    @Override
    public int getHeight() {
        return height;
    }
}
