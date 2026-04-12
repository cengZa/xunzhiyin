package com.lcj.campusreco.common.util;

import com.lcj.campusreco.common.exception.BizException;

public final class JsonUtils {

    private JsonUtils() {
    }

    public static String toJson(Object value) {
        return value == null ? "null" : String.valueOf(value);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        throw new BizException("当前工程骨架阶段未启用 JSON 反序列化实现");
    }

    public static <T> T fromJson(String json, Object typeReference) {
        throw new BizException("当前工程骨架阶段未启用 JSON 反序列化实现");
    }
}
