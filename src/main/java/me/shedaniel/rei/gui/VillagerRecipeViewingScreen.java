package me.shedaniel.rei.gui;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.mojang.blaze3d.platform.GlStateManager;
import com.zeitheron.hammercore.client.utils.Scissors;
import me.shedaniel.rei.api.*;
import me.shedaniel.rei.client.ScreenHelper;
import me.shedaniel.rei.gui.renderables.RecipeRenderer;
import me.shedaniel.rei.gui.widget.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.audio.PositionedSoundInstance;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Screen;
import net.minecraft.client.render.GuiLighting;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.StringTextComponent;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.BufferUtils;
import org.lwjgl.glfw.GLFW;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.nio.IntBuffer;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static me.shedaniel.rei.gui.RecipeViewingScreen.getSpeedCraftFunctionalByCategory;

public class VillagerRecipeViewingScreen extends Screen {
    
    private final Map<RecipeCategory, List<RecipeDisplay>> categoryMap;
    private final List<RecipeCategory> categories;
    private final List<Widget> widgets;
    private final List<ButtonWidget> buttonWidgets;
    private final List<Renderer> recipeRenderers;
    private final List<TabWidget> tabs;
    public Rectangle bounds, scrollListBounds;
    private int selectedCategoryIndex, selectedRecipeIndex;
    private double scroll;
    private int tabsPage;
    private static final int TABS_PER_PAGE = 9;
    
    public VillagerRecipeViewingScreen(Map<RecipeCategory, List<RecipeDisplay>> map) {
        super(new StringTextComponent(""));
        this.widgets = Lists.newArrayList();
        this.categoryMap = Maps.newLinkedHashMap();
        this.selectedCategoryIndex = 0;
        this.selectedRecipeIndex = 0;
        this.scroll = 0;
        this.tabsPage = 0;
        this.categories = Lists.newArrayList();
        this.buttonWidgets = Lists.newArrayList();
        this.tabs = Lists.newArrayList();
        this.recipeRenderers = Lists.newArrayList();
        RecipeHelper.getInstance().getAllCategories().forEach(category -> {
            if (map.containsKey(category)) {
                categories.add(category);
                categoryMap.put(category, map.get(category));
            }
        });
    }
    
