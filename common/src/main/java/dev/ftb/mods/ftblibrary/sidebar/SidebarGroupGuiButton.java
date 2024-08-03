package dev.ftb.mods.ftblibrary.sidebar;

import com.mojang.blaze3d.systems.RenderSystem;
import dev.ftb.mods.ftblibrary.FTBLibrary;
import dev.ftb.mods.ftblibrary.FTBLibraryClient;
import dev.ftb.mods.ftblibrary.icon.Color4I;
import dev.ftb.mods.ftblibrary.icon.Icons;
import dev.ftb.mods.ftblibrary.ui.Button;
import dev.ftb.mods.ftblibrary.ui.GuiHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

public class SidebarGroupGuiButton extends AbstractButton {
	public static Rect2i lastDrawnArea = new Rect2i(0, 0, 0, 0);
	private static final int BUTTON_SPACING = 17;

	private SidebarGuiButton mouseOver;
	private SidebarGuiButton selectedButton;
	private GridLocation selectedLocation;
	private boolean isMouseDown;
	private int mouseDownTime;
	private boolean isEditMode;
	private int currentMouseX;
	private int currentMouseY;

	private int mouseOffsetX;
	private int mouseOffsetY;

	private int maxGridWith = 1;
	private int maxGridHeight = 1;

	private boolean addBoxOpen;

	private final Map<SidebarGuiButton, GridLocation> realLocationMap = new HashMap<>();

	public SidebarGroupGuiButton() {
		super(0, 0, 0, 0, Component.empty());

		ensureGridAlignment();
		isMouseDown = false;
	}

