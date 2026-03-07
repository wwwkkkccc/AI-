package com.resumeai.service;

import com.resumeai.dto.RedactionResponse;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

/**
 * 隐私脱敏服务
 */
@Service
public class PrivacyRedactionService {

    // 手机号正则：1[3-9]开头的11位数字
    private static final Pattern PHONE_PATTERN = Pattern.compile("1[3-9]\\d{9}");

    // 邮箱正则
    private static final Pattern EMAIL_PATTERN = Pattern.compile("[\\w.-]+@[\\w.-]+\\.\\w+");

    // 身份证号正则：18位数字或17位数字+X
    private static final Pattern ID_CARD_PATTERN = Pattern.compile("\\d{17}[\\dXx]");

    // 银行卡号正则：16-19位数字（需要上下文判断）
    private static final Pattern BANK_CARD_PATTERN = Pattern.compile("\\d{16,19}");

    /**
     * 对文本进行隐私脱敏
     */
    public RedactionResponse redact(String text) {
        if (text == null || text.trim().isEmpty()) {
            throw new IllegalArgumentException("text is required");
        }

        StringBuilder redactedText = new StringBuilder(text);
        List<RedactionResponse.RedactedItem> redactedItems = new ArrayList<>();
        int offset = 0;

        // 1. 脱敏手机号
        Matcher phoneMatcher = PHONE_PATTERN.matcher(text);
        while (phoneMatcher.find()) {
            String original = phoneMatcher.group();
            int start = phoneMatcher.start() + offset;
            String replacement = "[手机号]";

            RedactionResponse.RedactedItem item = new RedactionResponse.RedactedItem();
            item.setType("phone");
            item.setOriginal(original);
            item.setPosition(phoneMatcher.start());
            redactedItems.add(item);

            redactedText.replace(start, start + original.length(), replacement);
            offset += replacement.length() - original.length();
        }

        // 2. 脱敏邮箱
        Matcher emailMatcher = EMAIL_PATTERN.matcher(text);
        offset = 0;
        redactedText = new StringBuilder(text);
        List<RedactionResponse.RedactedItem> emailItems = new ArrayList<>();

        while (emailMatcher.find()) {
            String original = emailMatcher.group();
            int start = emailMatcher.start();

            // 检查是否已经被脱敏
            boolean alreadyRedacted = false;
            for (RedactionResponse.RedactedItem item : redactedItems) {
                if (start >= item.getPosition() && start < item.getPosition() + item.getOriginal().length()) {
                    alreadyRedacted = true;
                    break;
                }
            }

            if (!alreadyRedacted) {
                RedactionResponse.RedactedItem item = new RedactionResponse.RedactedItem();
                item.setType("email");
                item.setOriginal(original);
                item.setPosition(start);
                emailItems.add(item);
            }
        }

        // 应用邮箱脱敏
        redactedText = new StringBuilder(text);
        offset = 0;
        for (RedactionResponse.RedactedItem item : redactedItems) {
            if ("phone".equals(item.getType())) {
                int start = item.getPosition() + offset;
                redactedText.replace(start, start + item.getOriginal().length(), "[手机号]");
                offset += "[手机号]".length() - item.getOriginal().length();
            }
        }

        String currentText = redactedText.toString();
        emailMatcher = EMAIL_PATTERN.matcher(currentText);
        redactedText = new StringBuilder(currentText);
        offset = 0;

        while (emailMatcher.find()) {
            String original = emailMatcher.group();
            int start = emailMatcher.start() + offset;
            String replacement = "[邮箱]";

            boolean found = false;
            for (RedactionResponse.RedactedItem item : emailItems) {
                if (item.getOriginal().equals(original)) {
                    found = true;
                    break;
                }
            }

            if (found) {
                redactedText.replace(start, start + original.length(), replacement);
                offset += replacement.length() - original.length();

                RedactionResponse.RedactedItem item = new RedactionResponse.RedactedItem();
                item.setType("email");
                item.setOriginal(original);
                item.setPosition(emailMatcher.start());
                redactedItems.add(item);
            }
        }

        // 3. 脱敏身份证号
        currentText = redactedText.toString();
        Matcher idCardMatcher = ID_CARD_PATTERN.matcher(currentText);
        redactedText = new StringBuilder(currentText);
        offset = 0;

        while (idCardMatcher.find()) {
            String original = idCardMatcher.group();
            int start = idCardMatcher.start() + offset;
            String replacement = "[身份证号]";

            redactedText.replace(start, start + original.length(), replacement);
            offset += replacement.length() - original.length();

            RedactionResponse.RedactedItem item = new RedactionResponse.RedactedItem();
            item.setType("id_card");
            item.setOriginal(original);
            item.setPosition(idCardMatcher.start());
            redactedItems.add(item);
        }

        // 4. 脱敏银行卡号（需要上下文判断）
        currentText = redactedText.toString();
        Matcher bankCardMatcher = BANK_CARD_PATTERN.matcher(currentText);
        redactedText = new StringBuilder(currentText);
        offset = 0;

        while (bankCardMatcher.find()) {
            String original = bankCardMatcher.group();
            int start = bankCardMatcher.start();

            // 检查上下文是否包含"卡号"、"银行"等关键词
            int contextStart = Math.max(0, start - 20);
            int contextEnd = Math.min(currentText.length(), start + original.length() + 20);
            String context = currentText.substring(contextStart, contextEnd).toLowerCase();

            if (context.contains("卡号") || context.contains("银行") ||
                context.contains("账号") || context.contains("card")) {

                int actualStart = start + offset;
                String replacement = "[银行卡号]";

                redactedText.replace(actualStart, actualStart + original.length(), replacement);
                offset += replacement.length() - original.length();

                RedactionResponse.RedactedItem item = new RedactionResponse.RedactedItem();
                item.setType("bank_card");
                item.setOriginal(original);
                item.setPosition(start);
                redactedItems.add(item);
            }
        }

        RedactionResponse response = new RedactionResponse();
        response.setRedactedText(redactedText.toString());
        response.setRedactedCount(redactedItems.size());
        response.setRedactedItems(redactedItems);

        return response;
    }
}
