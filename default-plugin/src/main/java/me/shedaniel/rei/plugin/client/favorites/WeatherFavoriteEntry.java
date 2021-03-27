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

package me.shedaniel.rei.plugin.client.favorites;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Vector4f;
import me.shedaniel.clothconfig2.api.ScissorsHandler;
import me.shedaniel.math.Point;
import me.shedaniel.math.Rectangle;
import me.shedaniel.rei.api.client.REIHelper;
import me.shedaniel.rei.api.client.config.ConfigObject;
import me.shedaniel.rei.api.client.favorites.FavoriteEntry;
import me.shedaniel.rei.api.client.favorites.FavoriteEntryType;
import me.shedaniel.rei.api.client.favorites.FavoriteMenuEntry;
import me.shedaniel.rei.api.client.gui.AbstractRenderer;
import me.shedaniel.rei.api.client.gui.Renderer;
import me.shedaniel.rei.api.client.gui.widgets.Tooltip;
import me.shedaniel.rei.api.common.util.Animator;
import me.shedaniel.rei.api.common.util.CollectionUtils;
import net.minecraft.Util;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TranslatableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Supplier;

public class WeatherFavoriteEntry extends FavoriteEntry {
    public static final ResourceLocation ID = new ResourceLocation("roughlyenoughitems", "weather");
    public static final String TRANSLATION_KEY = "favorite.section.weather";
    private static final ResourceLocation CHEST_GUI_TEXTURE = new ResourceLocation("roughlyenoughitems", "textures/gui/recipecontainer.png");
    public static final String KEY = "weather";
    @Nullable
    private final Weather weather;
    
    public WeatherFavoriteEntry(@Nullable Weather weather) {
        this.weather = weather;
    }
    
    @Override
    public boolean isInvalid() {
        return false;
    }
    
    @Override
    public Renderer getRenderer(boolean showcase) {
        return new AbstractRenderer() {
            private Animator notSetOffset = new Animator(0);
            private Rectangle notSetScissorArea = new Rectangle();
            private long nextSwitch = -1;
            
            @Override
            public void render(PoseStack matrices, Rectangle bounds, int mouseX, int mouseY, float delta) {
                int color = bounds.contains(mouseX, mouseY) ? 0xFFEEEEEE : 0xFFAAAAAA;
                if (bounds.width > 4 && bounds.height > 4) {
                    if (weather == null) {
                        matrices.pushPose();
                        updateAnimator(delta);
                        Vector4f vector4f = new Vector4f(bounds.x, bounds.y, 0, 1.0F);
                        vector4f.transform(matrices.last().pose());
                        Vector4f vector4f2 = new Vector4f(bounds.getMaxX(), bounds.getMaxY(), 0, 1.0F);
                        vector4f2.transform(matrices.last().pose());
                        notSetScissorArea.setBounds((int) vector4f.x(), (int) vector4f.y(), (int) vector4f2.x() - (int) vector4f.x(), (int) vector4f2.y() - (int) vector4f.y());
                        ScissorsHandler.INSTANCE.scissor(notSetScissorArea);
                        int offset = Math.round(notSetOffset.floatValue() * bounds.getHeight());
                        for (int i = 0; i <= 2; i++) {
                            Weather type = Weather.byId(i);
                            renderWeatherIcon(matrices, type, bounds.getCenterX(), bounds.getCenterY() + bounds.getHeight() * i - offset, color);
                        }
                        ScissorsHandler.INSTANCE.removeLastScissor();
                        matrices.popPose();
                    } else {
                        renderWeatherIcon(matrices, weather, bounds.getCenterX(), bounds.getCenterY(), color);
                    }
                }
//                fillGradient(matrices, bounds.getX(), bounds.getY(), bounds.getMaxX(), bounds.getY() + 1, color, color);
//                fillGradient(matrices, bounds.getX(), bounds.getMaxY() - 1, bounds.getMaxX(), bounds.getMaxY(), color, color);
//                fillGradient(matrices, bounds.getX(), bounds.getY(), bounds.getX() + 1, bounds.getMaxY(), color, color);
//                fillGradient(matrices, bounds.getMaxX() - 1, bounds.getY(), bounds.getMaxX(), bounds.getMaxY(), color, color);
            }
            
            private void updateAnimator(float delta) {
                notSetOffset.update(delta);
                if (showcase) {
                    if (nextSwitch == -1) {
                        nextSwitch = Util.getMillis();
                    }
                    if (Util.getMillis() - nextSwitch > 1000) {
                        nextSwitch = Util.getMillis();
                        notSetOffset.setTo(((int) notSetOffset.target() + 1) % 3, 500);
                    }
                } else {
                    notSetOffset.setTo((Minecraft.getInstance().gameMode.getPlayerMode().getId() + 1) % 3, 500);
                }
            }
            
            private void renderWeatherIcon(PoseStack matrices, Weather type, int centerX, int centerY, int color) {
                Minecraft.getInstance().getTextureManager().bind(CHEST_GUI_TEXTURE);
                blit(matrices, centerX - 7, centerY - 7, type.getId() * 14, 14, 14, 14, 256, 256);
            }
            
            @Override
            @Nullable
            public Tooltip getTooltip(Point mouse) {
                if (weather == null)
                    return Tooltip.create(mouse, new TranslatableComponent("text.rei.weather_button.tooltip.dropdown"));
                return Tooltip.create(mouse, new TranslatableComponent("text.rei.weather_button.tooltip.entry", new TranslatableComponent(weather.getTranslateKey())));
            }
            
            @Override
            public boolean equals(Object o) {
                if (this == o) return true;
                if (o == null || getClass() != o.getClass()) return false;
                return hashCode() == o.hashCode();
            }
            
            @Override
            public int hashCode() {
                return Objects.hash(getClass(), showcase, weather);
            }
        };
    }
    