    @Override
    protected void init() {
        super.init();
        this.children.clear();
        this.widgets.clear();
        this.buttonWidgets.clear();
        this.recipeRenderers.clear();
        this.tabs.clear();
        int largestWidth = width - 100;
        int largestHeight = height - 40;
        RecipeCategory category = categories.get(selectedCategoryIndex);
        RecipeDisplay display = categoryMap.get(category).get(selectedRecipeIndex);
        int guiWidth = MathHelper.clamp(category.getDisplayWidth(display) + 30, 0, largestWidth) + 100;
        int guiHeight = MathHelper.clamp(category.getDisplayHeight() + 40, 166, largestHeight);
        this.bounds = new Rectangle(width / 2 - guiWidth / 2, height / 2 - guiHeight / 2, guiWidth, guiHeight);
        this.widgets.add(new CategoryBaseWidget(bounds));
        this.scrollListBounds = new Rectangle(bounds.x + 4, bounds.y + 17, 97 + 5, guiHeight - 17 - 7);
        this.widgets.add(new SlotBaseWidget(scrollListBounds));
        
        Rectangle recipeBounds = new Rectangle(bounds.x + 100 + (guiWidth - 100) / 2 - category.getDisplayWidth(display) / 2, bounds.y + bounds.height / 2 - category.getDisplayHeight() / 2, category.getDisplayWidth(display), category.getDisplayHeight());
        this.widgets.addAll(category.setupDisplay(() -> display, recipeBounds));
        Optional<ButtonAreaSupplier> supplier = RecipeHelper.getInstance().getSpeedCraftButtonArea(category);
        final SpeedCraftFunctional functional = getSpeedCraftFunctionalByCategory(ScreenHelper.getLastContainerScreen(), category);
        if (supplier.isPresent())
            widgets.add(new SpeedCraftingButtonWidget(supplier.get().get(recipeBounds), supplier.get().getButtonText(), functional, () -> display));
        
        int index = 0;
        for(RecipeDisplay recipeDisplay : categoryMap.get(category)) {
            int finalIndex = index;
            RecipeRenderer recipeRenderer;
            recipeRenderers.add(recipeRenderer = category.getSimpleRenderer(recipeDisplay));
            buttonWidgets.add(new ButtonWidget(bounds.x + 5, 0, recipeRenderer.getWidth(), recipeRenderer.getHeight(), "") {
                @Override
                public void onPressed() {
                    selectedRecipeIndex = finalIndex;
                    VillagerRecipeViewingScreen.this.init();
                }
            });
            index++;
        }
        for(int i = 0; i < TABS_PER_PAGE; i++) {
            int j = i + tabsPage * TABS_PER_PAGE;
            if (categories.size() > j) {
                TabWidget tab;
                tabs.add(tab = new TabWidget(i, new Rectangle(bounds.x + bounds.width / 2 - Math.min(categories.size() - tabsPage * TABS_PER_PAGE, TABS_PER_PAGE) * 14 + i * 28, bounds.y - 28, 28, 28)) {
                    @Override
                    public boolean mouseClicked(double mouseX, double mouseY, int button) {
                        if (getBounds().contains(mouseX, mouseY)) {
                            MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                            if (getId() + tabsPage * TABS_PER_PAGE == selectedCategoryIndex)
                                return false;
                            selectedCategoryIndex = getId() + tabsPage * TABS_PER_PAGE;
                            scroll = 0;
                            VillagerRecipeViewingScreen.this.init();
                            return true;
                        }
                        return false;
                    }
                });
                tab.setRenderer(categories.get(j), categories.get(j).getIcon(), categories.get(j).getCategoryName(), tab.getId() + tabsPage * TABS_PER_PAGE == selectedCategoryIndex);
            }
        }
        
        this.widgets.add(new ClickableLabelWidget(bounds.x + 4 + scrollListBounds.width / 2, bounds.y + 6, categories.get(selectedCategoryIndex).getCategoryName()) {
            @Override
            public void onLabelClicked() {
                MinecraftClient.getInstance().getSoundManager().play(PositionedSoundInstance.master(SoundEvents.UI_BUTTON_CLICK, 1.0F));
                ClientHelper.getInstance().executeViewAllRecipesKeyBind();
            }
            
            @Override
            public Optional<String> getTooltips() {
                return Optional.ofNullable(I18n.translate("text.rei.view_all_categories"));
            }
            
            @Override
            public void render(int mouseX, int mouseY, float delta) {
                GlStateManager.color4f(1.0F, 1.0F, 1.0F, 1.0F);
                int colour = getDefaultColor();
                if (clickable && isHovered(mouseX, mouseY))
                    colour = getHoveredColor();
                font.draw((isHovered(mouseX, mouseY) ? "§n" : "") + text, x - font.getStringWidth(text) / 2, y, colour);
                if (clickable && getTooltips().isPresent())
                    if (!focused && isHighlighted(mouseX, mouseY))
                        ScreenHelper.getLastOverlay().addTooltip(QueuedTooltip.create(getTooltips().get().split("\n")));
                    else if (focused)
                        ScreenHelper.getLastOverlay().addTooltip(QueuedTooltip.create(new Point(x, y), getTooltips().get().split("\n")));
            }
    
            @Override
            public int getHoveredColor() {
                return -1;
            }
    
            @Override
            public int getDefaultColor() {
                return 4210752;
            }
        });
        this.children.addAll(buttonWidgets);
        this.widgets.addAll(tabs);
        this.children.addAll(widgets);
        this.children.add(ScreenHelper.getLastOverlay(true, false));
        ScreenHelper.getLastOverlay().init();
    }
    
    @Override
    public boolean mouseScrolled(double double_1, double double_2, double double_3) {
        double height = buttonWidgets.stream().map(ButtonWidget::getBounds).collect(Collectors.summingDouble(Rectangle::getHeight));
        if (scrollListBounds.contains(double_1, double_2) && height > scrollListBounds.height - 2) {
            if (double_3 > 0)
                scroll -= 16;
            else
                scroll += 16;
            scroll = MathHelper.clamp(scroll, 0, height - scrollListBounds.height + 2);
            return true;
        }
        return super.mouseScrolled(double_1, double_2, double_3);
    }
    
