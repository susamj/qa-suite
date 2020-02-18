import java.io.BufferedWriter;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.chrome.ChromeDriver;

import io.restassured.RestAssured;

/**
 * @author susamj
 * 
 */

public class FindBrokenLinksParallel {
    private static final int MYTHREADS = 1;
    public static List < String > brokenLinks = new ArrayList < String > ();

    public static void main(String args[]) throws Exception {
        Date startTime = new Date();
        ExecutorService executor = Executors.newFixedThreadPool(MYTHREADS);
        ChromeDriver driver = new ChromeDriver();
        driver.get("http://doc.aljazeera.net/sitemap.xml?page=1");
        List < String > links = findAllLinks(driver);
        System.out.println("Total number of links found " + links.size());

        for (String link: links) {
            Runnable worker = new MyRunnable(link);
            executor.execute(worker);
        }
        System.out.println("\nFinished all threads");
        Date endTime = new Date();
        long timeTakenToTestAllLinks = (endTime.getTime() - startTime.getTime()) / 1000;

        /********************/
        /*******************/
        /********************/

        startTime = new Date();
        List < String > images = findAllImages(driver);
        System.out.println("Total number of images found " + images.size());

        for (String image: images) {
            Runnable worker = new MyRunnable(image);
            executor.execute(worker);
        }
        executor.shutdown();
        while (!executor.isTerminated()) {

        }
        System.out.println("\nFinished all threads");
        endTime = new Date();
        long timeTakenToTestAllImages = (endTime.getTime() - startTime.getTime()) / 1000;

        System.out.println("Time taken to test all " + links.size() + " links: " + timeTakenToTestAllLinks + "seconds");
        System.out.println("Time taken to test all " + images.size() + " images: " + timeTakenToTestAllImages + "seconds");

        System.out.println("No of broken links: " + brokenLinks.size());
        BufferedWriter writer = new BufferedWriter(new FileWriter("brokenLinks.txt"));
        for (String brokenLink: brokenLinks) {
            writer.write(brokenLink + "\n");
        }
        writer.close();
        driver.quit();
    }

    public static class MyRunnable implements Runnable {
        private final String url;
        private String currentUrl;

        MyRunnable(String url) {
            this.url = url;
        }

        @Override
        public void run() {

            currentUrl = url;
            try {
                if (currentUrl != null) {

                    // Discard spaces from url
                    currentUrl = currentUrl.split(" ")[0];

                    int statusCode = RestAssured.given().urlEncodingEnabled(false).when().get(currentUrl).getStatusCode();

                    // Verifying response code - The HttpStatus should be 200 if not
                    if (statusCode != 200) {
                        brokenLinks.add(url);
                        System.out.println("Broken link : " + currentUrl + " and status code is :" + statusCode);
                    }
                }
            } catch (Exception threadRunException) {
                System.err.println("Error occured: {}" + threadRunException.getMessage());
            }

        }
    }

    public static List < String > findAllLinks(WebDriver driver) {
        List < WebElement > links = new ArrayList < WebElement > ();
        links = driver.findElements(By.tagName("a"));

        List < String > attributeValues = new ArrayList < String > ();
        for (WebElement element: links) {
            String linkHref = element.getAttribute("href");
            if (linkHref != null) {
                attributeValues.add(linkHref);
            }
        }
        return attributeValues;
    }

    public static List < String > findAllImages(WebDriver driver) {
        List < WebElement > images = new ArrayList < WebElement > ();
        String imgSrc = null;
        images = driver.findElements(By.tagName("img"));

        List < String > finalList = new ArrayList < String > ();
        for (WebElement element: images) {
            String src = element.getAttribute("src");
            String srcset = element.getAttribute("srcset");
            if (src != null) {
                imgSrc = src;
            } else if (srcset != null) {
                imgSrc = srcset;
            }
            if (imgSrc != null) {
                finalList.add(imgSrc);
            }
        }
        return finalList;
    }
}
