package net.foxsgr.minecraft.memories;

import com.google.gson.JsonObject;
import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import org.apache.logging.log4j.Logger;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.plugins.jpeg.JPEGImageWriteParam;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * Takes screenshots and uploads them to a server.
 */
public class Screenshotter {
    /**
     * JPEG compression level of images.
     */
    private static final float COMPRESSION_QUALITY = 0.8f;

    /**
     * The executor that takes the screenshots.
     */
    private static final Executor EXECUTOR = Executors.newSingleThreadExecutor();

    /**
     * The mod's logger.
     */
    private final Logger logger;

    /**
     * Creates a new screenshotter.
     *
     * @param logger the mod's logger.
     */
    public Screenshotter(Logger logger) {
        this.logger = logger;
    }

    /**
     * Takes a screenshot and uploads it to a server.
     */
    public void takeScreenshot(Runnable onFinish) {
        // Take the screenshot in the game thread
        var mc = Minecraft.getInstance();
        var screenshot = Screenshot.takeScreenshot(mc.getMainRenderTarget());

        // Upload the screenshot in a separate thread.
        EXECUTOR.execute(() -> {
            try {
                uploadScreenshot(screenshot);
                onFinish.run();
            } catch (Exception e) {
                this.logger.error("Error taking screenshot:", e);
                onFinish.run();
                Thread.currentThread().interrupt();
            }
        });
    }

    /**
     * Uploads the screenshot to the server.
     *
     * @param screenshot the screenshot to be uploaded.
     * @throws IOException          if the screenshot could not be encoded or sent.
     * @throws InterruptedException if the thread was interrupted while waiting for the server to respond.
     */
    private void uploadScreenshot(NativeImage screenshot) throws IOException, InterruptedException {
        var payload = buildPayload(screenshot);

        var client = HttpClient.newHttpClient();
        var url = ConfigManager.clientConfig.url.get();
        var request = HttpRequest
                .newBuilder(URI.create(url))
                .header("accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload))
                .build();

        var response = client.send(request, HttpResponse.BodyHandlers.ofString());
        screenshot.close();

        if (response.statusCode() != 200) {
            this.logger.debug("Payload: {}", payload);
            throw new IOException(
                    "Error %d response while uploading screenshot: %s".formatted(response.statusCode(), response.body())
            );
        }

        this.logger.info("Screenshot uploaded to server");
    }

    /**
     * Builds the payload to be sent to the server.
     *
     * @param screenshot the screenshot to be sent.
     * @return the payload.
     * @throws IOException if the screenshot could not be encoded.
     */
    private String buildPayload(NativeImage screenshot) throws IOException {
        var payload = new JsonObject();

        var mc = Minecraft.getInstance();
        payload.addProperty("username", mc.getUser().getName());

        var b64 = compressImage(screenshot);
        payload.addProperty("screenshot", b64);

        return payload.toString();
    }

    /**
     * Compresses the screenshot.
     *
     * @param screenshot the screenshot to be compressed.
     * @return the compressed screenshot.
     * @throws IOException if the screenshot could not be read or encoded.
     */
    private String compressImage(NativeImage screenshot) throws IOException {
        var inputImage = ImageIO.read(new ByteArrayInputStream(screenshot.asByteArray()));

        var bufferedImage = new BufferedImage(
                screenshot.getWidth(),
                screenshot.getHeight(),
                BufferedImage.TYPE_INT_RGB
        );
        bufferedImage.getGraphics().drawImage(inputImage, 0, 0, null);

        var params = new JPEGImageWriteParam(null);
        params.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        params.setCompressionQuality(Screenshotter.COMPRESSION_QUALITY);

        var outputStream = new ByteArrayOutputStream();
        var writer = ImageIO.getImageWritersByFormatName("jpg").next();
        writer.setOutput(ImageIO.createImageOutputStream(outputStream));
        writer.write(null, new IIOImage(bufferedImage, null, null), params);

        var compressedImageData = outputStream.toByteArray();
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(compressedImageData);
    }
}
