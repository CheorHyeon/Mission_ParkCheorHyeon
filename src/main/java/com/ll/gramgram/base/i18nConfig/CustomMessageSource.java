package com.ll.gramgram.base.i18nConfig;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class CustomMessageSource extends ResourceBundleMessageSource {
    private final ApplicationContext applicationContext;
    private CustomMessageSource customMessageSource;
    private static final Pattern VARIABLE_PATTERN = Pattern.compile("\\$\\[\\[(.+?)\\]\\]");

    @Override
    protected String resolveCodeWithoutArguments(String code, Locale locale) {
        if (!code.startsWith("c.")) return super.resolveCodeWithoutArguments(code, locale);
        // 코드안에 있는 값 : code / 번역 언어 : locate
        return replaceVariablesToString(super.resolveCodeWithoutArguments(code, locale), locale);
    }

    @Override
    protected MessageFormat resolveCode(String code, Locale locale) {
        if (!code.startsWith("c.")) return super.resolveCode(code, locale);
        // c.으로 시작하는것만 구현한 함수 적용
        return replaceVariables(super.resolveCode(code, locale), locale);
    }

    public String replaceVariablesToString(String code, Locale locale) {
        // @Cacheable 이 붙어있는 메서드는 원래 this 로 호출하면 캐시가 작동하지 않는다.
        // 그래서 프록시 객체를 꺼내서 해야한다.
        // this. 으로 호출하면 안된다.
        if (customMessageSource == null) {
            customMessageSource = applicationContext.getBean("customMessageSource", CustomMessageSource.class);
        }
        return customMessageSource._replaceVariablesToString(code, locale);
    }

    // 캐시 사용
    // 함수에 의해 저장되는 캐시 이름, 키 설정
    // c.logoTextWithEmojiEn=$[[c.logoTextStartEn]]$[[c.logoEmoji]]$[[c.logoTextEndEn]]
    // 위 코드에서 1, 2, 3번들 풀때 겹치는 연산들 발생 -> 캐시 필요
    @Cacheable(cacheNames = "translation", key = "#code + ',' + #locale")
    public String _replaceVariablesToString(String code, Locale locale) {
        System.out.println("code : " + code + ", locale : " + locale);
        StringBuilder result = new StringBuilder();
        Matcher matcher = VARIABLE_PATTERN.matcher(code);

        while (matcher.find()) {
            String variable = matcher.group(1);
            String replacement = getMessage(variable, null, locale);
            matcher.appendReplacement(result, replacement);
        }
        matcher.appendTail(result);

        return result.toString();
    }

    private MessageFormat replaceVariables(MessageFormat messageFormat, Locale locale) {
        String message = messageFormat.toPattern();

        return new MessageFormat(replaceVariablesToString(message, locale), locale);
    }
}