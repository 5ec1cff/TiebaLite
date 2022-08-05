package com.huanchengfly.tieba.post.api.adapters;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonParseException;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.annotations.JsonAdapter;
import com.huanchengfly.tieba.post.api.models.ThreadContentBean;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ContentMsgAdapter implements JsonDeserializer<List<ThreadContentBean.ContentBean>>, JsonSerializer<List<ThreadContentBean.ContentBean>> {
    @Override
    public List<ThreadContentBean.ContentBean> deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        List<ThreadContentBean.ContentBean> list = new ArrayList<>();
        if (json.isJsonArray()) {
            for (JsonElement element : json.getAsJsonArray()) {
                if (element.isJsonObject()) {
                    list.add(context.deserialize(element, ThreadContentBean.ContentBean.class));
                }
            }
        }

        return list;
    }

    @Override
    public JsonElement serialize(List<ThreadContentBean.ContentBean> src, Type typeOfSrc, JsonSerializationContext context) {
        JsonArray array = new JsonArray();
        src.forEach(element -> {
            array.add(context.serialize(element));
        });
        return array;
    }
}
