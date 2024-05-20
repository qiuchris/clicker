package com.qiuchris;

import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;
import org.bytedeco.javacpp.BytePointer;
import org.bytedeco.leptonica.PIX;
import org.bytedeco.tesseract.TessBaseAPI;
import org.openqa.selenium.By;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import org.openqa.selenium.html5.Location;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import static org.bytedeco.leptonica.global.leptonica.pixDestroy;
import static org.bytedeco.leptonica.global.leptonica.pixRead;

public class Main {
    public static void main(String[] args) throws IOException {
        Properties config = new Properties();
        config.load(new FileInputStream("config.txt"));

        ChromeOptions opt = new ChromeOptions();
        opt.setExperimentalOption("prefs",
                Collections.singletonMap("profile.managed_default_content_settings.geolocation", 1));

        ChromeDriver driver = new ChromeDriver();
        driver.manage().timeouts().implicitlyWait(Duration.ofMinutes(10));
        driver.setLocation(new Location(Double.parseDouble((String) config.get("LOCATION_LONGITUDE")),
                Double.parseDouble((String) config.get("LOCATION_LATITUDE")), 0));

        System.out.println("location set");
        try {
            WebDriverWait w = new WebDriverWait(driver, Duration.ofMinutes(10));
            Random random = new Random();
            String courseName = (String) config.get("COURSE_NAME");

            driver.get("https://student.iclicker.com");
            driver.findElement(By.id("userEmail")).sendKeys((String) config.get("EMAIL"));
            driver.findElement(By.id("userPassword")).sendKeys((String) config.get("PASSWORD"));
            driver.findElement(By.id("sign-in-button")).click();
            driver.findElements(By.partialLinkText((String) config.get("COURSE_NAME"))).get(0).click();
            w.until(ExpectedConditions.presenceOfElementLocated(By.id("btnJoin")));
            driver.findElement(By.id("btnJoin")).click();
            Thread.sleep(10000);

            System.out.println("ready");

            while (!driver.getTitle().contains(courseName)) {
                if (driver.getTitle().contains("Polling Active")) {
                    String imgLink = w.until(ExpectedConditions.presenceOfElementLocated
                                    (By.xpath("/html/body/div/div[2]/div/div/div/div/div[3]/img")))
                            .getAttribute("src");
                    System.out.println(imgLink);

                    int selection;
                    if (imgLink != null) {
                        selection = getResponse(imageToText(imgLink), (String) config.get("OPENAI_API_TOKEN"));
                    } else {
                        selection = random.nextInt(2);
                    }

                    driver.findElements(By.className("btn-container")).get(selection).click();
                    System.out.println("clicked");

                    while (driver.getTitle().contains("Polling Active")) {
                        Thread.sleep(1000);
                    }
                }
                Thread.sleep(10000);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
    }

    public static int getResponse(String text, String token) {
        ChatMessage system = new ChatMessage("system",
                "You are a user input categorizer, which can only answer with a single " +
                        "character: A, B, C, D, or E. Answer the following questions with a single character.");
        ChatMessage user = new ChatMessage("user", text);

        OpenAiService service = new OpenAiService(token);
        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .messages(List.of(system, user))
                .model("gpt-3.5-turbo-0125")
                .maxTokens(1)
                .build();
        String answer = service.createChatCompletion(completionRequest).getChoices().get(0).getMessage().getContent();
        System.out.println(answer);
        switch (answer) {
            case "A":
                return 0;
            case "B":
                return 1;
            case "C":
                return 2;
            case "D":
                return 3;
            case "E":
                return 4;
            default:
                return 1;
        }
    }

    public static String imageToText(String imgLink) throws IOException {
        Path imgPath = Files.createTempFile("image", ".png");
        try (InputStream in = new URL(imgLink).openStream()) {
            Files.copy(in, imgPath, StandardCopyOption.REPLACE_EXISTING);
        }

        BytePointer outText;
        TessBaseAPI api = new TessBaseAPI();

        if (api.Init("tessdata", "ENG") != 0) {
            System.err.println("couldn't initialize tesseract");
            System.exit(1);
        }

        PIX image = pixRead(imgPath.toString());
        api.SetImage(image);
        outText = api.GetUTF8Text();
        String string = outText.getString();

        api.End();
        pixDestroy(image);
        outText.deallocate();
        return string;
    }
}