	@Override
	public void renderWidget(GuiGraphics graphics, int mx, int my, float partialTicks) {
		graphics.pose().pushPose();
		graphics.pose().translate(0, 0, 5000);
		//Todo always?
		addBoxOpen = true;
		currentMouseX = mx;
		currentMouseY = my;
		mouseOver = null;
		GridLocation gridLocation = getGridLocation();

		for (Map.Entry<SidebarGuiButton, GridLocation> entry : realLocationMap.entrySet()) {
			SidebarGuiButton button = entry.getKey();
			if (entry.getValue().equals(gridLocation)) {
				mouseOver = button;
			}
		}

		maxGridWith = 1;
		maxGridHeight = 1;
		for (Map.Entry<SidebarGuiButton, GridLocation> value : realLocationMap.entrySet()) {
			GridLocation location = value.getValue();
			maxGridWith = Math.max(maxGridWith, location.x() + 1);
			maxGridHeight = Math.max(maxGridHeight, location.y() + 1);
		}

		if (isEditMode) {
			maxGridWith += 1;
			maxGridHeight += 1;

			drawHoveredGrid(graphics, 0, 0, maxGridWith, maxGridHeight, BUTTON_SPACING, Color4I.GRAY.withAlpha(70), Color4I.BLACK.withAlpha(90), mx, my);

			List<SidebarGuiButton> disabledButtonList = SidebarButtonManager.INSTANCE.getDisabledButtonList(isEditMode);
			if (!disabledButtonList.isEmpty()) {
				drawGrid(graphics, (maxGridWith + 1) * BUTTON_SPACING, 0, 1, 1, BUTTON_SPACING, BUTTON_SPACING, Color4I.GRAY, Color4I.BLACK);
				Icons.ADD.draw(graphics, (maxGridWith + 1) * BUTTON_SPACING + 1, 1, 16, 16);

				if (addBoxOpen) {
					int maxWidth = 0;
					for (SidebarGuiButton button : disabledButtonList) {
						String s = I18n.get(button.getSidebarButton().getLangKey());
						int width = Minecraft.getInstance().font.width(s);
						maxWidth = Math.max(maxWidth, width);
					}

					drawGrid(graphics, (maxGridWith + 1) * BUTTON_SPACING, BUTTON_SPACING, 1, disabledButtonList.size(), maxWidth + BUTTON_SPACING + 4, BUTTON_SPACING, Color4I.GRAY, Color4I.BLACK);

					for (int i = 0; i < disabledButtonList.size(); i++) {
						SidebarGuiButton button = disabledButtonList.get(i);
						if (selectedButton != null && selectedButton == button) {
							continue;
						}
						button.x = (maxGridWith + 1) * BUTTON_SPACING;
						button.y = BUTTON_SPACING + BUTTON_SPACING * i;
						GuiHelper.setupDrawing();
						button.getSidebarButton().getData().icon().draw(graphics, button.x + 1, button.y + 1, 16, 16);
						graphics.drawString(Minecraft.getInstance().font, I18n.get(button.getSidebarButton().getLangKey()), button.x + 20, button.y + 4, 0xFFFFFFFF);

						if (mx >= button.x && mx < button.x + 16 && my >= button.y && my < button.y + 16) {
							mouseOver = button;
						}
					}

				}
			}

		}


		var font = Minecraft.getInstance().font;

		for (SidebarGuiButton button : SidebarButtonManager.INSTANCE.getButtonList()) {
			GridLocation realGridLocation = realLocationMap.get(button);
			if (isEditMode || (button.equals(selectedButton) || button.isEnabled())) {
				graphics.pose().pushPose();
				if (isEditMode && button == selectedButton) {
					graphics.pose().translate(0, 0, 5000);
					button.x = mx - mouseOffsetX;
					button.y = my - mouseOffsetY;
				} else {
					if(realGridLocation == null) {
						continue;
					}
					button.x = 1 + realGridLocation.x() * BUTTON_SPACING;
					button.y = 1 + realGridLocation.y() * BUTTON_SPACING;
				}
				GuiHelper.setupDrawing();
				button.getSidebarButton().getData().icon().draw(graphics, button.x, button.y, 16, 16);

				if (isEditMode && button != selectedButton && SidebarButtonManager.INSTANCE.getEnabledButtonList(isEditMode).size() > 1) {
					if (mx >= button.x + 12 && my <= button.y + 4 && mx < button.x + 16 && my >= button.y) {
						Icons.CANCEL.draw(graphics, button.x + 11, button.y - 1, 6, 6);
					} else {
						Icons.CANCEL.draw(graphics, button.x + 12, button.y, 4, 4);
					}
				}

				if (button == mouseOver) {
					Color4I.WHITE.withAlpha(33).draw(graphics, button.x, button.y, 16, 16);
				}

				if (button.getSidebarButton().getCustomTextHandler() != null) {
					var text = button.getSidebarButton().getCustomTextHandler().get();

					if (!text.isEmpty()) {
						var nw = font.width(text);
						var width = 16;
						Color4I.LIGHT_RED.draw(graphics, button.x + width - nw, button.y - 1, nw + 1, 9);
						graphics.drawString(font, text, button.x + width - nw + 1, button.y, 0xFFFFFFFF);
						RenderSystem.setShaderColor(1F, 1F, 1F, 1F);
					}
				}
				graphics.pose().popPose();
			}
			if(!isEditMode && mouseOver == button) {
				GuiHelper.setupDrawing();
				var mx1 = mx + 10;
				var my1 = Math.max(3, my - 9);

				List<String> list = new ArrayList<>();
				list.add(I18n.get(mouseOver.getSidebarButton().getLangKey()));

				if (mouseOver.getSidebarButton().getTooltipHandler() != null) {
					mouseOver.getSidebarButton().getTooltipHandler().accept(list);
				}

				var tw = 0;

				for (String s : list) {
					tw = Math.max(tw, font.width(s));
				}

				Color4I.DARK_GRAY.draw(graphics, mx1 - 3, my1 - 2, tw + 6, 2 + list.size() * 10);

				for (var i = 0; i < list.size(); i++) {
					graphics.drawString(font, list.get(i), mx1, my1 + i * 10, 0xFFFFFFFF);
				}
			}
		}

		graphics.pose().popPose();

	}

