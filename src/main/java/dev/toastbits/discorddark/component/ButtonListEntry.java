package dev.toastbits.discorddark.component;

import com.google.common.collect.Lists;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;

import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;

@Environment(EnvType.CLIENT)
public class ButtonListEntry extends AbstractConfigListEntry<Object> {
    private final ButtonWidget buttonWidget;
    private final List<ClickableWidget> widgets;
    private Boolean changed = false;

    public ButtonListEntry(Text fieldName, ButtonListEntryCallback callback, Boolean changedOnPress) {
        super(fieldName, false);

        this.buttonWidget =
            ButtonWidget.builder(
                getDisplayedFieldName(),
                (widget) -> {
                    callback.run();
                    if (changedOnPress) {
                        changed = true;
                    }
                }
            )
            .dimensions(0, 0, 0, 20)
            .build();

        this.widgets = Lists.newArrayList(new ClickableWidget[]{this.buttonWidget});
    }

    public Object getValue() {
        return null;
    }

    public Optional<Object> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public boolean isEdited() {
        return changed;
    }

    public void render(DrawContext graphics, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean isHovered, float delta) {
        super.render(graphics, index, y, x, entryWidth, entryHeight, mouseX, mouseY, isHovered, delta);
        this.buttonWidget.setX(x);
        this.buttonWidget.setY(y);
        this.buttonWidget.setWidth(entryWidth);
        this.buttonWidget.render(graphics, mouseX, mouseY, delta);
    }

    public List<? extends Element> children() {
        return this.widgets;
    }

    public List<? extends Selectable> narratables() {
        return this.widgets;
    }
}
