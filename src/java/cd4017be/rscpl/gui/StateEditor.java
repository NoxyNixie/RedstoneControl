package cd4017be.rscpl.gui;

import java.util.Arrays;
import java.util.function.IntConsumer;
import cd4017be.lib.Gui.comp.FormatText;
import cd4017be.lib.Gui.comp.GuiCompGroup;
import cd4017be.lib.Gui.comp.GuiFrame;
import cd4017be.lib.Gui.comp.Slider;
import cd4017be.lib.Gui.comp.TextField;
import cd4017be.lib.util.TooltipUtil;
import cd4017be.rs_ctr.circuit.Circuit;
import cd4017be.rscpl.util.StateBuffer;
import net.minecraft.nbt.NBTBase;
import net.minecraft.nbt.NBTPrimitive;
import net.minecraft.nbt.NBTTagByteArray;
import net.minecraft.nbt.NBTTagIntArray;
import static net.minecraftforge.common.util.Constants.NBT.*;

/**
 * @author CD4017BE
 *
 */
public class StateEditor extends GuiCompGroup {

	private final int maxVar;
	public final String[] keys;
	private StateBuffer state;
	public final Circuit circuit;
	public final IntConsumer set;
	public final boolean editIO;
	public boolean hex;
	int scroll;

	/**
	 * @param parent
	 * @param circuit
	 * @param ioLabels
	 * @param max
	 * @param set
	 * @return
	 */
	public static StateEditor of(GuiFrame parent, Circuit circuit, String[] ioLabels, int max, IntConsumer set, boolean editIO) {
		StateBuffer state = circuit.getState();
		String[] keys = state.nbt.getKeySet().toArray(new String[state.nbt.getSize()]);
		Arrays.sort(keys);
		return new StateEditor(parent, circuit, state, keys, ioLabels, Math.min(max, keys.length), set, editIO);
	}

	private StateEditor(GuiFrame parent, Circuit circuit, StateBuffer state, String[] keys, String[] ioLabels, int n, IntConsumer set, boolean editIO) {
		super(parent, 168, 19 + (circuit.inputs.length + circuit.outputs.length + n) * 18 + (n > 0 ? 12 : 0), 2 * (circuit.inputs.length + circuit.outputs.length + n) + 1);
		parent.extendBy(this);
		this.circuit = circuit;
		this.state = circuit.getState();
		this.set = set;
		this.keys = keys;
		this.maxVar = n;
		this.editIO = editIO;
		int y = 12, in = circuit.inputs.length;
		for (int i = 0; i < in; i++, y += 9) {
			final String label = ioLabels[i];
			final int idx = i;
			new FormatText(this, 70, 7, 8, y + 1, "\\" + label.replace("%", "%%"), null).color(0xff00007f);
			if (editIO) new TextField(this, 70, 7, 90, y + 1, 16, ()-> Integer.toString(this.circuit.inputs[idx], hex ? 16 : 10), (t)-> {try {
						int v = Integer.parseInt(t, hex ? 16 : 10);
						if (v == circuit.inputs[idx]) return;
						circuit.inputs[idx] = v;
						set.accept(-1 - idx);
					} catch (NumberFormatException e) {}});
			else new FormatText(this, 70, 7, 90, y + 1, "\\%s", ()-> new Object[] {Integer.toString(this.circuit.inputs[idx], hex ? 16 : 10)});
		}
		int out = circuit.outputs.length;
		for (int i = 0; i < out; i++, y += 9) {
			final String label = ioLabels[in + i];
			final int idx = i;
			new FormatText(this, 70, 7, 8, y + 1, "\\" + label.replace("%", "%%"), null).color(0xff007f00);
			new FormatText(this, 70, 7, 90, y + 1, "\\%s", ()-> new Object[] {Integer.toString(circuit.outputs[idx], hex ? 16 : 10)});
		}
		y += 12;
		if (keys.length > n)
			new Slider(this, 8, 12, n * 9 - 2, 152, y + 1, 178, 0, false, ()-> scroll, (v)-> scroll = (int)Math.round(v), null, 0, keys.length - n);
		for (int i = 0; i < n; i++, y += 9) {
			final int idx = i;
			new FormatText(this, 70, 7, 8, y + 1, "\\%s", ()-> new Object[] {keys[idx + scroll]});
			new TextField(this, 70, 7, 80, y + 1, 1024, ()-> getValue(idx), (t)-> setValue(idx, t));
		}
	}