	@Override
	public void onRelease(double d, double e) {
		super.onRelease(d, e);
		isMouseDown = false;
		//Normal click action
		if (!isEditMode && mouseOver != null) {
			mouseOver.getSidebarButton().clickButton(Screen.hasShiftDown());
		} else {
			if (selectedButton != null) {
				GridLocation gLocation = getGridLocation();
				//Make sure the placement is in grid
                if (!gLocation.isOutOfBounds()) {
					//Checks if the icon is placed in the same location picked up from, if so do nothing
					if (!gLocation.equals(selectedLocation)) {
						//checks if moved from the first spot, so we can move other icons over but only as the same row
						boolean isFrom0XTo1X = selectedLocation.y() == gLocation.y() && selectedLocation.x() == 0 && gLocation.x() == 1;
						selectedButton.setGridLocation(gLocation.x(), gLocation.y());
						//Checks for icon that needs to be moved over
						List<SidebarGuiButton> buttonList = SidebarButtonManager.INSTANCE.getButtonList();
                        for (SidebarGuiButton button : buttonList) {
							GridLocation realGridLocation = realLocationMap.get(button);
							if(realGridLocation != null) {
								if (!selectedButton.getSidebarButton().getId().equals(button.getSidebarButton().getId())) {
									if (gLocation.isLatterInColumn(realGridLocation)) {
										int moveAmount = isFrom0XTo1X && realGridLocation.x() == 1 ? -1 : 1;
										button.setGridLocation(realGridLocation.x() + moveAmount, realGridLocation.y());
									}
								}
							}
                        }
						//If the icon was disabled enable it
                        if (!selectedButton.isEnabled()) {
                            selectedButton.setEnabled(true);
							//Todo do we want this
							if (SidebarButtonManager.INSTANCE.getDisabledButtonList(isEditMode).isEmpty()) {
								addBoxOpen = false;
							}
                        }
                    }
                }
				selectedButton = null;
				ensureGridAlignment();
			}
		}
	}

	private void ensureGridAlignment() {
		List<SidebarGuiButton> enabledButtonList = SidebarButtonManager.INSTANCE.getEnabledButtonList(isEditMode);
		Map<Integer, List<SidebarGuiButton>> buttonMap = enabledButtonList
				.stream()
				.filter(SidebarGuiButton::isEnabled)
				.collect(Collectors.groupingBy(button -> button.getGirdLocation().y(), TreeMap::new, Collectors.toCollection(LinkedList::new)));

        realLocationMap.clear();

		int y = 0;
		for (Map.Entry<Integer, List<SidebarGuiButton>> entry : buttonMap.entrySet()) {
			entry.getValue().sort(Comparator.comparingInt(b -> b.getGirdLocation().x()));
			int x = 0;
			for (SidebarGuiButton button : entry.getValue()) {
				realLocationMap.put(button, new GridLocation(x, y));
				x++;
			}
			if (x != 0) {
				y++;
			}
		}


		SidebarButtonManager.INSTANCE.saveConfigFromButtonList();
		updateWidgetSize();
	}

	private void updateWidgetSize() {
		// Important: JEI doesn't like negative X/Y values and will silently clamp them,
		// leading it to think the values have changed every frame, and do unnecessary updating
		// of its GUI areas, including resetting the filter textfield's selection
		// https://github.com/FTBTeam/FTB-Mods-Issues/issues/262
		// https://github.com/mezz/JustEnoughItems/issues/2938
		var maxGirdX = 1;
		var maxGirdY = 1;
		for (SidebarGuiButton b : SidebarButtonManager.INSTANCE.getEnabledButtonList(isEditMode)) {
			maxGirdX = Math.max(maxGirdX, b.getGirdLocation().x() + 1);
			maxGirdY = Math.max(maxGirdY, b.getGirdLocation().y() + 1);
		}

		int disabledList = SidebarButtonManager.INSTANCE.getDisabledButtonList(isEditMode).size();
		if(isEditMode && addBoxOpen) {
			maxGirdX += 4;
            maxGirdY = Math.max(maxGirdY, disabledList);
		}
		if(isEditMode) {
			maxGirdX += 3;
			maxGirdY += 1;
		}


		width = (maxGirdX) * BUTTON_SPACING;
		height = (maxGirdY) * BUTTON_SPACING;


		setX(0);
		setY(0);

		lastDrawnArea = new Rect2i(getX(), getY(), width, height);
	}

