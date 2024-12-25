package org.example;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;

public class WebScraper {

    public static void main(String[] args) throws IOException {
        String url = "https://www.daraz.pk/";
        Document document = Jsoup.connect(url).get();

        // Generate and print locators for 'input', 'select', 'a', and 'button' elements
        for (String tag : Arrays.asList("input", "select", "a", "button")) {
            Map<String, List<Map<String, Object>>> locatorsByElement = generateLocatorsForElements(document, tag);
            printLocatorsForElementType(locatorsByElement, tag);
        }
    }

    // Generate locators for each element
    private static Map<String, List<Map<String, Object>>> generateLocatorsForElements(Document document, String tagType) {
        Elements elements = document.select(tagType);  // Find all elements of a specific type
        Map<String, List<Map<String, Object>>> locatorsByElement = new LinkedHashMap<>();

        for (int i = 0; i < elements.size(); i++) {
            Element element = elements.get(i);
            List<Map<String, Object>> rankedLocators = rankIdentifiers(element);

            // Determine element name: Use 'value' attribute if available, else fall back to 'tagType' and index
            String elementName;
            if (element.hasAttr("class")) {
                elementName = tagType + "_" + element.attr("class");
            } else if (element.hasAttr("id")) {
                elementName = tagType + "_" + element.attr("id");
            } else {
                elementName = tagType + "_" + (i + 1);  // Fallback to index
            }

            locatorsByElement.put(elementName, rankedLocators);
        }
        return locatorsByElement;
    }

    // Step 4: Print locators for a specific element type
    private static void printLocatorsForElementType(Map<String, List<Map<String, Object>>> locatorsByElement, String tagType) {
        System.out.println("--- Locators for " + tagType + " elements ---");
        for (Map.Entry<String, List<Map<String, Object>>> entry : locatorsByElement.entrySet()) {
            System.out.println("Element: " + entry.getKey());
            for (Map<String, Object> loc : entry.getValue()) {
                System.out.println("Locator: " + loc.get("locator") + ", Rank: " + loc.get("rank"));
            }
            System.out.println();
        }
    }

    // Method to rank identifiers and generate locators
    private static List<Map<String, Object>> rankIdentifiers(Element element) {
        Map<String, Integer> rankMap = new HashMap<>();
        rankMap.put("id_css", 1);
        rankMap.put("class_css", 2);
        rankMap.put("name_css", 3);
        rankMap.put("id_xpath", 4);
        rankMap.put("class_xpath", 5);
        rankMap.put("name_xpath", 6);
        rankMap.put("data-*", 7);
        rankMap.put("type", 8);
        rankMap.put("href", 9);

        List<Map<String, Object>> rankedLocators = new ArrayList<>();

        // Generate CSS and XPath locators with ranks
        if (element.hasAttr("id")) {
            String id = element.attr("id");
            addLocator(rankedLocators, "#" + id, rankMap.get("id_css"));
            addLocator(rankedLocators, "//" + element.tagName() + "[@id='" + id + "']", rankMap.get("id_xpath"));
        }

        if (element.hasAttr("class")) {
            String classValue = element.attr("class").replace(" ", ".");  // Convert space-separated class to dot-separated
            addLocator(rankedLocators, "." + classValue, rankMap.get("class_css"));
            addLocator(rankedLocators, "//" + element.tagName() + "[@class='" + element.attr("class") + "']", rankMap.get("class_xpath"));
        }

        if (element.hasAttr("name")) {
            String name = element.attr("name");
            addLocator(rankedLocators, "[name='" + name + "']", rankMap.get("name_css"));
            addLocator(rankedLocators, "//" + element.tagName() + "[@name='" + name + "']", rankMap.get("name_xpath"));
        }

        for (Map.Entry<String, String> attribute : element.attributes().asList()) {
            if (Pattern.matches("^data-.*", attribute.getKey())) {  // Match data-* attributes
                addLocator(rankedLocators, "//" + element.tagName() + "[@" + attribute.getKey() + "='" + attribute.getValue() + "']", rankMap.get("data-*"));
            }
        }

        if (element.hasAttr("type")) {

            addLocator(rankedLocators, "//" + element.tagName() + "[@type='" + element.attr("type") + "']", rankMap.get("type"));
        }

        if (element.tagName().equals("a") && element.hasAttr("href")) {  // Special case for <a> tags
            addLocator(rankedLocators, "//a[@href='" + element.attr("href") + "']", rankMap.get("href"));
        }

        // Sort locators based on rank (lower rank is better)
        rankedLocators.sort(Comparator.comparingInt(loc -> (Integer) loc.get("rank")));
        return rankedLocators;
    }

    // Utility method to add a locator with its rank
    private static void addLocator(List<Map<String, Object>> rankedLocators, String locator, int rank) {
        Map<String, Object> loc = new HashMap<>();
        loc.put("locator", locator);
        loc.put("rank", rank);
        rankedLocators.add(loc);
    }
}
