package com.lcj.campusreco.common.util;

import com.lcj.campusreco.common.exception.BizException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

public final class JsonUtils {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private JsonUtils() {
    }

    public static String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception ex) {
            throw new BizException("JSON 序列化失败: " + ex.getMessage());
        }
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return OBJECT_MAPPER.readValue(json, clazz);
        } catch (Exception ex) {
            throw new BizException("JSON 反序列化失败: " + ex.getMessage());
        }
    }

    public static <T> T fromJson(String json, Object typeReference) {
        try {
            if (typeReference instanceof TypeReference<?> reference) {
                @SuppressWarnings("unchecked")
                T result = (T) OBJECT_MAPPER.readValue(json, reference);
                return result;
            }
            throw new BizException("不支持的 TypeReference 类型");
        } catch (BizException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BizException("JSON 反序列化失败: " + ex.getMessage());
        }
    }
}
