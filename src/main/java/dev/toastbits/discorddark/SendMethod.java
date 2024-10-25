package dev.toastbits.discorddark;

import java.util.List;
import com.google.common.collect.Lists;
import net.minecraft.text.Text;
import static net.minecraft.text.Text.literal;

public class SendMethod {

    enum InfoType {
        NAME, PLAYER, COORDS, BIOME, DIMENSION
    }

    public static Text[] getInfoTypeTooltip(String main_tooltip) {
        List<Text> ret = Lists.newArrayList(literal(main_tooltip));
        String types = "";
        for (SendMethod.InfoType type : SendMethod.InfoType.values()) {
            types += String.format("\n%s: %s", type.name(), Utils.Translatable.gets(String.format("infotype.discorddark.%s.tooltip", type.name().toLowerCase())));
        }
        ret.add(literal(types));
        return ret.toArray(new Text[0]);
    }

    public boolean methodMatches(String method) {
        return method.toLowerCase().equals(identifier.toLowerCase());
    }

    String identifier = "";
    long guild_id = 0L;
    long channel_id = 0L;
    int embed_colour = 0xffffff;
    boolean use_dimension_colour = true;
    boolean play_sound = true;
    boolean require_name = false;
    boolean notify = true;

    boolean include_name = true;
    boolean include_player = true;
    boolean include_screenshot = true;
    boolean inline_fields = true;

    boolean hide_hud = false;
    boolean hide_hand = false;

    static final String d_identifier = "";
    static final long d_guild_id = 0L;
    static final long d_channel_id = 0L;
    static final int d_embed_colour = 0xffffff;
    static final boolean d_use_dimension_colour = true;
    static final boolean d_play_sound = true;
    static final boolean d_include_name = true;
    static final boolean d_include_player = true;
    static final boolean d_include_screenshot = true;
    static final boolean d_inline_fields = true;
    static final boolean d_hide_hud = false;
    static final boolean d_hide_hand = false;
    static final boolean d_require_name = false;
    static final boolean d_notify = true;

    List<InfoType> field_info = Lists.newArrayList();
    List<InfoType> footer_info = Lists.newArrayList();
}
