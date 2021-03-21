/*
 * This file is licensed under the MIT License, part of Roughly Enough Items.
 * Copyright (c) 2018, 2019, 2020 shedaniel
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

package me.shedaniel.rei.impl.search;

import me.shedaniel.rei.api.config.ConfigObject;
import me.shedaniel.rei.api.gui.config.SearchMode;
import me.shedaniel.rei.api.ingredient.EntryStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Unit;
import org.apache.commons.lang3.mutable.Mutable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
@Environment(EnvType.CLIENT)
public final class IdentifierArgumentType extends ArgumentType<Unit, String> {
    public static final IdentifierArgumentType INSTANCE = new IdentifierArgumentType();
    private static final String EMPTY = "";
    private static final Style STYLE = Style.EMPTY.withColor(TextColor.fromRgb(0x8d7eed));
    
    @Override
    public String getName() {
        return "identifier";
    }
    
    @Override
    @Nullable
    public String getPrefix() {
        return "*";
    }
    
    @Override
    public Style getHighlightedStyle() {
        return STYLE;
    }
    
    @Override
    public SearchMode getSearchMode() {
        return ConfigObject.getInstance().getIdentifierSearchMode();
    }
    
    @Override
    public boolean matches(Mutable<String> data, EntryStack<?> stack, String searchText, Unit filterData) {
        if (data.getValue() == null) {
            ResourceLocation identifier = stack.getIdentifier();
            if (identifier == null) {
                data.setValue(EMPTY);
            } else {
                String s = identifier.toString();
                if (s.isEmpty()) {
                    data.setValue(EMPTY);
                } else {
                    data.setValue(s);
                }
            }
        }
        String identifier = data.getValue();
        return !identifier.isEmpty() && identifier.contains(searchText);
    }
    
    @Override
    public Unit prepareSearchFilter(String searchText) {
        return Unit.INSTANCE;
    }
    
    private IdentifierArgumentType() {
    }
}
