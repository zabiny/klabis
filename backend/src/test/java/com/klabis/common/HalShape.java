package com.klabis.common;

import com.jayway.jsonpath.JsonPath;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Renders the {@code _links} and {@code _templates} of a node inside a HAL(-FORMS) response as a sorted,
 * human-readable list so tests can pin the exact hypermedia surface of a response (parity tests).
 */
public final class HalShape {

    private static final String HOST = "http://localhost";

    private HalShape() {
    }

    public static List<String> of(String responseBody, String nodePath) {
        Map<String, Object> node = JsonPath.read(responseBody, nodePath);
        List<String> shape = new ArrayList<>();
        links(node).forEach((rel, href) -> shape.add("link " + rel + " -> " + href));
        templates(node).forEach((name, descriptor) -> shape.add("template " + name + " " + descriptor));
        return shape;
    }

    public static boolean hasEmbedded(String responseBody, String key) {
        Map<String, Object> embedded = JsonPath.read(responseBody, "$._embedded");
        return embedded.containsKey(key);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> links(Map<String, Object> node) {
        Map<String, String> result = new TreeMap<>();
        Object links = node.get("_links");
        if (links instanceof Map<?, ?> map) {
            ((Map<String, Object>) map).forEach((rel, value) -> result.put(rel, hrefs(value)));
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private static String hrefs(Object link) {
        if (link instanceof List<?> list) {
            return list.stream().map(HalShape::hrefs).toList().toString();
        }
        Map<String, Object> map = (Map<String, Object>) link;
        String href = String.valueOf(map.get("href")).replace(HOST, "");
        return Boolean.TRUE.equals(map.get("templated")) ? href + " (templated)" : href;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, String> templates(Map<String, Object> node) {
        Map<String, String> result = new TreeMap<>();
        Object templates = node.get("_templates");
        if (templates instanceof Map<?, ?> map) {
            ((Map<String, Map<String, Object>>) map).forEach((name, template) -> {
                List<Map<String, Object>> properties = (List<Map<String, Object>>) template.getOrDefault("properties", List.of());
                List<String> propertyNames = properties.stream().map(p -> String.valueOf(p.get("name"))).toList();
                String target = template.get("target") == null ? "" : template.get("target").toString().replace(HOST, "") + " ";
                result.put(name, template.get("method") + " " + target + propertyNames);
            });
        }
        return result;
    }
}
