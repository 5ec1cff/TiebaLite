package com.huanchengfly.tieba.post.api.adapters

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type

class StringToBooleanAdapter : JsonDeserializer<Boolean> {
    override fun deserialize(
        json: JsonElement,
        typeOfT: Type?,
        context: JsonDeserializationContext?
    ): Boolean {
        if (json.isJsonPrimitive) return json.asString == "1"
        throw IllegalArgumentException("$json is not String!")
    }
}