    @Override
    public void render(int mouseX, int mouseY, float delta) {
        this.fillGradient(0, 0, this.width, this.height, -1072689136, -804253680);
        int yOffset = 0;
        this.widgets.forEach(widget -> {
            GuiLighting.disable();
            widget.render(mouseX, mouseY, delta);
        });
        GuiLighting.disable();
        ScreenHelper.getLastOverlay().render(mouseX, mouseY, delta);
        GL11.glPushMatrix();
        Scissors.begin();
        Scissors.scissor(0, scrollListBounds.y + 1, width, scrollListBounds.height - 2);
        for(int i = 0; i < buttonWidgets.size(); i++) {
            ButtonWidget buttonWidget = buttonWidgets.get(i);
            buttonWidget.getBounds().y = scrollListBounds.y + 1 + yOffset - (int) scroll;
            if (buttonWidget.getBounds().getMaxY() > scrollListBounds.getMinY() && buttonWidget.getBounds().getMinY() < scrollListBounds.getMaxY()) {
                GuiLighting.disable();
                buttonWidget.render(mouseX, mouseY, delta);
            }
            yOffset += buttonWidget.getBounds().height;
        }
        for(int i = 0; i < buttonWidgets.size(); i++) {
            if (buttonWidgets.get(i).getBounds().getMaxY() > scrollListBounds.getMinY() && buttonWidgets.get(i).getBounds().getMinY() < scrollListBounds.getMaxY()) {
                GuiLighting.disable();
                recipeRenderers.get(i).setBlitOffset(1);
                recipeRenderers.get(i).render(buttonWidgets.get(i).getBounds().x, buttonWidgets.get(i).getBounds().y, mouseX, mouseY, delta);
            }
        }
        Scissors.end();
        GL11.glPopMatrix();
        ScreenHelper.getLastOverlay().lateRender(mouseX, mouseY, delta);
    }
    
    private int getTitleBarHeight() {
        IntBuffer useless = BufferUtils.createIntBuffer(3), top = BufferUtils.createIntBuffer(1);
        GLFW.glfwGetWindowFrameSize(minecraft.window.getHandle(), useless, top, useless, useless);
        System.out.println(top.get(0));
        return top.get(0) / 3 * 2;
    }
    
    private int getReal(int i) {
        return (int) (i / ((double) minecraft.window.getScaledWidth() / (double) minecraft.window.getWidth()));
    }
    
    @Override
    public boolean keyPressed(int int_1, int int_2, int int_3) {
        if ((int_1 == 256 || this.minecraft.options.keyInventory.matchesKey(int_1, int_2)) && this.shouldCloseOnEsc()) {
            MinecraftClient.getInstance().openScreen(ScreenHelper.getLastContainerScreen());
            ScreenHelper.getLastOverlay().init();
            return true;
        }
        if (int_1 == 258) {
            boolean boolean_1 = !hasShiftDown();
            if (!this.changeFocus(boolean_1))
                this.changeFocus(boolean_1);
            return true;
        }
        if (ClientHelper.getInstance().getNextPageKeyBinding().matchesKey(int_1, int_2)) {
            if (categoryMap.get(categories.get(selectedCategoryIndex)).size() > 1) {
                selectedCategoryIndex++;
                if (selectedCategoryIndex >= categoryMap.get(categories.get(selectedCategoryIndex)).size())
                    selectedCategoryIndex = 0;
                init();
                return true;
            }
            return false;
        } else if (ClientHelper.getInstance().getPreviousPageKeyBinding().matchesKey(int_1, int_2)) {
            if (categoryMap.get(categories.get(selectedCategoryIndex)).size() > 1) {
                selectedCategoryIndex--;
                if (selectedCategoryIndex < 0)
                    selectedCategoryIndex = categoryMap.get(categories.get(selectedCategoryIndex)).size() - 1;
                init();
                return true;
            }
            return false;
        }
        for(Element element : children())
            if (element.keyPressed(int_1, int_2, int_3))
                return true;
        return super.keyPressed(int_1, int_2, int_3);
    }
    
}
