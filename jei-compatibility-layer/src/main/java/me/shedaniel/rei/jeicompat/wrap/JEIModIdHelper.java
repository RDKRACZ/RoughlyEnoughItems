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

package me.shedaniel.rei.jeicompat.wrap;

import lombok.experimental.ExtensionMethod;
import me.shedaniel.rei.api.client.ClientHelper;
import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.jeicompat.JEIPluginDetector;
import mezz.jei.api.helpers.IModIdHelper;
import mezz.jei.api.ingredients.IIngredientHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.NotNull;

import java.util.List;

@ExtensionMethod(JEIPluginDetector.class)
public enum JEIModIdHelper implements IModIdHelper {
    INSTANCE;
    
    @Override
    @NotNull
    public String getModNameForModId(@NotNull String modId) {
        return ClientHelper.getInstance().getModFromModId(modId);
    }
    
    @Override
    public boolean isDisplayingModNameEnabled() {
        return ConfigObject.getInstance().shouldAppendModNames();
    }
    
    @Override
    @NotNull
    public String getFormattedModNameForModId(@NotNull String modId) {
        String name = getModNameForModId(modId);
        if (name.isEmpty()) return name;
        return "§9§o" + name;
    }
    
    @Override
    @NotNull
    public <T> List<Component> addModNameToIngredientTooltip(@NotNull List<Component> tooltip, @NotNull T ingredient, @NotNull IIngredientHelper<T> ingredientHelper) {
        ResourceLocation location = ingredient.unwrapStack().getIdentifier();
        if (location != null) {
            ClientHelper.getInstance().appendModIdToTooltips(tooltip, location.getNamespace());
        }
        return tooltip;
    }
}
