package neuromuser.bta_autostack.mixin;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.container.ScreenContainerAbstract;
import net.minecraft.core.InventoryAction;
import net.minecraft.core.item.Item;
import net.minecraft.core.item.ItemArmor;
import net.minecraft.core.item.ItemStack;
import net.minecraft.core.item.tool.ItemTool;
import net.minecraft.core.item.tool.ItemToolSword;
import net.minecraft.core.player.inventory.menu.MenuAbstract;
import net.minecraft.core.player.inventory.slot.Slot;
import net.minecraft.core.sound.SoundCategory;
import org.lwjgl.input.Keyboard;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Objects;

@Mixin(value = ScreenContainerAbstract.class, remap = false)
public abstract class ScreenContainerAbstractMixin {

	@Shadow
	public MenuAbstract inventorySlots;

	@Shadow
	public abstract Slot getSlotAtPosition(int i, int j);

	@Inject(method = "clickInventory", at = @At("HEAD"), cancellable = true)
	private void handleAutoStack(int x, int y, int mouseButton, CallbackInfo ci) {
		boolean shiftPressed = Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);

		if (mouseButton == 2 && shiftPressed) {
			Slot clickedSlot = this.getSlotAtPosition(x, y);

			if (clickedSlot != null) {
				Minecraft mc = Minecraft.getMinecraft();
				Object targetInventory = clickedSlot.getContainer();
				boolean itemsMoved = false;

				for (int i = 0; i < inventorySlots.slots.size(); i++) {
					Slot playerSlot = inventorySlots.slots.get(i);

					if (playerSlot.getContainer() != targetInventory && playerSlot.hasItem()) {
						ItemStack playerStack = playerSlot.getItemStack();
						assert playerStack != null;
						Item item = playerStack.getItem();

						if (item instanceof ItemTool || item instanceof ItemToolSword || item instanceof ItemArmor ||
							item.getKey().contains("arrow") || item.getKey().contains("torch")) {
							continue;
						}

						boolean chestHasItem = false;
						for (int j = 0; j < inventorySlots.slots.size(); j++) {
							Slot chestSlot = inventorySlots.slots.get(j);
							if (chestSlot.getContainer() == targetInventory && chestSlot.hasItem()) {
								if (Objects.requireNonNull(chestSlot.getItemStack()).itemID == playerStack.itemID) {
									chestHasItem = true;
									break;
								}
							}
						}

						if (chestHasItem) {
							mc.playerController.handleInventoryMouseClick(
								inventorySlots.containerId,
								InventoryAction.MOVE_STACK,
								new int[]{i, 0},
								mc.thePlayer
							);
							itemsMoved = true;
						}
					}
				}

				if (itemsMoved) {
					mc.sndManager.playSound("random.pop", SoundCategory.GUI_SOUNDS, 0.25F, 1.2F);
				}

				ci.cancel();
			}
		}
	}
}
