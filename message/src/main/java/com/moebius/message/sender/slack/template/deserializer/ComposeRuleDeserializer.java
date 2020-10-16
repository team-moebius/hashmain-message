package com.moebius.message.sender.slack.template.deserializer;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.TreeNode;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.moebius.message.sender.slack.template.rule.ComposeRule;
import com.moebius.message.sender.slack.template.rule.StaticTextRule;
import com.moebius.message.sender.slack.template.rule.TextFormatRule;
import com.moebius.message.sender.slack.template.rule.TextRefRule;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ComposeRuleDeserializer extends JsonDeserializer<ComposeRule> {
    private final static String REFER_TYPE = "refer";
    private final static String FORMAT_TYPE = "format";

    @Override
    public ComposeRule deserialize(JsonParser p, DeserializationContext context) throws IOException {
        TreeNode treeNode = p.readValueAsTree();

        if (treeNode.isValueNode()) {
            return new StaticTextRule(getTextFromTreeNode(treeNode));
        }

        String type = getTextFromTreeNode(treeNode.get("type"));

        if (FORMAT_TYPE.equals(type)) {
            String formatString = getTextFromTreeNode(treeNode.get("formatString"));
            List<String> targetFields = getTextListFromTreeNode(treeNode.get("targetFields"));

            return new TextFormatRule(formatString, targetFields);
        } else if (REFER_TYPE.equals(type)) {
            String targetField = getTextFromTreeNode(treeNode.get("targetField"));
            return new TextRefRule(targetField);
        }

        return null;
    }

    private String getTextFromTreeNode(TreeNode treeNode) {
        if (treeNode instanceof TextNode) {
            return ((TextNode) treeNode).asText();
        } else {
            return treeNode.toString();
        }
    }

    private List<String> getTextListFromTreeNode(TreeNode treeNode) {
        if (treeNode instanceof ArrayNode) {
            ArrayNode arrayNode = (ArrayNode) treeNode;
            List<String> textList = new ArrayList<>();
            arrayNode.elements().forEachRemaining(jsonNode -> textList.add(jsonNode.asText()));
            return textList;
        } else {
            return Collections.emptyList();
        }
    }
}