    @Override
    public boolean doAction(int button) {
        if (button == 0) {
            if (weather != null) {
                Minecraft.getInstance().player.chat(ConfigObject.getInstance().getWeatherCommand().replaceAll("\\{weather}", weather.name().toLowerCase(Locale.ROOT)));
                Minecraft.getInstance().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
            }
            return true;
        }
        return false;
    }
    
    @Override
    public Optional<Supplier<Collection<FavoriteMenuEntry>>> getMenuEntries() {
        if (weather == null)
            return Optional.of(this::_getMenuEntries);
        return Optional.empty();
    }
    
    private Collection<FavoriteMenuEntry> _getMenuEntries() {
        return CollectionUtils.map(Weather.values(), WeatherMenuEntry::new);
    }
    
    @Override
    public int hashIgnoreAmount() {
        return weather == null ? -1 : weather.ordinal();
    }
    
    @Override
    public FavoriteEntry copy() {
        return this;
    }
    
    @Override
    public ResourceLocation getType() {
        return ID;
    }
    
    @Override
    public boolean isSame(FavoriteEntry other) {
        if (!(other instanceof WeatherFavoriteEntry)) return false;
        WeatherFavoriteEntry that = (WeatherFavoriteEntry) other;
        return Objects.equals(weather, that.weather);
    }
    
    public enum Type implements FavoriteEntryType<WeatherFavoriteEntry> {
        INSTANCE;
    
        @Override
        public WeatherFavoriteEntry read(CompoundTag object) {
            String stringValue = object.getString(KEY);
            Weather type = stringValue.equals("NOT_SET") ? null : Weather.valueOf(stringValue);
            return new WeatherFavoriteEntry(type);
        }
        
        @Override
        public WeatherFavoriteEntry fromArgs(Object... args) {
            return new WeatherFavoriteEntry((Weather) args[0]);
        }
    
        @Override
        public CompoundTag save(WeatherFavoriteEntry entry, CompoundTag tag) {
            tag.putString(KEY, entry.weather == null ? "NOT_SET" : entry.weather.name());
            return tag;
        }
    }
    
    @ApiStatus.Internal
    public enum Weather {
        CLEAR(0, "text.rei.weather.clear"),
        RAIN(1, "text.rei.weather.rain"),
        THUNDER(2, "text.rei.weather.thunder");
        
        private final int id;
        private final String translateKey;
        
        Weather(int id, String translateKey) {
            this.id = id;
            this.translateKey = translateKey;
        }
        
        public static Weather byId(int id) {
            return byId(id, CLEAR);
        }
        
        public static Weather byId(int id, Weather defaultWeather) {
            for (Weather weather : values()) {
                if (weather.id == id)
                    return weather;
            }
            return defaultWeather;
        }
        
        public int getId() {
            return id;
        }
        
        public String getTranslateKey() {
            return translateKey;
        }
    }
    
    public static class WeatherMenuEntry extends FavoriteMenuEntry {
        public final String text;
        public final Weather weather;
        private int x, y, width;
        private boolean selected, containsMouse, rendering;
        private int textWidth = -69;
        
        public WeatherMenuEntry(Weather weather) {
            this.text = I18n.get(weather.getTranslateKey());
            this.weather = weather;
        }
        
        private int getTextWidth() {
            if (textWidth == -69) {
                this.textWidth = Math.max(0, font.width(text));
            }
            return this.textWidth;
        }
        
        @Override
        public int getEntryWidth() {
            return getTextWidth() + 4;
        }
        
        @Override
        public int getEntryHeight() {
            return 12;
        }
        
        @Override
        public List<? extends GuiEventListener> children() {
            return Collections.emptyList();
        }
        
        @Override
        public void updateInformation(int xPos, int yPos, boolean selected, boolean containsMouse, boolean rendering, int width) {
            this.x = xPos;
            this.y = yPos;
            this.selected = selected;
            this.containsMouse = containsMouse;
            this.rendering = rendering;
            this.width = width;
        }
        
        @Override
        public void render(PoseStack matrices, int mouseX, int mouseY, float delta) {
            if (selected) {
                fill(matrices, x, y, x + width, y + 12, -12237499);
            }
            if (selected && containsMouse) {
                REIHelper.getInstance().queueTooltip(Tooltip.create(new TranslatableComponent("text.rei.weather_button.tooltip.entry", text)));
            }
            font.draw(matrices, text, x + 2, y + 2, selected ? 16777215 : 8947848);
        }
        
        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (rendering && mouseX >= x && mouseX <= x + width && mouseY >= y && mouseY <= y + 12) {
                Minecraft.getInstance().player.chat(ConfigObject.getInstance().getWeatherCommand().replaceAll("\\{weather}", weather.name().toLowerCase(Locale.ROOT)));
                minecraft.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                closeMenu();
                return true;
            }
            return super.mouseClicked(mouseX, mouseY, button);
        }
    }
}