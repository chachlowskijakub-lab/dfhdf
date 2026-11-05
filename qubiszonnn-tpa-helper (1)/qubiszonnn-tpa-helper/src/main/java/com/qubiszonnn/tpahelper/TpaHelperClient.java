package com.qubiszonnn.tpahelper;

import com.qubiszonnn.tpahelper.gui.TpaScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;

public class TpaHelperClient implements ClientModInitializer {
    public static final String MOD_ID = "qubiszonnn_tpa_helper";
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key." + MOD_ID + ".open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_G, category));

        ChatListener.register();

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> ChatTracker.clear());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openKey.wasPressed()) {
                if (client.player != null && client.currentScreen == null) {
                    client.setScreen(new TpaScreen());
                }
            }
        });
    }
}