	private GridLocation getGridLocation() {
		int gridX = (currentMouseX - 1) / BUTTON_SPACING;
		int gridY = (currentMouseY - 1) / BUTTON_SPACING;
		if(gridX >= maxGridWith || gridY >= maxGridHeight) {
			return GridLocation.OUT_OF_BOUNDS;
		}
		return new GridLocation(gridX, gridY);
	}

	@Override
	public void onPress() {
		if(isEditMode) {
			if(currentMouseX >= ( maxGridWith + 1) * BUTTON_SPACING && currentMouseY <= BUTTON_SPACING) {
				addBoxOpen = !addBoxOpen;
				updateWidgetSize();
				return;
			}
		}

		if(mouseOver != null) {
			isMouseDown = true;
			mouseOffsetX = currentMouseX - mouseOver.x;
			mouseOffsetY = currentMouseY - mouseOver.y;

			if(isEditMode) {
				if(SidebarButtonManager.INSTANCE.getEnabledButtonList(isEditMode).size() > 1 && currentMouseX >= mouseOver.x + 11 && currentMouseY <= mouseOver.y + 3) {
					mouseOver.setEnabled(false);
					mouseOver = null;
					ensureGridAlignment();
					return;
				}

				selectedButton = mouseOver;
				GridLocation realGridLocation = realLocationMap.get(selectedButton);
				selectedLocation = realGridLocation == null ? selectedButton.getGirdLocation() : realGridLocation;
			}
		}
	}

	@Override
	public void updateWidgetNarration(NarrationElementOutput narrationElementOutput) {
		defaultButtonNarrationText(narrationElementOutput);
	}


	//Custom handling so our button click locations are where a buttons are not just a box
	@Override
	protected boolean isValidClickButton(int i) {
		if (super.isValidClickButton(i)) {
			if (isEditMode) {
                return selectedButton != null || mouseOver != null;
			} else {
				return mouseOver != null;
			}
		}
		return false;
	}

	public void tick() {
		if(isMouseDown) {
			mouseDownTime++;
			if(!isEditMode && mouseDownTime > 20) {
				isEditMode = true;
				mouseOver = null;
				ensureGridAlignment();
				updateWidgetSize();
			}
		}else {
			mouseDownTime = 0;
		}
	}

	private static void drawGrid(GuiGraphics graphics, int x, int y, int width, int height, int spacingWidth, int spacingHeight, Color4I backgroundColor, Color4I gridColor) {
		backgroundColor.draw(graphics, x, y, width * spacingWidth, height * spacingHeight);
		for (var i = 0; i < width + 1; i++) {
			gridColor.draw(graphics, x + i * spacingWidth, y, 1, height * spacingHeight + 1);
		}
		for (var i = 0; i < height + 1; i++) {
			gridColor.draw(graphics, x, y + i * spacingHeight, width * spacingWidth, 1);
		}
	}

	public static void drawGrid(GuiGraphics graphics, int x, int y, int width, int height, int spacing, Color4I backgroundColor, Color4I gridColor) {
		drawGrid(graphics, x, y, width, height, spacing, spacing, backgroundColor, gridColor);
	}

	public static void drawHoveredGrid(GuiGraphics graphics, int x, int y, int width, int height, int spacing, Color4I backgroundColor, Color4I gridColor, int mx, int my) {
		drawGrid(graphics, x, y, width, height, spacing, backgroundColor, gridColor);
		if (mx >= x && my >= y && mx < x + width * spacing && my < y + height * spacing) {
			Color4I.WHITE.withAlpha(127).draw(graphics, (mx / spacing) * spacing + 1, (my / spacing) * spacing + 1, spacing - 1, spacing - 1);
		}
	}
}
