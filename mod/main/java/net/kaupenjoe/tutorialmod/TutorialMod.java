package net.kaupenjoe.tutorialmod;

import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.CommandDispatcher;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.slf4j.Logger;
import com.mojang.logging.LogUtils;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

import java.lang.reflect.Method;

import com.mojang.brigadier.exceptions.CommandSyntaxException;

@Mod(TutorialMod.MOD_ID)
public class TutorialMod {
    public static final String MOD_ID = "tutorialmod";
    public static final Logger LOGGER = LogUtils.getLogger();

    public TutorialMod() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("prompt")
                        .then(Commands.argument("text", StringArgumentType.greedyString())
                                .executes(TutorialMod::handlePromptCommand))
        );
    }

    private static int handlePromptCommand(CommandContext<CommandSourceStack> context) {
        String prompt = StringArgumentType.getString(context, "text");

        context.getSource().sendSuccess(() -> Component.literal("Received prompt: " + prompt), false);

        new Thread(() -> {

            try {
                executePythonScript(context, prompt);
            } catch (CommandSyntaxException e) {
                LOGGER.error("Error executing Python script", e);
                context.getSource().sendSuccess(() -> Component.literal("Error executing Python script!"), false);
            }

        }).start();
        

        return 1; // Indicates the command executed successfully
    }

    private static void executePythonScript(CommandContext<CommandSourceStack> context, String prompt) throws CommandSyntaxException {
        String apiUrl = "http://127.0.0.1:5000/search";

        try {
            URL url = new URL(apiUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            String jsonInputString = String.format("{\"prompt\": \"%s\"}", prompt);

            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = connection.getResponseCode();
            LOGGER.info("Response Code: " + responseCode);

            StringBuilder response = new StringBuilder();
            try (BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }

            context.getSource().sendSuccess(() -> Component.literal("API Response: " + response.toString().trim()), false);

        } catch (IOException e) {
            LOGGER.error("Error calling API", e);
            context.getSource().sendSuccess(() -> Component.literal("Error calling API!"), false);
        }
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Server starting with Tutorial Mod...");
    }

    @Mod.EventBusSubscriber(modid = MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
    public static class ClientModEvents {
        @SubscribeEvent
        public static void onClientSetup(FMLClientSetupEvent event) {
            LOGGER.info("Client setup for Tutorial Mod...");
        }
    }
}