	private String getValue(int i) {
		NBTBase tag = state.nbt.getTag(keys[i += scroll]);
		if (tag instanceof NBTPrimitive) {
			NBTPrimitive ptag = (NBTPrimitive)tag;
			switch(tag.getId()) {
			case TAG_BYTE: return String.format(hex ? "%02X" : "%d", ptag.getByte());
			case TAG_SHORT: return String.format(hex ? "%04X" : "%d", ptag.getShort());
			case TAG_INT: return String.format(hex ? "%08X" : "%d", ptag.getInt());
			case TAG_LONG: return String.format(hex ? "%016X" : "%d", ptag.getLong());
			case TAG_FLOAT: return String.format(hex ? "%a" : "%f", ptag.getFloat());
			case TAG_DOUBLE: return String.format(hex ? "%a" : "%f", ptag.getDouble());
			}
		} else if (tag instanceof NBTTagByteArray) {
			byte[] arr = ((NBTTagByteArray)tag).getByteArray();
			int l = arr.length, ll = hex ? Integer.toHexString(l - 1).length() : Integer.toString(l - 1).length();
			String fmt = hex ? "%0" + ll + "x:%02x " : "%d:%d ";
			StringBuilder sb = new StringBuilder((ll + 3) * l);
			for (int j = 0; j < arr.length; j++)
				sb.append(String.format(fmt, j, arr[j] & 0xff));
			sb.deleteCharAt(sb.length() - 1);
			return sb.toString();
		} else if (tag instanceof NBTTagIntArray) {
			int[] arr = ((NBTTagIntArray)tag).getIntArray();
			int l = arr.length, ll = hex ? Integer.toHexString(l - 1).length() : Integer.toString(l - 1).length();
			String fmt = hex ? "%0" + ll + "x:%08x " : "%d:%d ";
			StringBuilder sb = new StringBuilder((ll + (hex ? 8 : 5)) * l);
			for (int j = 0; j < arr.length; j++)
				sb.append(String.format(fmt, j, arr[j]));
			sb.deleteCharAt(sb.length() - 1);
			return sb.toString();
		}
		return tag.toString();
	}

	private void setValue(int i, String t) {
		String key = keys[i += scroll];
		try {
			switch(state.nbt.getTagId(key)) {
			case TAG_BYTE: state.set(key, (byte)parseNumber(t, Byte.MAX_VALUE)); break;
			case TAG_SHORT: state.set(key, (short)parseNumber(t, Short.MAX_VALUE)); break;
			case TAG_INT: state.set(key, (int)parseNumber(t, Integer.MAX_VALUE)); break;
			case TAG_LONG: state.set(key, parseNumber(t, Long.MAX_VALUE)); break;
			case TAG_FLOAT: state.set(key, Float.parseFloat(t)); break;
			case TAG_DOUBLE: state.set(key, Double.parseDouble(t)); break;
			case TAG_BYTE_ARRAY: {
				int rad = hex ? 16 : 10;
				byte[] arr = state.nbt.getByteArray(key);
				for (int p = t.indexOf(':'), q = -1; p >= 0; p = t.indexOf(':', p + 1)) {
					int j = Integer.parseInt(t.substring(q + 1, p), rad);
					if (j >= arr.length) continue;
					if ((q = t.indexOf(' ', p)) < 0) q = t.length();
					arr[j] = (byte)Integer.parseInt(t.substring(p + 1, q), rad);
				}
			}	break;
			case TAG_INT_ARRAY: {
				int rad = hex ? 16 : 10;
				int[] arr = state.nbt.getIntArray(key);
				for (int p = t.indexOf(':'), q = -1; p >= 0; p = t.indexOf(':', p + 1)) {
					int j = Integer.parseInt(t.substring(q + 1, p), rad);
					if (j >= arr.length) continue;
					if ((q = t.indexOf(' ', p)) < 0) q = t.length();
					arr[j] = Integer.parseInt(t.substring(p + 1, q), rad);
				}
			}	break;
			default: return;
			}
			circuit.setState(state);
			set.accept(i);
		} catch (NumberFormatException e) {}
	}

	private long parseNumber(String t, long max) {
		if (t.isEmpty()) throw new NumberFormatException();
		int rad = hex ? 16 : 10;
		boolean usgn = hex || t.charAt(0) == '+';
		long l;
		if (usgn) {
			l = Long.parseUnsignedLong(t, rad);
			if (max != Long.MAX_VALUE && (l < 0 || l > max * 2 + 1))
				throw new NumberFormatException();
		} else {
			l = Long.parseLong(t, rad);
			if (l > max || l < -1 - max)
				throw new NumberFormatException();
		}
		return l;
	}

	@Override
	public void drawBackground(int mx, int my, float t) {
		int y = this.y;
		drawRect(x, y, 0, 165, 168, 12);
		y += 12;
		for (int n = circuit.inputs.length; n > 0; n--, y += 9)
			drawRect(x, y, 0, editIO ? 147 : 81, 168, 9);
		for (int n = circuit.outputs.length; n > 0; n--, y += 9)
			drawRect(x, y, 0, 156, 168, 9);
		if (maxVar > 0) {
			int m = maxVar * 9, m2 = m/2;
			drawRect(x, y, 0, 165, 168, 12 + m - m2);
			drawRect(x, y + 12 + m - m2, 0, 249 - m2, 168, 7 + m2);
			String title = TooltipUtil.translate("gui.rs_ctr.state.name");
			drawNow();
			fontRenderer.drawString(title, x + (w - fontRenderer.getStringWidth(title)) / 2, y + 2, 0x404040);
		} else {
			drawRect(x, y, 0, 249, 168, 7);
			drawNow();
		}
		String title = TooltipUtil.translate("gui.rs_ctr.state.io");
		fontRenderer.drawString(title, x + (w - fontRenderer.getStringWidth(title)) / 2, this.y + 2, 0x404040);
		super.drawBackground(mx, my, t);
	}

	public void update() {
		state = circuit.getState();
	}

}
